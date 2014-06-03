package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import java.sql.SQLException
import scala.util.control.Exception._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.ScalaFutures

class DBSpec extends FlatSpec with Matchers with BeforeAndAfter with Settings with LoanPattern with ScalaFutures {

  val tableNamePrefix = "emp_DBObjectSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB"

  it should "be a trait" in {
    val tableName = tableNamePrefix + "_trait"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db: DBConnection = DB(ConnectionPool.borrow())
      val result = db readOnly {
        session => session.list("select * from " + tableName + "")(rs => rs.string("name"))
      }
      result.size should be > 0
    }
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB readOnly {
        session => session.list("select * from " + tableName + "")(rs => rs.string("name"))
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = DB.readOnlySession()
      try {
        val result = session.list("select * from " + tableName + "")(rs => rs.string("name"))
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[SQLException] {
        DB readOnly {
          session => session.update("update " + tableName + " set name = ?", "xxx")
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
      val result = DB autoCommit {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = DB.autoCommitSession()
      try {
        val list = session.list("select id from " + tableName + " order by id")(rs => rs.int("id"))
        list(0) should equal(1)
        list(1) should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB autoCommit {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.int("id"))
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[TooManyRowsException] {
        DB autoCommit {
          _.single("select id from " + tableName + "")(rs => Some(rs.int("id")))
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = DB readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should equal("name1")
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB autoCommit {
        _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB autoCommit {
        _.foreach("select id from " + tableName + "")(rs => println(rs.int("id")))
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB autoCommit {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (DB autoCommit {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (DB readOnly {
        _.single("select name from " + tableName + " where id = ?", 1)(_.string("name"))
      }).get
      name should equal("name1")
      val count = DB autoCommit {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
    }
  }

  // --------------------
  // localTx

  it should "execute single in localTx block" in {
    val tableName = tableNamePrefix + "_singleInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = DB localTx {
        _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (DB localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "not be able to rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        val count = db localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should equal(1)
        db.rollbackIfActive()
        val name = (DB localTx {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).getOrElse("---")
        name should equal("foo")
      }
    }
  }

  // --------------------
  // futureLocalTx

  implicit val patienceTimeout = PatienceConfig(30.seconds)

  it should "execute single in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = DB futureLocalTx { s =>
        Future(s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")))
      }
      whenReady(fResult) { _ should equal(Some("1")) }
    }
  }

  it should "execute list in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = DB futureLocalTx { s =>
        Future(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      whenReady(fResult) { _.size should equal(2) }
    }
  }

  it should "execute update in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fCount = DB futureLocalTx { s =>
        Future(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      whenReady(fCount) {
        _ should equal(1)
      }
      val fName = fCount.flatMap { _ =>
        DB futureLocalTx (s => Future(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))))
      }
      whenReady(fName) { _ should be(Some("foo")) }
    }
  }

  it should "not be able to rollback in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      futureUsing(DB(ConnectionPool.borrow())) { db =>
        val fCount = DB futureLocalTx { s =>
          Future(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        whenReady(fCount) {
          _ should equal(1)
        }
        db.rollbackIfActive()
        val fName = DB futureLocalTx { s =>
          Future(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        whenReady(fName) { _ should equal(Some("foo")) }
        fName
      }
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block  before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[IllegalStateException] {
        using(DB(ConnectionPool.borrow())) { db =>
          db withinTx {
            session =>
              session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
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
        val result = db withinTx {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
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
        val session = db.withinTxSession()
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        db.begin()
        val result = db withinTx {
          _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
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
        val result = db withinTx {
          _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
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
        val count = db withinTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should equal(1)
        val name = (db withinTx {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
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
      using(DB(ConnectionPool.borrow())) {
        db =>
          db.begin()
          val count = db withinTx {
            _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          }
          count should equal(1)
          db.rollback()
          db.begin()
          val name = (db withinTx {
            _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          }).get
          name should equal("name1")
      }
    }
  }

  it should "fix issue #41 [library] LoggingSQLAndTime raises IndexOutOfBoundsException when '?' is included in SQL templates" in {
    val tableName = tableNamePrefix + "_issue41"
    ultimately({
      GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)
      TestUtils.deleteTable(tableName)
    }) {
      TestUtils.initialize(tableName)
      DB localTx { implicit s =>
        SQL("insert into " + tableName + " values (?,?)").bind(3, "so what?").update.apply()
      }
      GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
        enabled = true,
        logLevel = 'info
      )
      DB readOnly { implicit s =>
        val res1 = SQL("select * from " + tableName + " where name =  /* why? */ 'so what?' and id = ? /* really? */ -- line?")
          .bind(3)
          .map(rs => rs.string("name")).list.apply()
        res1.size should equal(1)
        val res2 = SQL("select * from " + tableName + " where name = /* why? */ 'so what?' and id = /*'id*/123 /* really? */ -- line?")
          .bindByName('id -> 3)
          .map(rs => rs.string("name")).list.apply()
        res2.size should equal(1)
      }
    }
  }

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      Future {
        using(DB(ConnectionPool.borrow())) { db =>
          db.begin()
          val session = db.withinTxSession()
          session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          Thread.sleep(1000L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "foo")
          db.rollback()
        }
      }
      Future {
        using(DB(ConnectionPool.borrow())) { db =>
          db.begin()
          val session = db.withinTxSession()
          Thread.sleep(200L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "name1")
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      val name = DB(ConnectionPool.borrow()) autoCommit {
        session =>
          session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }
      assert(name.get == "name1")
    }
  }

  // --------------------
  // metadata
  it should "work with db metadata" in {
    val tableName = tableNamePrefix + "_metadata"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB.getAllTableNames().size should be > (0)
    }
  }

  // https://github.com/scalikejdbc/scalikejdbc/issues/245
  it should "work with no pk table with MySQL" in {
    if (driverClassName == "com.mysql.jdbc.Driver") {
      val tableName = "issue245_" + System.currentTimeMillis
      try {
        DB autoCommit { implicit s =>
          SQL("create table `" + tableName + "` (some_setting tinyint(1) NOT NULL DEFAULT '1')").execute.apply()
        }
        val table = DB.getTable(tableName)
        table.isDefined should equal(true)
      } finally {
        try DB autoCommit { implicit s => SQL("drop table " + tableName).execute.apply() }
        catch { case e: Exception => }
      }
    }
  }

}
