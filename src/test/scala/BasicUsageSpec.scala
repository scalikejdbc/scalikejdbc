import org.scalatest._
import org.scalatest.matchers._
import java.sql.SQLException
import util.control.Exception._
import java.sql.Connection
import scalikejdbc._

class BasicUsageSpec extends FlatSpec with ShouldMatchers {

  behavior of "Basic usage of ScalikeJDBC"

  // ---------------------------
  // Preparing Connection Pool
  // ---------------------------

  // loading jdbc.properties
  private val props = new java.util.Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))

  // loading JDBC driver
  val driverClassName = props.getProperty("driverClassName")
  Class.forName(driverClassName)

  // preparing the connection pool settings
  val poolSettings = new ConnectionPoolSettings(initialSize = 100, maxSize = 100)

  // JDBC settings
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  // create singleton(default) connection pool
  ConnectionPool.singleton(url, user, password, poolSettings)

  // named connection pool
  ConnectionPool.add('named, url, user, password, poolSettings)

  // ---------------------------
  // Borrow a connection from the ConnectionPool
  // ---------------------------

  it should "be able to borrow a connection from ConnectionPool" in {
    val conn = ConnectionPool.borrow()
    conn should not be null
    conn.close() // bring back the connection to the pool
  }

  it should "be able to borrow a connection from named ConnectionPool" in {
    val conn = ConnectionPool('named).borrow()
    conn should not be null
    conn.close()
  }

  // ---------------------------
  // Transaction
  // ---------------------------

  it should "be able to begin the transaction" in {
    val conn = ConnectionPool.borrow()
    val db = DB(conn)

    db.begin()
    db.beginIfNotYet() // NEVER throw an exception

    db.rollback()
    db.rollbackIfActive() // NEVER throw an exception

    db.begin()
    db.commit()
  }

  // ---------------------------
  // Operations
  // ---------------------------

  val tableNamePrefix = "emp_BasicUsageSpec" + System.currentTimeMillis()

  "autoCommit" should "excute without a transaction" in {

    val tableName = tableNamePrefix + "_autoCommit"

    // get a connection and create DB instance
    using(ConnectionPool.borrow()) {
      conn =>
        DB(conn) autoCommit {
          session =>
            ignoring(classOf[Throwable]) {
              session.execute("drop table " + tableName)
            }
        }
    }

    // connect and begin a block (ConnectionPool required)
    DB autoCommit {
      session =>
        session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
        session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
        session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")

        intercept[SQLException] {
          session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
        }
    }

    // connect and begin a block (ConnectionPool required)
    DB.connect().autoCommit {
      session =>
        session.update("update " + tableName + " set name = ? where id = ?", "updated2", 2)
    }

    // passing a connection as an implicit parameter
    implicit val conn: Connection = ConnectionPool.borrow()
    DB.connected.autoCommit {
      session =>
        session.update("update " + tableName + " set name = ? where id = ?", "updated2", 2)
    }

    // creating a session instance without a block
    val db = DB(conn)
    val session = db.autoCommitSession
    session.update("update " + tableName + " set name = ? where id = ?", "name2", 2)

  }

  case class Emp(id: Int, name: String)

  case class Emp2(id: Int, name: Option[String])

  "readOnly" should "execute only queries" in {

    val tableName = tableNamePrefix + "_readOnly"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)

      val emp: Option[Emp] = DB readOnly {
        _.single("select * from " + tableName + " where id = ?", 1) {
          rs =>
            Emp(rs.int("id"), rs.string("name"))
        }
      }

      val emps: List[Emp] = DB readOnly {
        _.list("select * from " + tableName) {
          rs =>
            Emp(rs.int("id"), rs.string("name"))
        }
      }

      val session = DB(conn).readOnlySession
      val emps2: List[Emp2] = session.list("select * from " + tableName) {
        rs =>
          Emp2(rs.int("id"), Option(rs.string("name")))
      }
    }
  }

  "localTx" should "execute in the same transation" in {

    val tableName = tableNamePrefix + "_localTx"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)

      DB localTx {
        session =>
          val emp: Option[Emp] = session.single("select * from " + tableName + " where id = ?", 1) {
            rs => Emp(rs.int("id"), rs.string("name"))
          }
          val emps: List[Emp] = session.list("select * from " + tableName) {
            rs => Emp(rs.int("id"), rs.string("name"))
          }
      }
    }
  }

  "withinTx" should "execute in the tx which already has started" in {

    val tableName = tableNamePrefix + "_withinTx"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)

      implicit val db = DB(conn)
      ultimately(db.rollbackIfActive()) {
        db.begin()

        // with implicit DB instnace
        DB withinTx {
          session =>
            val emp: Option[Emp] = session.single("select * from " + tableName + " where id = ?", 1)(rs =>
              Emp(rs.int("id"), rs.string("name"))
            )
            val emps: List[Emp] = session.list("select * from " + tableName) { rs =>
              Emp(rs.int("id"), rs.string("name"))
            }
        }
      }
    }
  }

  "Anorm 2.0" should "be availabe with scalikejdbc seamlessly" in {

    val tableName = tableNamePrefix + "_anorm20"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      DB localTxWithConnection { implicit conn =>

        import anorm._
        import anorm.SqlParser._

        case class Emp(id: Int, name: Option[String])

        val allColumns = get[Int]("id") ~ get[Option[String]]("name") map {
          case id ~ name => Emp(id, name)
        }
        val empOpt: Option[Emp] = SQL("select * from " + tableName + " where id = {id}").on('id -> 1).as(allColumns.singleOpt)
        val emps: List[Emp] = SQL("select * from " + tableName).as(allColumns.*)
      }
    }

  }

}
