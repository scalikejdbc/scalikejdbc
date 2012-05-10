package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter
import org.joda.time.DateTime
import java.util.Calendar
import java.sql.PreparedStatement

class DBSessionSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DBSessionSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DBSession"

  it should "be available" in {
    val session = new DBSession(connect = () => ConnectionPool.borrow())
    session.conn should not be (null)
    session.connection should not be (null)
    try {
      session should not be null
    } finally { session.close() }
  }

  it should "be able to close java.sql.Connection with filters" in {
    val tableName = tableNamePrefix + "_closeConnection"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      // new Connection for testing close
      val db = new DB(() => ConnectionPool.borrow())
      val session = db.autoCommitSession()
      try {

        val before = (stmt: PreparedStatement) => println("before")
        val after = (stmt: PreparedStatement) => println("after")
        session.executeWithFilters(before, after, "insert into " + tableName + " values (?, ?)", 3, Option("Ben"))
        val benOpt = session.single("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")
      } finally { session.close() }

      try {
        session.single("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
        fail("Exception should be thrown")
      } catch {
        case e: java.sql.SQLException =>
      }

      session.close()
      session.close()
    }
  }

  it should "execute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val session = db.autoCommitSession()
      try {
        session.execute("insert into " + tableName + " values (?, ?)", 3, Option("Ben"))
        val benOpt = session.single("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")

        session.execute("insert into " + tableName + " values (?, ?)", 4, Option(null))
        val noName = session.single("select id,name from " + tableName + " where id = ?", 4)(rs => (rs.int("id"), rs.string("name")))
        noName.get._1 should equal(4)
        noName.get._2 should equal(null)
      } finally { session.close() }
    }
  }

  // --------------------
  // auto commit

  it should "execute single in auto commit mode" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val session = db.autoCommitSession()
      try {
        val singleResult = session.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
        val firstResult = session.first("select id from " + tableName)(rs => rs.string("id"))
        singleResult.get should equal("1")
        firstResult.get should equal("1")
      } finally { session.close() }
    }
  }

  it should "execute list in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val session = db.autoCommitSession()
      try {
        val result = session.list("select id from " + tableName) {
          rs => rs.string("id")
        }
        result.size should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute update in auto commit mode with filters" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    val db = new DB(() => ConnectionPool.borrow())
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = new DB(() => ConnectionPool.borrow()).autoCommitSession()
      try {
        val before = (stmt: PreparedStatement) => println("before")
        val after = (stmt: PreparedStatement) => println("after")
        val count = session.updateWithFilters(before, after, "update " + tableName + " set name = ? where id = ?", "foo", 1)
        db.rollbackIfActive()
        count should equal(1)
        val name = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        } getOrElse "---"
        name should equal("foo")
      } finally { session.close() }
    }

  }

  it should "execute executeUpdate in auto commit mode" in {
    val tableName = tableNamePrefix + "_executeUpdateInAutoCommit"
    val db = new DB(() => ConnectionPool.borrow())
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = new DB(() => ConnectionPool.borrow()).autoCommitSession()
      try {
        val count = session.executeUpdate("update " + tableName + " set name = ? where id = ?", "foo", 1)
        db.rollbackIfActive()
        count should equal(1)
        val name = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        } getOrElse "---"
        name should equal("foo")
      } finally { session.close() }
    }

  }

  // --------------------
  // within tx mode

  it should "execute single in within tx mode" in {
    val tableName = tableNamePrefix + "_singleInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      try {
        TestUtils.initializeEmpRecords(session, tableName)
        val result = session.single("select id from " + tableName + " where id = ?", 1) {
          rs => rs.string("id")
        }
        result.get should equal("1")
        db.rollbackIfActive()
      } finally { session.close() }
    }
  }

  it should "execute list in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      try {
        TestUtils.initializeEmpRecords(session, tableName)
        val result = session.list("select id from " + tableName + "") {
          rs => rs.string("id")
        }
        result.size should equal(2)
        db.rollbackIfActive()
      } finally { session.close() }
    }
  }

  it should "execute update in within tx mode" in {
    val tableName = tableNamePrefix + "_updateInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      try {
        TestUtils.initializeEmpRecords(session, tableName)
        val nameBefore = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }.get
        nameBefore should equal("name1")
        val count = session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        count should equal(1)
        db.rollbackIfActive()
        val name = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }.get
        name should equal("name1")
      } finally { session.close() }
    }
  }

  it should "bind java.util.Date as java.sql.Timestamp" in {
    DB autoCommit {
      implicit session =>
        try {
          SQL("create table dbsessionspec_judate (id integer primary key, date timestamp)").execute.apply()
          SQL("insert into dbsessionspec_judate values (?, ?)").bind(1, new java.util.Date()).update.apply()
        } finally {
          SQL("drop table dbsessionspec_judate").execute.apply()
        }
    }
  }

  it should "be able to get a generated key" in {
    DB autoCommit {
      implicit session =>
        try {
          try {
            SQL("create table dbsessionspec_genkey (id integer generated always as identity(start with 0), name varchar(30))").execute.apply()
          } catch {
            case e =>
              println(e.getMessage)
              try {
                SQL("create table dbsessionspec_genkey (id integer auto_increment, name varchar(30), primary key(id))").execute.apply()
              } catch {
                case e =>
                  SQL("create table dbsessionspec_genkey (id serial not null, name varchar(30), primary key(id))").execute.apply()
              }
          }
          var id = -1L
          val before = (stmt: PreparedStatement) => {}
          val after = (stmt: PreparedStatement) => {
            val rs = stmt.getGeneratedKeys
            rs.next()
            id = rs.getLong(1)
          }
          SQL("insert into dbsessionspec_genkey (name) values (?)").bind("xxx").updateWithFilters(before, after).apply()
          id should be <= 1L
          SQL("insert into dbsessionspec_genkey (name) values (?)").bind("xxx").updateWithFilters(before, after).apply()
          id should be <= 2L
        } finally {
          SQL("drop table dbsessionspec_genkey").execute.apply()
        }
    }
  }

  it should "be able to updateAndReturnGeneratedKey" in {
    DB autoCommit {
      implicit session =>
        try {
          try {
            SQL("create table dbsessionspec_update_genkey (id integer generated always as identity(start with 0), name varchar(30))").execute.apply()
          } catch {
            case e =>
              println(e.getMessage)
              try {
                SQL("create table dbsessionspec_update_genkey (id integer auto_increment, name varchar(30), primary key(id))").execute.apply()
              } catch {
                case e =>
                  SQL("create table dbsessionspec_update_genkey (id serial not null, name varchar(30), primary key(id))").execute.apply()
              }
          }

          val id1 = SQL("insert into dbsessionspec_update_genkey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id1 should be <= 1L
          val id2 = SQL("insert into dbsessionspec_update_genkey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id2 should be <= 2L
          val id3 = SQL("insert into dbsessionspec_update_genkey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id3 should be <= 3L
          val id4 = SQL("insert into dbsessionspec_update_genkey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id4 should be <= 4L
        } finally {
          SQL("drop table dbsessionspec_update_genkey").execute.apply()
        }
    }
  }

  it should "work with datatime values" in {

    val date = new DateTime(2012, 5, 3, 13, 40, 0, 0).toDate
    execute(date, date, date)
    execute(date.toDateTime, date.toDateTime, date.toDateTime)
    execute(date.toLocalDateTime, date.toLocalDateTime, date.toLocalDateTime)
    execute(date.toLocalDate, date.toLocalTime, date.toLocalDateTime)
    execute(date.toSqlTimestamp, date.toSqlTimestamp, date.toSqlTimestamp)
    execute(date.toSqlDate, date.toSqlTime, date)

    def execute(date: Any, time: Any, timestamp: Any) {
      DB autoCommit {
        implicit session =>
          try {
            try {
              SQL("""
            create table dbsessionspec_dateTimeValues (
              id integer generated always as identity,
              date_value date not null,
              time_value time not null,
              timestamp_value timestamp not null
            )
          """).execute.apply()
            } catch {
              case e =>
                try {
                  SQL("""
            create table dbsessionspec_dateTimeValues (
              id integer auto_increment,
              date_value date not null,
              time_value time not null,
              timestamp_value timestamp not null,
              primary key(id)
            )
          """).execute.apply()
                } catch {
                  case e =>
                    SQL("""
            create table dbsessionspec_dateTimeValues (
              id serial not null,
              date_value date not null,
              time_value time not null,
              timestamp_value timestamp not null,
              primary key(id)
            )
          """).execute.apply()

                }

            }

            SQL("""
            insert into dbsessionspec_dateTimeValues
              (date_value, time_value, timestamp_value)
              values
              (?, ?, ?)
          """).bind(
              date,
              time,
              timestamp
            ).update.apply()

            val c = Calendar.getInstance()
            SQL("select * from dbsessionspec_dateTimeValues").map {
              rs => (rs.date("date_value"), rs.time("time_value"), rs.timestamp("timestamp_value"))
            }.first().apply().map {
              case (d: java.sql.Date, t: java.sql.Time, ts: java.sql.Timestamp) =>

                // java.sql.Date
                d.toLocalDate.getYear should equal(2012)
                d.toLocalDate.getMonthOfYear should equal(5)
                d.toLocalDate.getDayOfMonth should equal(3)

                // java.sql.Time
                t.toLocalTime.getHourOfDay should equal(13)
                t.toLocalTime.getMinuteOfHour should equal(40)
                t.toLocalTime.getSecondOfMinute should equal(0)
                t.toLocalTime.getMillisOfSecond should equal(0)

                // java.sql.Timestamp
                ts.toDateTime.getYear should equal(2012)
                ts.toDateTime.getMonthOfYear should equal(5)
                ts.toDateTime.getDayOfMonth should equal(3)
                ts.toDateTime.getHourOfDay should equal(13)
                ts.toDateTime.getMinuteOfHour should equal(40)
                ts.toDateTime.getSecondOfMinute should equal(0)
                ts.toDateTime.getMillisOfSecond should equal(0)

            } orElse {
              fail("Expected value is not found.")
            }

          } finally {
            try {
              SQL("drop table dbsessionspec_dateTimeValues").execute.apply()
            } catch {
              case e =>
            }
          }
      }
    }

  }

  it should "work with short values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("create table dbsession_work_with_short_values (id bigint generated always as identity, s smallint)").execute.apply()
        } catch {
          case e =>
            try {
              SQL("create table dbsession_work_with_short_values (id bigint auto_increment, s smallint, primary key(id))").execute.apply()
            } catch {
              case e =>
                SQL("create table dbsession_work_with_short_values (id serial not null, s smallint, primary key(id))").execute.apply()
            }
        }
        val s: Short = 123
        SQL("insert into dbsession_work_with_short_values (s) values (?)").bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_short_values").execute.apply()
        } catch { case e => e.printStackTrace }
      }
    }
  }

  it should "work with Scala BigDecimal values" in {
    val conn = ConnectionPool.borrow()
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("create table dbsession_work_with_scala_big_decimal_values (id bigint generated always as identity, s bigint)").execute.apply()
        } catch {
          case e =>
            try {
              SQL("create table dbsession_work_with_scala_big_decimal_values (id bigint auto_increment, s bigint, primary key(id))").execute.apply()
            } catch {
              case e =>
                SQL("create table dbsession_work_with_scala_big_decimal_values (id serial not null, s bigint, primary key(id))").execute.apply()
            }
        }
        val s: BigDecimal = BigDecimal(123)
        SQL("insert into dbsession_work_with_scala_big_decimal_values (s) values (?)").bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_scala_big_decimal_values").execute.apply()
        } catch { case e => e.printStackTrace }
      }
    }
  }

  it should "work with Java BigDecimal values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("create table dbsession_work_with_java_big_decimal_values (id bigint generated always as identity, s bigint)").execute.apply()
        } catch {
          case e =>
            try {
              SQL("create table dbsession_work_with_java_big_decimal_values (id bigint auto_increment, s bigint, primary key(id))").execute.apply()
            } catch {
              case e =>
                SQL("create table dbsession_work_with_java_big_decimal_values (id serial not null, s bigint, primary key(id))").execute.apply()
            }
        }
        val s: BigDecimal = BigDecimal(123)
        SQL("insert into dbsession_work_with_java_big_decimal_values (s) values (?)").bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_java_big_decimal_values").execute.apply()
        } catch { case e => e.printStackTrace }
      }
    }
  }

  it should "work with optional wrapper class values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("""
          create table dbsession_work_with_optional_values (
            id bigint generated always as identity, 
            v_boolean boolean, 
            v_byte tinyint, 
            v_double double, 
            v_float real, 
            v_int int, 
            v_long bigint, 
            v_short smallint,
            v_timestamp timestamp
          )
        """).execute.apply()
        } catch {
          case e =>
            try {
              SQL("""
          create table dbsession_work_with_optional_values (
            id bigint auto_increment,
            v_boolean boolean, 
            v_byte tinyint, 
            v_double double, 
            v_float real, 
            v_int int, 
            v_long bigint, 
            v_short smallint,
            v_timestamp datetime,
            primary key(id)
          )
        """).execute.apply()
            } catch {
              case e =>
                SQL("""
          create table dbsession_work_with_optional_values (
            id serial not null,
            v_boolean boolean, 
            v_byte smallint, 
            v_double double precision, 
            v_float real, 
            v_int int, 
            v_long bigint, 
            v_short smallint,
            v_timestamp timestamp,
            primary key(id)
          )
        """).execute.apply()

            }

        }
        val id = SQL("""
          insert into dbsession_work_with_optional_values 
          (v_boolean, v_byte, v_double, v_float, v_int, v_long, v_short, v_timestamp) values 
          (?,?,?,?,?,?,?,?)
        """).bind(
          None, None, None, None, None, None, None, None
        ).updateAndReturnGeneratedKey.apply()

        case class Result(vBoolean: Option[Boolean], vByte: Option[Byte], vDouble: Option[Double],
          vFloat: Option[Float], vInt: Option[Int], vLong: Option[Long], vShort: Option[Short],
          vTimestamp: Option[DateTime])

        def assert(resultOpt: Option[Result]): Unit = {
          resultOpt.isDefined should be(true)
          val result = resultOpt.get
          result.vBoolean.isDefined should be(false)
          result.vByte.isDefined should be(false)
          result.vDouble.isDefined should be(false)
          result.vFloat.isDefined should be(false)
          result.vInt.isDefined should be(false)
          result.vLong.isDefined should be(false)
          result.vShort.isDefined should be(false)
          result.vTimestamp.isDefined should be(false)
        }

        assert(SQL("select * from dbsession_work_with_optional_values where id = ?").bind(id).map {
          rs =>
            Result(
              vBoolean = Option(rs.boolean("v_boolean").asInstanceOf[Boolean]),
              vByte = Option(rs.byte("v_byte").asInstanceOf[Byte]),
              vDouble = Option(rs.double("v_double").asInstanceOf[Double]),
              vFloat = Option(rs.float("v_float").asInstanceOf[Float]),
              vInt = Option(rs.int("v_int").asInstanceOf[Int]),
              vLong = Option(rs.long("v_long").asInstanceOf[Long]),
              vShort = Option(rs.short("v_short").asInstanceOf[Short]),
              vTimestamp = Option(rs.timestamp("v_timestamp")).map(_.toDateTime)
            )
        }.single.apply())

        assert(SQL("select * from dbsession_work_with_optional_values where id = ?").bind(id).map {
          rs =>
            Result(
              vBoolean = opt[Boolean](rs.boolean("v_boolean")),
              vByte = opt[Byte](rs.byte("v_byte")),
              vDouble = opt[Double](rs.double("v_double")),
              vFloat = opt[Float](rs.float("v_float")),
              vInt = opt[Int](rs.int("v_int")),
              vLong = opt[Long](rs.long("v_long")),
              vShort = opt[Short](rs.short("v_short")),
              vTimestamp = Option(rs.timestamp("v_timestamp")).map(_.toDateTime)
            )
        }.single.apply())

        assert(SQL("select * from dbsession_work_with_optional_values where id = ?").bind(id).map {
          rs =>
            Result(
              vBoolean = rs.booleanOpt("v_boolean"),
              vByte = rs.byteOpt("v_byte"),
              vDouble = rs.doubleOpt("v_double"),
              vFloat = rs.floatOpt("v_float"),
              vInt = rs.intOpt("v_int"),
              vLong = rs.longOpt("v_long"),
              vShort = rs.shortOpt("v_short"),
              vTimestamp = Option(rs.timestamp("v_timestamp")).map(_.toDateTime)
            )
        }.single.apply())

      } finally {
        try {
          SQL("drop table dbsession_work_with_optional_values").execute.apply()
        } catch { case e => e.printStackTrace }
      }
    }
  }

}
