import java.time.LocalDateTime
import util.control.Exception._
import scalikejdbc._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class BasicUsageSpec extends AnyFlatSpec with Matchers with LoanPattern {

  behavior of "Basic usage of ScalikeJDBC"

  // ---------------------------
  // Preparing Connection Pool
  // ---------------------------

  // loading jdbc.properties
  private val props = new java.util.Properties
  props.load(
    classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties")
  )
  // loading JDBC driver
  val driverClassName = props.getProperty("driverClassName")
  Class.forName(driverClassName)
  // preparing the connection pool settings
  val poolSettings =
    new ConnectionPoolSettings(initialSize = 100, maxSize = 100)
  // JDBC settings
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  // create singleton(default) connection pool
  ConnectionPool.singleton(url, user, password, poolSettings)
  // named connection pool
  ConnectionPool.add("named", url, user, password, poolSettings)

  // ---------------------------
  // Borrow a connection from the ConnectionPool
  // ---------------------------

  it should "borrow a connection from ConnectionPool" in {
    val conn = ConnectionPool.borrow()
    try {
      conn should not be null
    } finally {
      conn.close() // bring back the connection to the pool
    }
  }

  it should "use loan pattern for borrowing a connection from ConnectionPool" in {
    using(ConnectionPool.borrow()) { conn =>
      conn should not be null
    }
  }

  it should "borrow a connection from named ConnectionPool" in {
    using(ConnectionPool("named").borrow()) { conn =>
      conn should not be null
    }
  }

  // ---------------------------
  // Transaction
  // ---------------------------

  it should "handle transactions" in {
    using(ConnectionPool.borrow()) { conn =>
      using(DB(conn)) { db =>
        db.begin()
        db.beginIfNotYet() // NEVER throw an exception
        db.rollback()
        db.rollbackIfActive() // NEVER throw an exception
        db.begin()
        db.commit()
      }
    }
  }

  // ---------------------------
  // Working with DBSession
  // ---------------------------

  val tableNamePrefix =
    "emp_BasicUsageSpec" + System.currentTimeMillis().toString.substring(8)

  "autoCommit" should "execute without a transaction" in {
    val tableName = tableNamePrefix + "_autoCommit"

    // get a connection and create DB instance
    DB autoCommit { session =>
      ignoring(classOf[Throwable]) {
        session.execute("drop table " + tableName)
      }
      session.execute(
        "create table " + tableName + " (id integer primary key, name varchar(30),created_at timestamp not null)"
      )
    }

    // connect and begin a block (ConnectionPool required)
    DB autoCommit { session =>
      session.update(
        "insert into " + tableName + " (id, name, created_at) values (?, ? ,?)",
        1,
        Some("name1"),
        LocalDateTime.now
      )
      session.update(
        "insert into " + tableName + " (id, name, created_at) values (?, ? ,?)",
        2,
        Some("name2"),
        LocalDateTime.now
      )
    }

    // named datasources
    NamedDB("named") autoCommit { session =>
      session.list("select * from " + tableName)(_.int("id"))
    }

    // creating a session instance without a block (be careful to close resources)
    val db = DB(ConnectionPool.borrow())
    val session = db.autoCommitSession()
    session.update(
      "update " + tableName + " set name = ? where id = ?",
      "name2",
      2
    )
    session.close()
  }

  case class Emp(id: Int, name: String)
  case class Emp2(id: Int, name: Option[String])

  "readOnly" should "execute only queries" in {
    val tableName = tableNamePrefix + "_readOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val emp: Option[Emp] = DB readOnly { session =>
        session.single("select * from " + tableName + " where id = ?", 1) {
          rs => Emp(rs.int("id"), rs.string("name"))
        }
      }
      val emps: List[Emp] = DB readOnly { session =>
        session.list("select * from " + tableName) { rs =>
          Emp(rs.int("id"), rs.string("name"))
        }
      }
      val session = DB.readOnlySession()
      val emps2: List[Emp2] = session.list("select * from " + tableName) { rs =>
        Emp2(rs.int("id"), Option(rs.string("name")))
      }
      session.close()

    }
  }

  "localTx" should "execute in the same transaction" in {
    val tableName = tableNamePrefix + "_localTx"
    try {
      TestUtils.initialize(tableName)

      DB localTx { session =>
        val emp: Option[Emp] =
          session.single("select * from " + tableName + " where id = ?", 1) {
            rs => Emp(rs.int("id"), rs.string("name"))
          }
        val emps: List[Emp] = session.list("select * from " + tableName) { rs =>
          Emp(rs.int("id"), rs.string("name"))
        }
      }

    } finally { TestUtils.deleteTable(tableName) }
  }

  "withinTx" should "execute in the tx which already has started" in {
    val tableName = tableNamePrefix + "_withinTx"
    try {
      TestUtils.initialize(tableName)

      implicit val db = DB(ConnectionPool.borrow())
      try {
        db.begin()
        // with implicit DB instance
        DB withinTx { session =>
          val emp: Option[Emp] =
            session.single("select * from " + tableName + " where id = ?", 1)(
              rs => Emp(rs.int("id"), rs.string("name"))
            )
          val emps: List[Emp] = session.list("select * from " + tableName) {
            rs => Emp(rs.int("id"), rs.string("name"))
          }
        }
      } finally { db.rollbackIfActive() }

    } finally { TestUtils.deleteTable(tableName) }
  }

  // ---------------------------
  // Working with SQL API
  // ---------------------------

  "SQL" should "be available" in {
    using(ConnectionPool.borrow()) { conn =>
      try {
        TestUtils.initialize("emp_BasicUsageSpec_SQL")

        val eopt: Option[Emp] = DB readOnly { implicit session =>
          SQL("select * from emp_BasicUsageSpec_SQL where id = ?")
            .bind(1)
            .map(rs => Emp(rs.int("id"), rs.string("name")))
            .single
            .apply()
        }
        eopt.isDefined should be(true)

        val ehead: Option[Emp] = DB readOnly { implicit session =>
          SQL("select * from emp_BasicUsageSpec_SQL")
            .map(rs => Emp(rs.int("id"), rs.string("name")))
            .first
            .apply()
        }
        ehead.isDefined should be(true)

        val es: List[Emp] = DB readOnly { implicit session =>
          SQL("select * from emp_BasicUsageSpec_SQL")
            .map(rs => Emp(rs.int("id"), rs.string("name")))
            .list
            .apply()
        }
        es.size should equal(2)

        val tr: Iterable[Emp] = DB readOnly { implicit session =>
          SQL("select * from emp_BasicUsageSpec_SQL")
            .map(rs => Emp(rs.int("id"), rs.string("name")))
            .iterable
            .apply()
        }

        {
          implicit val session = DB(conn).readOnlySession()
          val e2s: List[Emp2] = SQL("select * from emp_BasicUsageSpec_SQL")
            .map(rs => Emp2(rs.int("id"), Option(rs.string("name"))))
            .list
            .apply()
          e2s.size should equal(2)
          var sum: Long = 0L
          SQL("select id from emp_BasicUsageSpec_SQL").foreach { rs =>
            sum += rs.long("id")
          }
          sum should equal(3L)
        }

      } finally { TestUtils.deleteTable("emp_BasicUsageSpec_SQL") }
    }
  }

  "An example of SQL" should "be available" in {
    try {
      TestUtils.initialize("emp")

      val empMapper =
        (rs: WrappedResultSet) => Emp(rs.int("id"), rs.string("name"))

      // SQL instances are reusable
      val get10EmpSQL: SQL[Emp, HasExtractor] = {
        DB autoCommit { implicit s =>
          try {
            val sql =
              SQL("select * from emp order by id limit 10").map(empMapper)
            sql.list.apply() // trying limit keyword for this database
            sql // ok, this database supports limit keyword
          } catch {
            case e: Exception =>
              val sql = SQL(
                "select * from emp order by id fetch first 10 rows only"
              ).map(empMapper)
              sql.list.apply()
              sql
          }
        }
      }

      // SQLTo* instances are also reusable
      val get10EmpAllSQL: SQLToList[Emp, HasExtractor] =
        get10EmpSQL.list // or #toList

      DB autoCommit { implicit s =>
        // internally PreparedStatement#executeQuery()
        val emps: List[Emp] = get10EmpAllSQL.apply()
        emps.size should be <= 10

        val getFirstOf10Emp: SQLToOption[Emp, HasExtractor] =
          get10EmpSQL.first // or #headOption
        val firstEmp: Option[Emp] = getFirstOf10Emp.apply()
        firstEmp.isDefined should be(true)

        // expects single result or nothing, when multiple results are returned, Exception will be thrown.
        val single: Option[Emp] = SQL("select * from emp where id = ?")
          .bind(1)
          .map(empMapper)
          .single
          .apply() // or #toOption
        single.isDefined should be(true)

        // Execute DDL
        try {
          SQL("drop table company").execute.apply()
        } catch { case e: Exception => }
        try {
          val result: Boolean = SQL("""
            create table company (
              id integer primary key,
              name varchar(30) not null,
              description varchar(1000),
              created_at timestamp
            );""").execute.apply()
        } catch { case e: Exception => }
        SQL("truncate table company").execute.apply()

        // simply using statement with ?(place holder)
        SQL("""insert into company values (?, ?, ?, ?);""")
          .bind(
            1,
            "Typesafe",
            """Typesafe makes it easy to build software based on the open source Scala programming language, Akka middleware, and Play web framework.
             From multicore to cloud computing, it's purpose built for scale.""",
            LocalDateTime.now
          )
          .update
          .apply()

        // Anorm like template
        SQL("""
          insert into company values (
            {id},
            {name},
            {description},
            {createdAt}
          );""")
          .bindByName(
            "id" -> 2,
            "name" -> "Typesafe",
            "description" -> "xxx",
            "createdAt" -> LocalDateTime.now
          )
          .update
          .apply()

        // executable template
        SQL("""
          insert into company values (
            /*'id */123,
            /*'name */'Alice',
            /*'description */'xxxx',
            /*'createdAt */''
          );""")
          .bindByName(
            "id" -> 3,
            "name" -> "Typesafe",
            "description" -> "xxx",
            "createdAt" -> LocalDateTime.now
          )
          .update
          .apply()

      }
    } finally { TestUtils.deleteTable("emp") }
  }

  "Logging SQL and timing" should "be available" in {
    DB autoCommit { implicit session =>
      try {

        // default settings
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings

        try {
          SQL("drop table logging_sql_and_timing").execute.apply()
        } catch { case e: Exception => }
        SQL(
          "create table logging_sql_and_timing (id int primary key, name varchar(13) not null)"
        ).execute.apply()

        // bulk insert
        1 to 10000 foreach { i =>
          SQL("insert into  logging_sql_and_timing values (?,?)")
            .bind(i, "id_%010d".format(i))
            .update
            .apply()
        }

        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = "INFO",
          warningThresholdMillis = 10L
        )
        // this query will spend more than 10 millis
        SQL("select  *  from logging_sql_and_timing")
          .map(_.int("id"))
          .list
          .apply()

      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings
        try {
          SQL("drop table logging_sql_and_timing").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  it should "execute batch" in {
    val tableName = tableNamePrefix + "_batch"
    try {
      TestUtils.initialize(tableName)

      DB localTx { implicit session =>
        val params1: Seq[Seq[Any]] = (1001 to 2000).map { i =>
          Seq(i, "name" + i)
        }
        session.batch(
          "insert into " + tableName + " (id, name) values (?, ?)",
          params1: _*
        )

        val params2: Seq[Seq[Any]] = (2001 to 3000).map { i =>
          Seq(i, "name" + i)
        }
        SQL("insert into " + tableName + " (id, name) values (?, ?)")
          .batch(params2: _*)
          .apply()

        val params3: Seq[Seq[(String, Any)]] = (3001 to 4000).map { i =>
          Seq("id" -> i, "name" -> ("name" + i))
        }
        SQL("insert into " + tableName + " (id, name) values ({id}, {name})")
          .batchByName(params3: _*)
          .apply()

      }

      DB readOnly { implicit s =>
        val count: Long = SQL("select count(1) from " + tableName)
          .map(_.long(1))
          .single
          .apply()
          .get
        count should be > 3000L
      }

    } finally { TestUtils.deleteTable(tableName) }
  }

  it should "execute batch with empty params" in {
    val tableName = tableNamePrefix + "_batch_with_empty_params"
    try {
      TestUtils.initialize(tableName)
      DB localTx { implicit session =>
        SQL("insert into " + tableName + " (id, name) values (999, 'Alice')")
          .batchByName(Seq.empty[Seq[(String, Any)]]: _*)
          .apply()
      }
    } finally { TestUtils.deleteTable(tableName) }
  }

}
