import org.joda.time.DateTime
import org.scalatest._
import org.scalatest.matchers._
import java.sql.SQLException
import scala.Option
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

  val tableNamePrefix = "emp_BasicUsageSpec" + System.currentTimeMillis().toString.substring(8)

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
        session.execute("create table " + tableName + " (id integer primary key, name varchar(30),created_at timestamp not null)")
        session.update("insert into " + tableName + " (id, name, created_at) values (?, ? ,?)", 1, Some("name1"), new DateTime)
        session.update("insert into " + tableName + " (id, name, created_at) values (?, ? ,?)", 2, Some("name2"), new DateTime)

        intercept[SQLException] {
          session.update("insert into " + tableName + " (id, name, created_at) values (?, ?, ?)", 2, Some("name2"), new DateTime)
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
    val db = DB(ConnectionPool.borrow())
    val session = db.autoCommitSession
    session.update("update " + tableName + " set name = ? where id = ?", "name2", 2)
    session.close()

    // named datasources
    NamedDB('named) autoCommit {
      session =>
        session.list("select * from " + tableName) {
          rs => rs.int("id")
        }
    }

  }

  case class Emp(id: Int, name: String)

  case class Emp2(id: Int, name: Option[String])

  "readOnly" should "execute only queries" in {

    val tableName = tableNamePrefix + "_readOnly"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val emp: Option[Emp] = DB readOnly {
        _.single("select * from " + tableName + " where id = ?", 1) {
          rs => Emp(rs.int("id"), rs.string("name"))
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
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

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
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      implicit val db = DB(conn)
      ultimately(db.rollbackIfActive()) {
        db.begin()

        // with implicit DB instnace
        DB withinTx {
          session =>
            val emp: Option[Emp] = session.single("select * from " + tableName + " where id = ?", 1)(rs =>
              Emp(rs.int("id"), rs.string("name"))
            )
            val emps: List[Emp] = session.list("select * from " + tableName) {
              rs =>
                Emp(rs.int("id"), rs.string("name"))
            }
        }
      }
    }
  }

  "Anorm 2.0" should "be availabe with scalikejdbc seamlessly" in {

    val tableName = tableNamePrefix + "_anorm20"

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      import anorm._
      import anorm.SqlParser._

      case class Emp(id: Int, name: Option[String])
      val empAllColumns = get[Int]("id") ~ get[Option[String]]("name") map {
        case id ~ name => Emp(id, name)
      }

      DB localTxWithConnection {
        implicit conn =>
          val empOpt: Option[Emp] = SQL("select * from " + tableName + " where id = {id}").on('id -> 1).as(empAllColumns.singleOpt)
          val empList: List[Emp] = SQL("select * from " + tableName).as(empAllColumns.*)
      }
    }

  }

  "SQL" should "be available" in {

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable("emp_BasicUsageSpec_ActiveSql")) {
      TestUtils.initialize("emp_BasicUsageSpec_ActiveSql")

      val eopt: Option[Emp] = DB readOnly {
        implicit session =>
          SQL("select * from emp_BasicUsageSpec_ActiveSql where id = ?").bind(1)
            .map(rs => Emp(rs.int("id"), rs.string("name"))).single.apply()
      }
      eopt.isDefined should be(true)

      val ehead: Option[Emp] = DB readOnly {
        implicit session =>
          SQL("select * from emp_BasicUsageSpec_ActiveSql")
            .map(rs => Emp(rs.int("id"), rs.string("name"))).first.apply()
      }
      ehead.isDefined should be(true)

      val es: List[Emp] = DB readOnly {
        implicit session =>
          SQL("select * from emp_BasicUsageSpec_ActiveSql")
            .map(rs => Emp(rs.int("id"), rs.string("name"))).list.apply()
      }
      es.size should equal(2)

      val tr: Traversable[Emp] = DB readOnly {
        implicit session =>
          SQL("select * from emp_BasicUsageSpec_ActiveSql")
            .map(rs => Emp(rs.int("id"), rs.string("name"))).traversable.apply()
      }
      tr.foreach {
        case e: Emp => e should not be (null)
      }

      {
        implicit val session = DB(conn).readOnlySession
        val e2s: List[Emp2] = SQL("select * from emp_BasicUsageSpec_ActiveSql")
          .map(rs => Emp2(rs.int("id"), Option(rs.string("name")))).list.apply()
        e2s.size should equal(2)
      }
    }
  }

  "An example of SQL" should "be available" in {

    val conn: Connection = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable("emp")) {
      TestUtils.initialize("emp")

      val empMapper = (rs: WrappedResultSet) => Emp(rs.int("id"), rs.string("name"))

      val get10EmpSQL: SQL[Emp] = {
        DB autoCommit { implicit s =>
          try {
            val sql = SQL("select * from emp order by id limit 10").map(empMapper)
            sql.list.apply()
            sql
          } catch {
            case e =>
              val sql = SQL("select * from emp order by id fetch first 10 rows only").map(empMapper)
              sql.list.apply()
              sql
          }
        }
      }

      val get10EmpAllSQL: SQLToList[Emp] = get10EmpSQL.list // or #toList

      DB autoCommit {
        implicit s =>

          val emps: List[Emp] = get10EmpAllSQL.apply()
          emps.size should be <= 10

          val getFirstOf10Emp: SQLToOption[Emp] = get10EmpSQL.first // or #headOption
          val firstEmp: Option[Emp] = getFirstOf10Emp.apply()
          firstEmp.isDefined should be(true)

          val single: Option[Emp] = SQL("select * from emp where id = ?").bind(1).map(empMapper).single.apply() // or #toOption
          single.isDefined should be(true)
          try { SQL("drop table company").execute.apply() } catch { case e => }
          try {
            val result: Boolean = SQL("""
              create table company (
                id integer primary key,
                name varchar(30) not null,
                description varchar(1000),
                created_at timestamp
              )
            """).execute.apply()
          } catch { case e => }
          val result: Boolean = SQL("truncate table company").execute.apply()
          val count: Int = SQL("insert into company values (?, ?, ?, ?)")
            .bind(
              1,
              "Typesafe",
              """
            Typesafe makes it easy to build software based on the open source Scala programming language, Akka middleware, and Play web framework.
            From multicore to cloud computing, it's purpose built for scale.
            """,
              new DateTime).update.apply()

      }
    }
  }

  "Logging SQL and timing" should "be available" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table logging_sql_and_timing").execute.apply()
        } catch { case e => }
        SQL("create table logging_sql_and_timing (id int primary key, name varchar(13) not null)")
          .execute.apply()
        1 to 100000 foreach { i =>
          SQL("insert                into  logging_sql_and_timing values (?,?)").bind(i, "id_%010d".format(i)).update.apply()
        }
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = 'INFO,
          warningThresholdMillis = 10L
        )
        SQL("select                                   *  from     logging_sql_and_timing").map(rs => rs.int("id")).list.apply()
      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
        try {
          SQL("drop table logging_sql_and_timing")
            .execute.apply()
        } catch { case e => }
      }
    }
  }

}
