package scalikejdbc

import org.scalatest._
import java.sql.SQLException
import util.control.Exception._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DB_SQLOperationSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings
  with LoanPattern {

  val tableNamePrefix =
    "emp_DB_SQLOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(SQL Operation)"

  it should "be available" in {
    using(ConnectionPool.borrow()) { conn =>
      val db = DB(conn)
      db should not be null
    }
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB readOnly { implicit session =>
        GlobalSettings.loggingSQLAndTime =
          LoggingSQLAndTimeSettings(enabled = true, logLevel = "info")
        val result = SQL(
          "select * from " + tableName + " where name = 'name1' and id = /*'id*/123;"
        )
          .bindByName("id" -> 1)
          .map(rs => Some(rs.string("name")))
          .toList
          .apply()
        result.size should equal(1)
        GlobalSettings.loggingSQLAndTime =
          LoggingSQLAndTimeSettings(enabled = false)
      }
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      implicit val session: DBSession = DB.readOnlySession()
      try {
        val result = SQL("select * from " + tableName + "")
          .map(rs => Some(rs.string("name")))
          .toList
          .apply()
        result.size should be > 0
      } finally {
        session.close()
      }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[SQLException] {
        DB readOnly { implicit session =>
          SQL("update " + tableName + " set name = ?")
            .bind("xxx")
            .executeUpdate
            .apply()
        }
      }
    }
  }

  // --------------------
  // autoCommit

  it should "execute query in autoCommit block" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB autoCommit { implicit session =>
        SQL("select * from " + tableName + "")
          .map(rs => Some(rs.string("name")))
          .toList
          .apply()
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      implicit val session: DBSession = DB.autoCommitSession()
      try {
        val list = SQL("select id from " + tableName + " order by id")
          .map(_.int("id"))
          .toList
          .apply()
        list(0) should equal(1)
        list(1) should equal(2)
      } finally {
        session.close()
      }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB autoCommit { implicit session =>
        SQL("select id from " + tableName + " where id = ?")
          .bind(1)
          .map(_.int("id"))
          .toOption
          .apply()
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[TooManyRowsException] {
        DB autoCommit { implicit session =>
          SQL("select id from " + tableName + "")
            .map(rs => Some(rs.int("id")))
            .toOption
            .apply()
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = DB readOnly { implicit session =>
        SQL("select * from " + tableName + " where id = ?")
          .bind(1)
          .map(extractName)
          .toOption
          .apply()
      }
      name.get should equal("name1")
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB autoCommit { implicit session =>
        SQL("select id from " + tableName + "")
          .map(rs => Some(rs.int("id")))
          .toList
          .apply()
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB autoCommit { implicit session =>
        SQL("select id from " + tableName + "")
          .map(_.int("id"))
          .toIterable
          .apply()
          .foreach { id =>
            println(id)
          }
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB autoCommit { implicit session =>
        SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdate
          .apply()
      }
      count should equal(1)
      val name = DB autoCommit { implicit session =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .toOption
          .apply()
          .get
      }
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlockAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = DB readOnly { implicit s =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .toOption
          .apply()
          .get
      }
      name should equal("name1")
      val count = DB autoCommit { implicit s =>
        SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdate
          .apply()
      }
      count should equal(1)
    }
  }

  it should "not rollback in autoCommit block" in {
    val tableName = tableNamePrefix + "_neverRollbackInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      try {
        DB autoCommit { implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate
            .apply()
          throw new RuntimeException
        }
      } catch { case e: Exception => }

      // should be committed
      val name = DB readOnly { implicit s =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .single
          .apply()
          .get
      }
      name should equal("foo")
    }
  }

  // --------------------
  // localTx

  it should "execute single in localTx block" in {
    val tableName = tableNamePrefix + "_singleInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx { implicit s =>
        SQL("select id from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("id"))
          .toOption
          .apply()
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx { implicit s =>
        SQL("select id from " + tableName + "")
          .map(rs => Some(rs.string("id")))
          .toList
          .apply()
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB localTx { implicit s =>
        SQL("update " + tableName + " set name = ? where id = ?")
          .bind("foo", 1)
          .executeUpdate
          .apply()
      }
      count should equal(1)
      val name = DB localTx { implicit s =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .toOption
          .apply()
          .get
      }
      name should equal("foo")
    }
  }

  it should "rollback when using localTx" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val count = db localTx { implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate
            .apply()
        }
        count should equal(1)
        db.rollbackIfActive()
      }

      val name = DB localTx { implicit s =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .single
          .apply()
          .get
      }
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      try {
        DB localTx { implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate
            .apply()
          throw new RuntimeException
        }
      } catch { case e: Exception => }

      // should not be committed
      val name = DB readOnly { implicit s =>
        SQL("select name from " + tableName + " where id = ?")
          .bind(1)
          .map(_.string("name"))
          .single
          .apply()
          .get
      }
      name should equal("name1")
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[IllegalStateException] {
        using(DB(ConnectionPool.borrow())) { db =>
          db withinTx { implicit session =>
            SQL("select * from " + tableName + "")
              .map(rs => Some(rs.string("name")))
              .list
              .apply()
          }
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val result = db withinTx { implicit session =>
          SQL("select * from " + tableName + "")
            .map(rs => Some(rs.string("name")))
            .list
            .apply()
        }
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute query in withinTx session" in {
    val tableName = tableNamePrefix + "_queryInWithinTxSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()
        try {
          val result = SQL("select * from " + tableName + "")
            .map(rs => Some(rs.string("name")))
            .list
            .apply()
          result.size should be > 0
          db.rollbackIfActive()
        } finally {
          session.close()
        }
      }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val result = db withinTx { implicit s =>
          SQL("select id from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("id"))
            .single
            .apply()
        }
        result.get should equal("1")
        db.rollbackIfActive()
      }
    }
  }

  it should "execute list in withinTx block" in {
    val tableName = tableNamePrefix + "_listInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val result = db withinTx { implicit s =>
          SQL("select id from " + tableName + "")
            .map(rs => Some(rs.string("id")))
            .list
            .apply()
        }
        result.size should equal(2)
        db.rollbackIfActive()
      }
    }
  }

  it should "execute update in withinTx block" in {
    val tableName = tableNamePrefix + "_updateInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val count = db withinTx { implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate
            .apply()
        }
        count should equal(1)
        val name = (db withinTx { implicit s =>
          SQL("select name from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("name"))
            .single
            .apply()
        }).get
        name should equal("foo")
        db.rollback()
      }
    }
  }

  it should "rollback in withinTx block" in {
    val tableName = tableNamePrefix + "_rollbackInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val count = db withinTx { implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate
            .apply()
        }
        count should equal(1)
        db.rollback()
        db.begin()
        val name = (db withinTx { implicit s =>
          SQL("select name from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("name"))
            .single
            .apply()
        }).get
        name should equal("name1")
      }
    }
  }

  it should "execute batch in withinTx block" in {
    val tableName = tableNamePrefix + "_batch"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val count1 = db withinTx { implicit s =>
          val params: Seq[Seq[Any]] = (1001 to 2000).map { i =>
            Seq(i, "name" + i.toString)
          }
          SQL("insert into " + tableName + " (id, name) values (?, ?)")
            .batch(params: _*)
            .apply()
        }
        count1.size should equal(1000)

        val count2 = db withinTx { implicit s =>
          // https://github.com/scalikejdbc/scalikejdbc/issues/481
          SQL("insert into " + tableName + " (id, name) values ({id}, {name})")
            .batchByName(Seq.empty[Seq[(String, Any)]]: _*)
            .apply()

          val params: Seq[Seq[(String, Any)]] = (2001 to 3000).map { i =>
            Seq[(String, Any)]("id" -> i, "name" -> ("name" + i.toString))
          }
          SQL("insert into " + tableName + " (id, name) values ({id}, {name})")
            .batchByName(params: _*)
            .apply()
        }
        count2.size should equal(1000)
        db.rollback()
      }
    }
  }

  it should "never get stuck" in {
    // Note: This is not an issue. Just only related to how to write specs.
    val tableName = tableNamePrefix + "_gettingstuck"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        implicit val session = db.withinTxSession()

        val params1: Seq[Seq[Any]] = (1001 to 2000).map { i =>
          Seq(i, "name" + i.toString)
        }
        val count1 = SQL(
          "insert into " + tableName + " (id, name) values (?, ?)"
        ).batch(params1: _*).apply()
        count1.size should equal(1000)

        val params2: Seq[Seq[(String, Any)]] = (2001 to 2003).map { i =>
          Seq[(String, Any)]("id" -> i, "name" -> ("name" + i.toString))
        }
        try {
          val count2 = SQL(
            "insert into " + tableName + " (id, name) values (?, {name})"
          ).batchByName(params2: _*).apply()
          count2.size should equal(1000)
        } catch {
          case e: Exception =>
          // Exception should be caught here. It's not a bug.
        }
        // https://github.com/scalikejdbc/scalikejdbc/issues/481
        SQL("insert into " + tableName + " (id, name) values (?, {name})")
          .batchByName(Seq.empty[Seq[(String, Any)]]: _*)
          .apply()

        db.rollback()
      }
    }
  }

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      import scala.concurrent.ExecutionContext.Implicits.global
      scala.concurrent.Future {
        using(DB(ConnectionPool.borrow())) { db =>
          db.begin()
          implicit val session = db.withinTxSession()
          SQL("update " + tableName + " set name = ? where id = ?")
            .bind("foo", 1)
            .executeUpdate()
          Thread.sleep(1000L)
          val name = SQL("select name from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("name"))
            .single
            .apply()
          name.get should equal("foo")
          db.rollback()
        }
      }
      scala.concurrent.Future {
        using(DB(ConnectionPool.borrow())) { db =>
          db.begin()
          implicit val session = db.withinTxSession()
          Thread.sleep(200L)
          val name = SQL("select name from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("name"))
            .single
            .apply()
          name.get should equal("name1")
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      using(ConnectionPool.borrow()) { conn =>
        val name = DB(conn) autoCommit { implicit session =>
          SQL("select name from " + tableName + " where id = ?")
            .bind(1)
            .map(_.string("name"))
            .single
            .apply()
        }
        name.get should equal("name1")
      }
    }
  }

  it should "solve issue #30" in {
    GlobalSettings.loggingSQLAndTime =
      new LoggingSQLAndTimeSettings(enabled = true, logLevel = "info")
    try {
      DB autoCommit { implicit session =>
        try {
          SQL("drop table issue30;").execute.apply()
        } catch { case e: Exception => }
        SQL("""
        create table issue30 (
          id bigint not null,
          data1 varchar(255) not null,
          data2 varchar(255) not null
        );""").execute.apply()
        SQL("""insert into issue30 (id, data1, data2) values(?, ?, ?)""")
          .batch((101 to 121) map { i => Seq(i, "a", "b") }: _*)
          .apply()
        SQL("""insert into issue30 (id, data1, data2) values(?, ?, ?)""")
          .batch((201 to 205) map { i => Seq(i, "a", "b") }: _*)
          .apply()
      }
    } finally {
      try {
        DB autoCommit { implicit s =>
          SQL("drop table issue30;").execute.apply()
        }
      } catch { case e: Exception => }
      GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
    }
  }

  it should "have #toMap" in {
    val tableName = tableNamePrefix + "_toMap"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx { implicit s =>
        SQL("insert into " + tableName + " values (?, ?)")
          .bind(4, Option(null))
          .update
          .apply()
        SQL("select id, name from " + tableName + " where id = ?")
          .bind(4)
          .map(_.toMap())
          .single
          .apply()
      }
      result.isDefined should equal(true)
      if (result.get.get("ID").isDefined) {
        result.get.get("ID") should equal(Some(4))
        result.get.keys should equal(Set("ID"))
      } else {
        result.get.get("id") should equal(Some(4))
        result.get.keys should equal(Set("id"))
      }
      result.get.get("Name") should equal(None)
    }
  }

  it should "have #toSymbolMap" in {
    val tableName = tableNamePrefix + "_toMap"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx { implicit s =>
        SQL("insert into " + tableName + " values (?, ?)")
          .bind(4, Option(null))
          .update
          .apply()
        SQL("select id, name from " + tableName + " where id = ?")
          .bind(4)
          .map(_.toSymbolMap())
          .single
          .apply()
      }
      result.isDefined should equal(true)
      if (result.get.get(Symbol("ID")).isDefined) {
        result.get.get(Symbol("ID")) should equal(Some(4))
        result.get.keys should equal(Set(Symbol("ID")))
      } else {
        result.get.get(Symbol("id")) should equal(Some(4))
        result.get.keys should equal(Set(Symbol("id")))
      }
      result.get.get(Symbol("Name")) should equal(None)
    }
  }

}
