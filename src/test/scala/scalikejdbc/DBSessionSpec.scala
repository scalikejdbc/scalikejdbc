package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter
import org.joda.time.DateTime
import java.util.Calendar
import java.sql.PreparedStatement

class DBSessionSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DBSessionSpec" + System.currentTimeMillis()

  behavior of "DBSession"

  it should "be available" in {
    val conn = ConnectionPool.borrow()
    val session = new DBSession(conn)
    session should not be null
  }

  it should "be able to close java.sql.Connection with filters" in {
    val tableName = tableNamePrefix + "_closeConnection"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)

      // new Connection for testing close
      val db = new DB(ConnectionPool.borrow())
      val session = db.autoCommitSession()

      val before = (stmt: PreparedStatement) => println("before")
      val after = (stmt: PreparedStatement) => println("after")
      session.executeWithFilters(before, after, "insert into " + tableName + " values (?, ?)", 3, Option("Ben"))
      val benOpt = session.single("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
      benOpt.get._1 should equal(3)
      benOpt.get._2 should equal("Ben")

      session.close()

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
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()

      session.execute("insert into " + tableName + " values (?, ?)", 3, Option("Ben"))
      val benOpt = session.single("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
      benOpt.get._1 should equal(3)
      benOpt.get._2 should equal("Ben")

      session.execute("insert into " + tableName + " values (?, ?)", 4, Option(null))
      val noName = session.single("select id,name from " + tableName + " where id = ?", 4)(rs => (rs.int("id"), rs.string("name")))
      noName.get._1 should equal(4)
      noName.get._2 should equal(null)
    }
  }

  // --------------------
  // auto commit

  it should "execute single in auto commit mode" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val singleResult = session.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      val firstResult = session.first("select id from " + tableName)(rs => rs.string("id"))
      singleResult.get should equal("1")
      firstResult.get should equal("1")
    }
  }

  it should "execute list in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val result = session.list("select id from " + tableName) {
        rs => rs.string("id")
      }
      result.size should equal(2)
    }
  }

  it should "execute update in auto commit mode with filters" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    val db = new DB(conn)
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val session = new DB(ConnectionPool.borrow()).autoCommitSession()
      val before = (stmt: PreparedStatement) => println("before")
      val after = (stmt: PreparedStatement) => println("after")
      val count = session.updateWithFilters(before, after, "update " + tableName + " set name = ? where id = ?", "foo", 1)
      db.rollbackIfActive()
      count should equal(1)
      val name = session.single("select name from " + tableName + " where id = ?", 1) {
        rs => rs.string("name")
      } getOrElse "---"
      name should equal("foo")
    }

  }

  it should "execute executeUpdate in auto commit mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_executeUpdateInAutoCommit"
    val db = new DB(conn)
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val session = new DB(ConnectionPool.borrow()).autoCommitSession()
      val count = session.executeUpdate("update " + tableName + " set name = ? where id = ?", "foo", 1)
      db.rollbackIfActive()
      count should equal(1)
      val name = session.single("select name from " + tableName + " where id = ?", 1) {
        rs => rs.string("name")
      } getOrElse "---"
      name should equal("foo")
    }

  }

  // --------------------
  // within tx mode

  it should "execute single in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.single("select id from " + tableName + " where id = ?", 1) {
        rs => rs.string("id")
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute list in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.list("select id from " + tableName + "") {
        rs => rs.string("id")
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
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
          SQL("create table dbsessionspec_genkey (id integer generated always as identity, name varchar(30))").execute.apply()
          var id = -1L
          val before = (stmt: PreparedStatement) => {}
          val after = (stmt: PreparedStatement) => {
            val rs = stmt.getGeneratedKeys
            rs.next()
            id = rs.getLong(1)
          }
          SQL("insert into dbsessionspec_genkey (name) values (?)").bind("xxx").updateWithFilters(before, after).apply()
          id should equal(0)
          SQL("insert into dbsessionspec_genkey (name) values (?)").bind("xxx").updateWithFilters(before, after).apply()
          id should equal(1)
        } finally {
          SQL("drop table dbsessionspec_genkey").execute.apply()
        }
    }
  }

  it should "be able to updateAndReturnGeneratedKey" in {
    DB autoCommit {
      implicit session =>
        try {
          SQL("create table dbsessionspec_updateAndReturnGeneratedKey (id integer generated always as identity, name varchar(30))").execute.apply()
          val id1 = SQL("insert into dbsessionspec_updateAndReturnGeneratedKey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id1 should equal(0)
          val id2 = SQL("insert into dbsessionspec_updateAndReturnGeneratedKey (name) values (?)").bind("xxx").updateAndReturnGeneratedKey.apply()
          id2 should equal(1)
        } finally {
          SQL("drop table dbsessionspec_updateAndReturnGeneratedKey").execute.apply()
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
            SQL("""
            create table dbsessionspec_dateTimeValues (
              id integer generated always as identity,
              date_value date not null,
              time_value time not null,
              timestamp_value timestamp not null
            )
          """).execute.apply()

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

}
