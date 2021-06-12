package scalikejdbc

import scalikejdbc.jodatime.JodaUnixTimeInMillisConverterImplicits._
import scalikejdbc.jodatime.JodaWrappedResultSet._
import scala.util.control.Exception._
import org.scalatest._
import org.scalatest.BeforeAndAfter
import org.joda.time.DateTime
import java.sql._
import scala.concurrent.ExecutionContext
import java.io.ByteArrayInputStream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DBSessionSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings
  with LogSupport
  with LoanPattern
  with JavaUtilDateConverterImplicits {

  def opt[A](v: Any): Option[A] = Option(v.asInstanceOf[A])

  val tableNamePrefix =
    "emp_DBSessionSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DBSession"

  it should "be available" in {
    using(DBSession(ConnectionPool.borrow())) { session =>
      session.conn should not be (null)
      session.connection should not be (null)
      session should not be null
    }
  }

  it should "have #tx" in {
    DB.localTx { session =>
      session.tx.isDefined should be(true)
    }
  }

  it should "be able to close java.sql.Connection with filters" in {
    val tableName = tableNamePrefix + "_closeConnection"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      // new Connection for testing close
      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        val before = (stmt: PreparedStatement) => println("before")
        val after = (stmt: PreparedStatement) => println("after")
        session.executeWithFilters(
          before,
          after,
          "insert into " + tableName + " values (?, ?)",
          3,
          Option("Ben")
        )
        val benOpt = session.single(
          "select id,name from " + tableName + " where id = ?",
          3
        )(rs => (rs.int("id"), rs.string("name")))
        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")

        session.close()

        try {
          session.single(
            "select id,name from " + tableName + " where id = ?",
            3
          )(rs => (rs.int("id"), rs.string("name")))
          fail("Exception should be thrown")
        } catch {
          case e: java.sql.SQLException =>
        }

        session.close()
        session.close()
      }
    }
  }

  it should "execute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        session.execute(
          "insert into " + tableName + " values (?, ?)",
          3,
          Option("Ben")
        )
        val benOpt = session.single(
          "select id,name from " + tableName + " where id = ?",
          3
        )(rs => (rs.int("id"), rs.string("name")))
        benOpt.get._1 should equal(3)
        benOpt.get._2 should equal("Ben")

        session.execute(
          "insert into " + tableName + " values (?, ?)",
          4,
          Option(null)
        )
        val noName = session.single(
          "select id,name from " + tableName + " where id = ?",
          4
        )(rs => (rs.int("id"), rs.string("name")))
        noName.get._1 should equal(4)
        noName.get._2 should equal(null)
      }
    }
  }

  // --------------------
  // auto commit

  it should "execute single in auto commit mode" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        val singleResult =
          session.single("select id from " + tableName + " where id = ?", 1)(
            rs => rs.string("id")
          )
        val firstResult =
          session.first("select id from " + tableName)(rs => rs.string("id"))
        singleResult.get should equal("1")
        firstResult.get should equal("1")
      }
    }
  }

  it should "execute list in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        val result = session.list("select id from " + tableName) { rs =>
          rs.string("id")
        }
        result.size should equal(2)
      }
    }
  }

  it should "execute collection in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        val result =
          session.collection[String, Vector]("select id from " + tableName) {
            rs => rs.string("id")
          }
        result.size should equal(2)
      }
    }
  }

  it should "execute update in auto commit mode with filters" in {
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        try {
          val before = (stmt: PreparedStatement) => println("before")
          val after = (stmt: PreparedStatement) => println("after")
          val count = session.updateWithFilters(
            before,
            after,
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
          db.rollbackIfActive()
          count should equal(1)
          val name = session.single(
            "select name from " + tableName + " where id = ?",
            1
          ) { rs =>
            rs.string("name")
          } getOrElse "---"
          name should equal("foo")
        } finally {
          session.close()
        }
      }
    }
  }

  it should "execute executeUpdate in auto commit mode" in {
    val tableName = tableNamePrefix + "_executeUpdateInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        val session = db.autoCommitSession()
        try {
          val count = session.executeUpdate(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
          db.rollbackIfActive()
          count should equal(1)
          val name = session.single(
            "select name from " + tableName + " where id = ?",
            1
          ) { rs =>
            rs.string("name")
          } getOrElse "---"
          name should equal("foo")
        } finally {
          session.close()
        }
      }
    }

  }

  // --------------------
  // within tx mode

  it should "execute single in within tx mode" in {
    val tableName = tableNamePrefix + "_singleInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val session = db.withinTxSession()
        try {
          TestUtils.initializeEmpRecords(session, tableName)
          val result =
            session.single("select id from " + tableName + " where id = ?", 1) {
              rs => rs.string("id")
            }
          result.get should equal("1")
          db.rollbackIfActive()
        } finally {
          session.close()
        }
      }
    }
  }

  it should "execute list in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val session = db.withinTxSession()
        try {
          TestUtils.initializeEmpRecords(session, tableName)
          val result = session.list("select id from " + tableName + "") { rs =>
            rs.string("id")
          }
          result.size should equal(2)
          db.rollbackIfActive()
        } finally {
          session.close()
        }
      }
    }
  }

  it should "execute collection in within tx mode" in {
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(new DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val session = db.withinTxSession()
        try {
          TestUtils.initializeEmpRecords(session, tableName)
          val result = session.collection[String, Vector](
            "select id from " + tableName + ""
          ) { rs =>
            rs.string("id")
          }
          result.size should equal(2)
          db.rollbackIfActive()
        } finally {
          session.close()
        }
      }
    }
  }

  it should "execute batch in local tx mode" in {
    val tableName = tableNamePrefix + "_batchInLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      GlobalSettings.loggingSQLAndTime =
        new LoggingSQLAndTimeSettings(enabled = false)
      val batchTime: Long = DB localTx { session =>
        val before = System.currentTimeMillis()
        val paramsList = (10001 to 30000).map(i => Seq(i, "Name" + i))
        session.batch(
          "insert into " + tableName + " (id, name) values (?, ?)",
          paramsList: _*
        )
        System.currentTimeMillis() - before
      }
      val loopTime: Long = DB localTx { session =>
        val before = System.currentTimeMillis()
        (30001 to 40000) foreach { i =>
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            i,
            "Name" + i
          )
        }
        System.currentTimeMillis() - before
      }
      println("")
      println("batch: " + batchTime + ", loop: " + loopTime)
      println("")
    }
  }

  it should "be rolled back when BatchUpdasteException occurred" in {
    val tableName = tableNamePrefix + "_batchInLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      try {
        DB localTx { session =>
          val paramsList = (1001 to 2000).map(i => Seq(i, "Name" + i))
          session.batch(
            "insert into " + tableName + " (id, name) values (?, ?)",
            paramsList: _*
          )
          throw new RuntimeException
        }
      } catch {
        case e: Exception =>
      }
      val result = DB localTx { implicit session =>
        SQL("select id from " + tableName + " where id = ?")
          .bind(1001)
          .map(_.long("id"))
          .toOption
          .apply()
      }
      result.isDefined should not be (true)
    }
  }

  it should "execute update in within tx mode" in {
    val tableName = tableNamePrefix + "_updateInWithinTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(new DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val session = db.withinTxSession()
        TestUtils.initializeEmpRecords(session, tableName)
        val nameBefore = session
          .single("select name from " + tableName + " where id = ?", 1) { rs =>
            rs.string("name")
          }
          .get
        nameBefore should equal("name1")
        val count = session.update(
          "update " + tableName + " set name = ? where id = ?",
          "foo",
          1
        )
        count should equal(1)
        db.rollbackIfActive()
        val name = session
          .single("select name from " + tableName + " where id = ?", 1) { rs =>
            rs.string("name")
          }
          .get
        name should equal("name1")
      }
    }
  }

  it should "bind java.util.Date as java.sql.Timestamp" in {
    DB autoCommit { implicit session =>
      try {
        SQL(
          "create table dbsessionspec_judate (id integer primary key, date timestamp)"
        ).execute.apply()
        SQL("insert into dbsessionspec_judate values (?, ?)")
          .bind(1, new java.util.Date())
          .update
          .apply()
      } finally {
        SQL("drop table dbsessionspec_judate").execute.apply()
      }
    }
  }

  it should "be able to get a generated key" in {
    DB autoCommit { implicit session =>
      try {
        // NOTE: id column should be the first one for PostgreSQL
        try {
          SQL(
            "create table dbsessionspec_genkey (id integer generated always as identity(start with 0), name varchar(30))"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsessionspec_genkey (id integer auto_increment, primary key(id), name varchar(30))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsessionspec_genkey (id serial not null, primary key(id), name varchar(30))"
                ).execute.apply()
            }
        }
        var id = -1L
        val before = (stmt: PreparedStatement) => {}
        val after = (stmt: PreparedStatement) => {
          val rs = stmt.getGeneratedKeys
          while (rs.next()) {
            id =
              if (
                driverClassName == "org.h2.Driver" || driverClassName == "com.mysql.jdbc.Driver"
              ) rs.getLong(1)
              else rs.getLong("id")
          }
        }
        session.updateWithFilters(
          true,
          before,
          after,
          "insert into dbsessionspec_genkey (name) values (?)",
          "xxx"
        )
        id should be <= 1L
        session.updateWithFilters(
          true,
          before,
          after,
          "insert into dbsessionspec_genkey (name) values (?)",
          "xxx"
        )
        id should be <= 2L

        val ids: collection.Seq[Long] =
          session.batchAndReturnGeneratedKey[collection.Seq](
            "insert into dbsessionspec_genkey (name) values (?)",
            Seq(Seq("XXX"), Seq("XXX"), Seq("XXX")): _*
          )
        ids.size should equal(3)
        ids.last should be <= 5L
        // for Oracle DB
        //  {
        //    val ids: collection.Seq[Long] = session.batchAndReturnSpecifiedGeneratedKey(
        //      "insert into dbsessionspec_genkey (name) values (?)", "id", Seq(Seq("XXX"), Seq("XXX"), Seq("XXX")): _*)
        //    ids.size should equal(3)
        //    ids.last should be <= 8L
        //  }
      } finally {
        SQL("drop table dbsessionspec_genkey").execute.apply()
      }
    }
  }

  it should "be able to updateAndReturnGeneratedKey" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsessionspec_update_genkey (id integer generated always as identity(start with 0), name varchar(30))"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsessionspec_update_genkey (id integer auto_increment, name varchar(30), primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsessionspec_update_genkey (id serial not null, name varchar(30), primary key(id))"
                ).execute.apply()
            }
        }

        val id1 = SQL(
          "insert into dbsessionspec_update_genkey (name) values (?)"
        ).bind("xxx").updateAndReturnGeneratedKey.apply()
        id1 should be <= 1L
        val id2 = SQL(
          "insert into dbsessionspec_update_genkey (name) values (?)"
        ).bind("xxx").updateAndReturnGeneratedKey.apply()
        id2 should be <= 2L
        val id3 = SQL(
          "insert into dbsessionspec_update_genkey (name) values (?)"
        ).bind("xxx").updateAndReturnGeneratedKey.apply()
        id3 should be <= 3L
        val id4 = SQL(
          "insert into dbsessionspec_update_genkey (name) values (?)"
        ).bind("xxx").updateAndReturnGeneratedKey.apply()
        id4 should be <= 4L
      } finally {
        SQL("drop table dbsessionspec_update_genkey").execute.apply()
      }
    }
  }

  it should "be able to updateAndReturnGeneratedKey with key" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsessionspec_update_genkey2 (name varchar(30), id integer generated always as identity(start with 0))"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsessionspec_update_genkey2 (name varchar(30), id integer auto_increment, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsessionspec_update_genkey2 (name varchar(30), id serial not null, primary key(id))"
                ).execute.apply()
            }
        }

        if (
          driverClassName == "org.h2.Driver" || driverClassName == "com.mysql.jdbc.Driver"
        ) {
          val id1 = SQL(
            "insert into dbsessionspec_update_genkey2 (name) values (?)"
          ).bind("xxx").updateAndReturnGeneratedKey.apply()
          id1 should be <= 1L
        } else {
          val id1 = SQL(
            "insert into dbsessionspec_update_genkey2 (name) values (?)"
          ).bind("xxx").updateAndReturnGeneratedKey("id").apply()
          id1 should be <= 1L
        }
        val id2 = SQL(
          "insert into dbsessionspec_update_genkey2 (name) values (?)"
        ).bind("xxx").updateAndReturnGeneratedKey(2).apply()
        id2 should be <= 2L
      } finally {
        SQL("drop table dbsessionspec_update_genkey2").execute.apply()
      }
    }
  }

  it should "work with datetime values" in {

    val date = new DateTime(2012, 5, 3, 13, 40, 0, 0).toDate
    execute(
      "DateTime all",
      date.toJodaDateTime,
      date.toJodaDateTime,
      date.toJodaDateTime
    )
    execute(
      "LocalDateTime all",
      date.toJodaLocalDateTime,
      date.toJodaLocalDateTime,
      date.toJodaLocalDateTime
    )
    execute(
      "LocalDate and LocalTime all",
      date.toJodaLocalDate,
      date.toJodaLocalTime,
      date.toJodaLocalDateTime
    )
    execute(
      "java.sql.Timestamp all",
      date.toSqlTimestamp,
      date.toSqlTimestamp,
      date.toSqlTimestamp
    )
    execute("java.sql/java.util mixed", date.toSqlDate, date.toSqlTime, date)
    execute("java.util.Date all", date, date, date)

    def execute(label: String, date: Any, time: Any, timestamp: Any): Unit = {
      log.warn("datetime check: " + label)
      DB autoCommit { implicit session =>
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
            case e: Exception =>
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
                case e: Exception =>
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

          // clean table
          SQL("delete from dbsessionspec_dateTimeValues").update.apply()

          SQL("""
            insert into dbsessionspec_dateTimeValues
              (date_value, time_value, timestamp_value)
              values
              (?, ?, ?)
                 """).bind(date, time, timestamp).update.apply()

          SQL(
            "select * from dbsessionspec_dateTimeValues where timestamp_value = ?"
          ).bind(timestamp)
            .map { rs =>
              (
                rs.date("date_value"),
                rs.time("time_value"),
                rs.timestamp("timestamp_value")
              )
            }
            .first
            .apply()
            .map {
              case (
                  d: java.sql.Date,
                  t: java.sql.Time,
                  ts: java.sql.Timestamp
                ) =>
                // java.sql.Date
                d.toJodaLocalDate.getYear should equal(2012)
                d.toJodaLocalDate.getMonthOfYear should equal(5)
                d.toJodaLocalDate.getDayOfMonth should equal(3)

                // java.sql.Time
                t.toJodaLocalTime.getHourOfDay should equal(13)
                t.toJodaLocalTime.getMinuteOfHour should equal(40)
                t.toJodaLocalTime.getSecondOfMinute should equal(0)
                t.toJodaLocalTime.getMillisOfSecond should equal(0)

                // java.sql.Timestamp
                ts.toJodaDateTime.getYear should equal(2012)
                ts.toJodaDateTime.getMonthOfYear should equal(5)
                ts.toJodaDateTime.getDayOfMonth should equal(3)
                ts.toJodaDateTime.getHourOfDay should equal(13)
                ts.toJodaDateTime.getMinuteOfHour should equal(40)
                ts.toJodaDateTime.getSecondOfMinute should equal(0)
                ts.toJodaDateTime.getMillisOfSecond should equal(0)

            } orElse {
            fail("Expected value is not found.")
          }

          // joda-time API support
          {
            import org.joda.time._
            SQL(
              "select * from dbsessionspec_dateTimeValues where timestamp_value = ?"
            ).bind(timestamp)
              .map { rs =>
                (
                  rs.jodaLocalDate("date_value"),
                  rs.jodaLocalTime("time_value"),
                  rs.jodaDateTime("timestamp_value"),
                  rs.jodaLocalDateTime("timestamp_value")
                )
              }
              .first
              .apply()
              .map {
                case (
                    d: LocalDate,
                    t: LocalTime,
                    ts: DateTime,
                    ldt: LocalDateTime
                  ) =>
                  // LocalDate
                  d.getYear should equal(2012)
                  d.getMonthOfYear should equal(5)
                  d.getDayOfMonth should equal(3)

                  // LocalTime
                  t.getHourOfDay should equal(13)
                  t.getMinuteOfHour should equal(40)
                  t.getSecondOfMinute should equal(0)
                  t.getMillisOfSecond should equal(0)

                  // DateTime
                  ts.getYear should equal(2012)
                  ts.getMonthOfYear should equal(5)
                  ts.getDayOfMonth should equal(3)
                  ts.getHourOfDay should equal(13)
                  ts.getMinuteOfHour should equal(40)
                  ts.getSecondOfMinute should equal(0)
                  ts.getMillisOfSecond should equal(0)

                  // LocalDateTime
                  ldt.getYear should equal(2012)
                  ldt.getMonthOfYear should equal(5)
                  ldt.getDayOfMonth should equal(3)
                  ldt.getHourOfDay should equal(13)
                  ldt.getMinuteOfHour should equal(40)
                  ldt.getSecondOfMinute should equal(0)
                  ldt.getMillisOfSecond should equal(0)

              } orElse {
              fail("Expected value is not found.")
            }
          }

        } finally {
          try {
            SQL("drop table dbsessionspec_dateTimeValues").execute.apply()
          } catch {
            case e: Exception =>
          }
        }
      }
    }

  }

  it should "work with short values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_short_values (id bigint generated always as identity, s smallint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_work_with_short_values (id bigint auto_increment, s smallint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_work_with_short_values (id serial not null, s smallint, primary key(id))"
                ).execute.apply()
            }
        }
        val s: Short = 123
        SQL("insert into dbsession_work_with_short_values (s) values (?)")
          .bind(s)
          .update
          .apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_short_values").execute.apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "work with Scala BigDecimal values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_scala_big_decimal_values (id bigint generated always as identity, s bigint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_work_with_scala_big_decimal_values (id bigint auto_increment, s bigint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_work_with_scala_big_decimal_values (id serial not null, s bigint, primary key(id))"
                ).execute.apply()
            }
        }
        val s: BigDecimal = BigDecimal(123)
        SQL(
          "insert into dbsession_work_with_scala_big_decimal_values (s) values (?)"
        ).bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_scala_big_decimal_values").execute
            .apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "work with Java BigDecimal values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_java_big_decimal_values (id bigint generated always as identity, s bigint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_work_with_java_big_decimal_values (id bigint auto_increment, s bigint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_work_with_java_big_decimal_values (id serial not null, s bigint, primary key(id))"
                ).execute.apply()
            }
        }
        val s: BigDecimal = BigDecimal(123)
        SQL(
          "insert into dbsession_work_with_java_big_decimal_values (s) values (?)"
        ).bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_java_big_decimal_values").execute
            .apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "work with Scala BigInt values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_scala_big_int_values (id bigint generated always as identity, s bigint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_work_with_scala_big_int_values (id bigint auto_increment, s bigint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_work_with_scala_big_int_values (id serial not null, s bigint, primary key(id))"
                ).execute.apply()
            }
        }
        val s: BigInt = BigInt(123)
        SQL(
          "insert into dbsession_work_with_scala_big_int_values (s) values (?)"
        ).bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_scala_big_int_values").execute
            .apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "work with Java BigInteger values" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_java_big_integer_values (id bigint generated always as identity, s bigint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_work_with_java_big_integer_values (id bigint auto_increment, s bigint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_work_with_java_big_integer_values (id serial not null, s bigint, primary key(id))"
                ).execute.apply()
            }
        }
        val s: BigInt = BigInt(123)
        SQL(
          "insert into dbsession_work_with_java_big_integer_values (s) values (?)"
        ).bind(s).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_java_big_integer_values").execute
            .apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
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
            v_byte int, 
            v_double double, 
            v_float real, 
            v_int int, 
            v_long bigint, 
            v_short smallint,
            v_timestamp timestamp
          )
                 """).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL("""
          create table dbsession_work_with_optional_values (
            id bigint auto_increment,
            v_boolean boolean, 
            v_byte int, 
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
              case e: Exception =>
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
                        """)
          .bind(None, None, None, None, None, None, None, None)
          .updateAndReturnGeneratedKey
          .apply()

        case class Result(
          vBoolean: Option[Boolean],
          vByte: Option[Byte],
          vDouble: Option[Double],
          vFloat: Option[Float],
          vInt: Option[Int],
          vLong: Option[Long],
          vShort: Option[Short],
          vTimestamp: Option[DateTime]
        )

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

        def assertUnexpected(resultOpt: Option[Result]): Unit = {
          resultOpt.isDefined should be(true)
          val result = resultOpt.get
          result.vBoolean.isDefined should be(true)
          result.vByte.isDefined should be(true)
          result.vDouble.isDefined should be(true)
          result.vFloat.isDefined should be(true)
          result.vInt.isDefined should be(true)
          result.vLong.isDefined should be(true)
          result.vShort.isDefined should be(true)
          result.vTimestamp.isDefined should be(false)
        }

        // should use nullable*** methods
        assert(
          SQL("select * from dbsession_work_with_optional_values where id = ?")
            .bind(id)
            .map { rs =>
              Result(
                vBoolean = rs.booleanOpt("v_boolean"),
                vByte = rs.byteOpt("v_byte"),
                vDouble = rs.doubleOpt("v_double"),
                vFloat = rs.floatOpt("v_float"),
                vInt = rs.intOpt("v_int"),
                vLong = rs.longOpt("v_long"),
                vShort = rs.shortOpt("v_short"),
                vTimestamp = rs.jodaDateTimeOpt("v_timestamp")
              )
            }
            .single
            .apply()
        )

        assert(
          SQL("select * from dbsession_work_with_optional_values where id = ?")
            .bind(id)
            .map { rs =>
              Result(
                vBoolean =
                  Option(rs.nullableBoolean("v_boolean").asInstanceOf[Boolean]),
                vByte = Option(rs.nullableByte("v_byte").asInstanceOf[Byte]),
                vDouble =
                  Option(rs.nullableDouble("v_double").asInstanceOf[Double]),
                vFloat =
                  Option(rs.nullableFloat("v_float").asInstanceOf[Float]),
                vInt = Option(rs.nullableInt("v_int").asInstanceOf[Int]),
                vLong = Option(rs.nullableLong("v_long").asInstanceOf[Long]),
                vShort =
                  Option(rs.nullableShort("v_short").asInstanceOf[Short]),
                vTimestamp =
                  Option(rs.timestamp("v_timestamp")).map(_.toJodaDateTime)
              )
            }
            .single
            .apply()
        )

        // should use nullable*** methods
        intercept[ResultSetExtractorException](
          SQL("select * from dbsession_work_with_optional_values where id = ?")
            .bind(id)
            .map { rs =>
              Result(
                vBoolean = opt[Boolean](rs.boolean("v_boolean")),
                vByte = opt[Byte](rs.byte("v_byte")),
                vDouble = opt[Double](rs.double("v_double")),
                vFloat = opt[Float](rs.float("v_float")),
                vInt = opt[Int](rs.int("v_int")),
                vLong = opt[Long](rs.long("v_long")),
                vShort = opt[Short](rs.short("v_short")),
                vTimestamp =
                  Option(rs.timestamp("v_timestamp")).map(_.toJodaDateTime)
              )
            }
            .single
            .apply()
        )

        assert(
          SQL("select * from dbsession_work_with_optional_values where id = ?")
            .bind(id)
            .map { rs =>
              Result(
                vBoolean = opt[Boolean](rs.nullableBoolean("v_boolean")),
                vByte = opt[Byte](rs.nullableByte("v_byte")),
                vDouble = opt[Double](rs.nullableDouble("v_double")),
                vFloat = opt[Float](rs.nullableFloat("v_float")),
                vInt = opt[Int](rs.nullableInt("v_int")),
                vLong = opt[Long](rs.nullableLong("v_long")),
                vShort = opt[Short](rs.nullableShort("v_short")),
                vTimestamp =
                  Option(rs.timestamp("v_timestamp")).map(_.toJodaDateTime)
              )
            }
            .single
            .apply()
        )

        assert(
          SQL("select * from dbsession_work_with_optional_values where id = ?")
            .bind(id)
            .map { rs =>
              Result(
                vBoolean = rs.booleanOpt("v_boolean"),
                vByte = rs.byteOpt("v_byte"),
                vDouble = rs.doubleOpt("v_double"),
                vFloat = rs.floatOpt("v_float"),
                vInt = rs.intOpt("v_int"),
                vLong = rs.longOpt("v_long"),
                vShort = rs.shortOpt("v_short"),
                vTimestamp =
                  Option(rs.timestamp("v_timestamp")).map(_.toJodaDateTime)
              )
            }
            .single
            .apply()
        )

      } finally {
        try {
          SQL("drop table dbsession_work_with_optional_values").execute.apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "execute insert with InputStream values" in {
    DB autoCommit { implicit s =>
      try {
        try {
          SQL("create table image_data (name varchar(255), data blob);").execute
            .apply()
        } catch {
          case e: Exception =>
            // PostgreSQL doesn't have blob
            SQL(
              "create table image_data (name varchar(255), data bytea);"
            ).execute.apply()
        }
        using(this.getClass.getClassLoader.getResourceAsStream("google.png")) {
          stream =>
            try {
              SQL(
                "insert into image_data (name, data) values ({name}, {data});"
              )
                .bindByName("name" -> "logo", "data" -> stream)
                .update
                .apply()
            } catch {
              case e: Exception =>
                // PostgreSQL does not support #setBinaryStream
                if (url.startsWith("jdbc:postgresql")) println(e.getMessage)
                else fail("Failed to insert data because " + e.getMessage, e)
            }
        }
        SQL("select * from image_data;")
          .map(rs => rs.binaryStream("data"))
          .single
          .apply()
          .map { bs =>
            using(new java.io.ByteArrayOutputStream) { bos =>
              var next: Int = bs.read()
              while (next > -1) {
                bos.write(next)
                next = bs.read()
              }
              bos.flush()
              bos.toByteArray().size should equal(7007)
            }
          }
      } finally {
        try {
          SQL("drop table image_data;").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  it should "execute insert with byte array values" in {
    DB autoCommit { implicit s =>
      try {
        try {
          SQL(
            "create table image_data2 (name varchar(255), data blob);"
          ).execute.apply()
        } catch {
          case e: Exception =>
            // PostgreSQL doesn't have blob
            SQL(
              "create table image_data2 (name varchar(255), data bytea);"
            ).execute.apply()
        }
        using(this.getClass.getClassLoader.getResourceAsStream("google.png")) {
          stream =>
            using(new java.io.ByteArrayOutputStream) { bos =>
              var next: Int = stream.read()
              while (next > -1) {
                bos.write(next)
                next = stream.read()
              }
              bos.flush()
              SQL(
                "insert into image_data2 (name, data) values ({name}, {data});"
              )
                .bindByName("name" -> "logo", "data" -> bos.toByteArray)
                .update
                .apply()
            }
        }
        SQL("select * from image_data2")
          .map(rs => rs.binaryStream("data"))
          .single
          .apply()
          .map { bs =>
            using(new java.io.ByteArrayOutputStream) { bos =>
              var next: Int = bs.read()
              while (next > -1) {
                bos.write(next)
                next = bs.read()
              }
              bos.flush()
              bos.toByteArray().size should equal(7007)
            }
          }
      } finally {
        try {
          SQL("drop table image_data2").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  // https://github.com/scalikejdbc/scalikejdbc/issues/218
  it should "expose StatementExecutor" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_issue_218 (id bigint generated always as identity, s bigint)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            try {
              SQL(
                "create table dbsession_issue_218 (id bigint auto_increment, s bigint, primary key(id))"
              ).execute.apply()
            } catch {
              case e: Exception =>
                SQL(
                  "create table dbsession_issue_218 (id serial not null, s bigint, primary key(id))"
                ).execute.apply()
            }
        }
        val sql =
          SQL("insert into dbsession_issue_218 (s) values (?)").bind(123).update
        val executor =
          session.toStatementExecutor(sql.statement, sql.parameters)
        import ExecutionContext.Implicits.global
        scala.concurrent.Future {
          Thread.sleep(10L)
          executor.executeUpdate()
        }
        executor.underlying.cancel()
      } finally {
        try {
          SQL("drop table dbsession_issue_218").execute.apply()
        } catch {
          case e: Exception => e.printStackTrace
        }
      }
    }
  }

  it should "work with ParameterBinder" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL(
            "create table dbsession_work_with_parameter_binder (id bigint, data blob)"
          ).execute.apply()
        } catch {
          case e: Exception =>
            // PostgreSQL doesn't have blob
            SQL(
              "create table dbsession_work_with_parameter_binder (id bigint, data bytea)"
            ).execute.apply()
        }
        val bytes = scala.Array[Byte](1, 2, 3, 4, 5, 6, 7)
        val in = new ByteArrayInputStream(bytes)
        val v = ParameterBinder(
          value = in,
          binder = (stmt: PreparedStatement, idx: Int) =>
            stmt.setBinaryStream(idx, in, bytes.length)
        )
        SQL(
          "insert into dbsession_work_with_parameter_binder (data) values (?)"
        ).bind(v).update.apply()
      } finally {
        try {
          SQL("drop table dbsession_work_with_parameter_binder").execute.apply()
        } catch {
          case e: Exception => e.printStackTrace()
        }
      }
    }
  }

  it should "convert timeZone" in {
    import java.util.TimeZone
    import org.joda.time.DateTimeZone

    val time =
      new DateTime(2016, 1, 9, 2, 43, 42, DateTimeZone.forID("Asia/Tokyo"))

    val castToString: String = if (driverClassName.contains("mysql")) {
      "date_format(t, '%Y-%m-%d %H:%i:%S')"
    } else {
      "to_char(t, 'YYYY-MM-DD HH24:MI:SS')"
    }

    try {
      DB autoCommit { implicit session =>
        try SQL("drop table zone_test").execute.apply()
        catch { case e: Exception => }

        SQL("create table zone_test (id int primary key, t timestamp)").execute
          .apply()
      }

      /**
       * execute with Asia/Tokyo timezone
       */
      DB autoCommit { session =>
        implicit val jstSession = DBSession(
          conn = session.conn,
          connectionAttributes =
            session.connectionAttributes.copy(timeZoneSettings =
              TimeZoneSettings(true, TimeZone.getTimeZone("Asia/Tokyo"))
            )
        )

        SQL("insert into zone_test values (?, ?)")
          .bind(1, time)
          .execute
          .apply()(jstSession)
        val jstString = SQL(
          s"select $castToString as s from zone_test where id = 1"
        ).map(_.string("s")).single.apply().get
        jstString should equal(time.toString("yyyy-MM-dd HH:mm:ss"))

        val expectedTime1 = SQL("select t from zone_test where id = 1")
          .map(_.jodaDateTime("t"))
          .single
          .apply()
          .get
        expectedTime1.isEqual(time) should equal(true)
      }

      /**
       * execute with UTC timezone
       */
      DB autoCommit { session =>
        implicit val utcSession = DBSession(
          conn = session.conn,
          connectionAttributes =
            session.connectionAttributes.copy(timeZoneSettings =
              TimeZoneSettings(true, TimeZone.getTimeZone("UTC"))
            )
        )

        SQL("insert into zone_test values (?, ?)").bind(2, time).execute.apply()
        val utcString = SQL(
          s"select $castToString as s from zone_test where id = 2"
        ).map(_.string("s")).single.apply().get
        utcString should equal(
          time
            .withZone(DateTimeZone.forID("UTC"))
            .toString("yyyy-MM-dd HH:mm:ss")
        )

        val expectedTime2 = SQL("select t from zone_test where id = 2")
          .map(_.jodaDateTime("t"))
          .single
          .apply()
          .get
        expectedTime2.isEqual(time) should equal(true)
      }
    } finally {
      DB autoCommit { implicit session =>
        try SQL("drop table zone_test").execute.apply()
        catch { case e: Exception => }
      }
    }
  }
}
