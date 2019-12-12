package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import java.sql.SQLException
import cats.effect.IO
import util.control.Exception._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures

import ExecutionContext.Implicits.global

class NamedDBSpec extends FlatSpec with Matchers with BeforeAndAfter with Settings with LoanPattern with ScalaFutures {

  val tableNamePrefix = "emp_NamedDBSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "NamedDB"

  it should "be available" in {
    using(ConnectionPool.borrow(Symbol("named"))) { conn =>
      using(new DB(conn)) { db =>
        db should not be null
      }
    }
  }

  it should "be a trait " in {
    val tableName = tableNamePrefix + "_trait"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db: DBConnection = NamedDB(Symbol("named"))
      val result = db readOnly {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
    }
  }

  // --------------------
  // tx

  "#tx" should "not be available before beginning tx" in {
    using(NamedDB(Symbol("named"))) { db =>
      intercept[IllegalStateException] {
        db.tx.begin()
      }
      db.rollbackIfActive()
    }
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB(Symbol("named"))) { db =>
        val result = db readOnly {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        }
        result.size should be > 0
      }
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = NamedDB(Symbol("named")).readOnlySession()
      try {
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[SQLException] {
        NamedDB(Symbol("named")) readOnly {
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
      val result = NamedDB(Symbol("named")) autoCommit {
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
      val session = NamedDB(Symbol("named")).autoCommitSession()
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
      val result = NamedDB(Symbol("named")) autoCommit {
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
        NamedDB(Symbol("named")) autoCommit {
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
      val name: Option[String] = NamedDB(Symbol("named")) readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should equal("name1")
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB(Symbol("named")) autoCommit {
        _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      NamedDB(Symbol("named")) autoCommit {
        _.foreach("select id from " + tableName + "")(rs => println(rs.int("id")))
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB(Symbol("named"))) { db =>
        val count = NamedDB(Symbol("named")) autoCommit {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should equal(1)
        val name = (db autoCommit {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).get
        name should equal("foo")
      }
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updatInAutoCommitAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (NamedDB(Symbol("named")) readOnly {
        _.single("select name from " + tableName + " where id = ?", 1)(_.string("name"))
      }).get
      name should equal("name1")
      val count = NamedDB(Symbol("named")) autoCommit {
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
      val result = NamedDB(Symbol("named")) localTx {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB(Symbol("named")) localTx {
        _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = NamedDB(Symbol("named")) localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (NamedDB(Symbol("named")) localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB(Symbol("named"))) { db =>
        val count = db localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should equal(1)
        db.rollbackIfActive()
        val name = (NamedDB(Symbol("named")) localTx {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).getOrElse("---")
        name should equal("foo")
      }
    }
  }

  // --------------------
  // futureLocalTx

  implicit val patienceTimeout = PatienceConfig(10.seconds)

  it should "execute single in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB(Symbol("named")) futureLocalTx { s =>
        Future(s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")))
      }
      whenReady(fResult) { _ should equal(Some("1")) }
    }
  }

  it should "execute list in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB(Symbol("named")) futureLocalTx { s =>
        Future(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      whenReady(fResult) { _.size should equal(2) }
    }
  }

  it should "execute update in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fCount = NamedDB(Symbol("named")) futureLocalTx { s =>
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
      futureUsing(DB(ConnectionPool(Symbol("named")).borrow())) { db =>
        val fCount = NamedDB(Symbol("named")) futureLocalTx { s =>
          Future(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        whenReady(fCount) {
          _ should equal(1)
        }
        db.rollbackIfActive()
        val fName = NamedDB(Symbol("named")) futureLocalTx { s =>
          Future(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        whenReady(fName) { _ should equal(Some("foo")) }
        fName
      }
    }
  }

  it should "do rollback in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure = NamedDB(Symbol("named")) futureLocalTx { implicit s =>
        Future(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .map(_ => s.update("update foo should be rolled back"))
      }
      intercept[Exception] {
        Await.result(failure, 10.seconds)
      }
      val res = NamedDB(Symbol("named")) readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // ioLocalTx

  it should "execute single in ioLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB(Symbol("named")) ioLocalTx { s =>
        IO(s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")))
      }
      fResult.unsafeRunSync() should equal(Some("1"))
    }
  }

  it should "execute list in ioLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB(Symbol("named")) ioLocalTx { s =>
        IO(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      fResult.unsafeRunSync().size should equal(2)
    }
  }

  it should "execute update in ioLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fCount = NamedDB(Symbol("named")) ioLocalTx { s =>
        IO(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      val result1 = fCount.unsafeRunSync()

      result1 should equal(1)

      val fName = IO(result1).flatMap { _ =>
        DB ioLocalTx (s => IO(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))))
      }
      fName.unsafeRunSync() should be(Some("foo"))
    }
  }

  it should "not be able to rollback in ioLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      futureUsing(DB(ConnectionPool(Symbol("named")).borrow())) { db =>
        val fCount = NamedDB(Symbol("named")) ioLocalTx { s =>
          IO(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        fCount.unsafeRunSync() should equal(1)

        db.rollbackIfActive()
        val fName = NamedDB(Symbol("named")) ioLocalTx { s =>
          IO(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        fName.unsafeRunSync() should equal(Some("foo"))
        fName.unsafeToFuture()
      }
    }
  }

  it should "do rollback in ioLocalTx block" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure = NamedDB(Symbol("named")) ioLocalTx { implicit s =>
        IO(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .map(_ => s.update("update foo should be rolled back"))
      }
      intercept[Exception] {
        Await.result(failure.unsafeToFuture(), 10.seconds)
      }
      val res = NamedDB(Symbol("named")) readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[IllegalStateException] {
        NamedDB(Symbol("named")) withinTx {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB(Symbol("named"))) { db =>
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
      using(NamedDB(Symbol("named"))) { db =>
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
      using(NamedDB(Symbol("named"))) { db =>
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
      using(NamedDB(Symbol("named"))) { db =>
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
      using(NamedDB(Symbol("named"))) { db =>
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
    ultimately({
      ignoring(classOf[Throwable]) {
        DB(ConnectionPool.borrow(Symbol("named"))) autoCommit { _.execute("drop table " + tableName) }
      }
    }) {
      NamedDB(Symbol("named")) autoCommit {
        session =>
          handling(classOf[Throwable]) by {
            t =>
              try {
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              } catch {
                case e: Exception =>
                  session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              }
              session.update("delete from " + tableName)
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          } apply {
            session.single("select count(1) from " + tableName)(rs => rs.int(1))
            session.update("delete from " + tableName)
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          }
      }
      using(NamedDB(Symbol("named"))) {
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

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately({
      ignoring(classOf[Throwable]) {
        DB(ConnectionPool.borrow(Symbol("named"))) autoCommit { _.execute("drop table " + tableName) }
      }
    }) {
      NamedDB(Symbol("named")) autoCommit {
        session =>
          handling(classOf[Throwable]) by {
            t =>
              try {
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              } catch {
                case e: Exception =>
                  try {
                    session.execute("create table " + tableName + " (id int primary key, name varchar(30))")
                  } catch {
                    case e: Exception =>
                  }
              }
              session.update("delete from " + tableName)
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          } apply {
            session.single("select count(1) from " + tableName)(rs => rs.int(1))
            session.update("delete from " + tableName)
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          }
      }
      import scala.concurrent.ExecutionContext.Implicits.global
      scala.concurrent.Future {
        using(NamedDB(Symbol("named"))) { db =>
          db.begin()
          val session = db.withinTxSession()
          session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          Thread.sleep(1000L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "foo")
          db.rollback()
        }
      }
      scala.concurrent.Future {
        using(NamedDB(Symbol("named"))) { db =>
          db.begin()
          val session = db.withinTxSession()
          Thread.sleep(200L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "name1")
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      val name = NamedDB(Symbol("named")) autoCommit {
        session =>
          session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }
      assert(name.get == "name1")
    }
  }

}
