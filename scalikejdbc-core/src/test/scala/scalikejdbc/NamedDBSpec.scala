package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import java.sql.SQLException
import util.control.Exception._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures
import scalikejdbc.iomonads.MyIO

import ExecutionContext.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NamedDBSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings
  with LoanPattern
  with ScalaFutures {

  val tableNamePrefix =
    "emp_NamedDBSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "NamedDB"

  it should "be available" in {
    using(ConnectionPool.borrow("named")) { conn =>
      using(new DB(conn)) { db =>
        db should not be null
      }
    }
  }

  it should "be a trait " in {
    val tableName = tableNamePrefix + "_trait"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db: DBConnection = NamedDB("named")
      val result = db readOnly { session =>
        session.list("select * from " + tableName + "")(rs =>
          Some(rs.string("name"))
        )
      }
      result.size should be > 0
    }
  }

  // --------------------
  // tx

  "#tx" should "not be available before beginning tx" in {
    using(NamedDB("named")) { db =>
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
      using(NamedDB("named")) { db =>
        val result = db readOnly { session =>
          session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
        }
        result.size should be > 0
      }
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = NamedDB("named").readOnlySession()
      try {
        val result = session.list("select * from " + tableName + "")(rs =>
          Some(rs.string("name"))
        )
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[SQLException] {
        NamedDB("named") readOnly {
          _.update("update " + tableName + " set name = ?", "xxx")
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
      val result = NamedDB("named") autoCommit { session =>
        session.list("select * from " + tableName + "")(rs =>
          Some(rs.string("name"))
        )
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = NamedDB("named").autoCommitSession()
      try {
        val list =
          session.list("select id from " + tableName + " order by id")(
            _.int("id")
          )
        list(0) should equal(1)
        list(1) should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB("named") autoCommit {
        _.single("select id from " + tableName + " where id = ?", 1)(
          _.int("id")
        )
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[TooManyRowsException] {
        NamedDB("named") autoCommit {
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
      val name: Option[String] = NamedDB("named") readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should equal("name1")
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB("named") autoCommit {
        _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      NamedDB("named") autoCommit {
        _.foreach("select id from " + tableName + "")(rs =>
          println(rs.int("id"))
        )
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB("named")) { db =>
        val count = NamedDB("named") autoCommit {
          _.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        }
        count should equal(1)
        val name = (db autoCommit {
          _.single("select name from " + tableName + " where id = ?", 1)(
            _.string("name")
          )
        }).get
        name should equal("foo")
      }
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (NamedDB("named") readOnly {
        _.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      }).get
      name should equal("name1")
      val count = NamedDB("named") autoCommit {
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
      val result = NamedDB("named") localTx {
        _.single("select id from " + tableName + " where id = ?", 1)(
          _.string("id")
        )
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB("named") localTx {
        _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = NamedDB("named") localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (NamedDB("named") localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB("named")) { db =>
        val count = db localTx {
          _.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        }
        count should equal(1)
        db.rollbackIfActive()
        val name = (NamedDB("named") localTx {
          _.single("select name from " + tableName + " where id = ?", 1)(
            _.string("name")
          )
        }).getOrElse("---")
        name should equal("foo")
      }
    }
  }

  // --------------------
  // futureLocalTx

  implicit val patienceTimeout: PatienceConfig = PatienceConfig(10.seconds)

  it should "execute single in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB("named") futureLocalTx { s =>
        Future(
          s.single("select id from " + tableName + " where id = ?", 1)(
            _.string("id")
          )
        )
      }
      whenReady(fResult) { _ should equal(Some("1")) }
    }
  }

  it should "execute list in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fResult = NamedDB("named") futureLocalTx { s =>
        Future(
          s.list("select id from " + tableName + "")(rs =>
            Some(rs.string("id"))
          )
        )
      }
      whenReady(fResult) { _.size should equal(2) }
    }
  }

  it should "execute update in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val fCount = NamedDB("named") futureLocalTx { s =>
        Future(
          s.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        )
      }
      whenReady(fCount) {
        _ should equal(1)
      }
      val fName = fCount.flatMap { _ =>
        DB futureLocalTx (s =>
          Future(
            s.single("select name from " + tableName + " where id = ?", 1)(
              _.string("name")
            )
          )
        )
      }
      whenReady(fName) { _ should be(Some("foo")) }
    }
  }

  it should "not be able to rollback in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInFutureLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      futureUsing(DB(ConnectionPool("named").borrow())) { db =>
        val fCount = NamedDB("named") futureLocalTx { s =>
          Future(
            s.update(
              "update " + tableName + " set name = ? where id = ?",
              "foo",
              1
            )
          )
        }
        whenReady(fCount) {
          _ should equal(1)
        }
        db.rollbackIfActive()
        val fName = NamedDB("named") futureLocalTx { s =>
          Future(
            s.single("select name from " + tableName + " where id = ?", 1)(
              _.string("name")
            )
          )
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
      val failure = NamedDB("named") futureLocalTx { implicit s =>
        Future(
          s.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        )
          .map(_ => s.update("update foo should be rolled back"))
      }
      intercept[Exception] {
        Await.result(failure, 10.seconds)
      }
      val res = NamedDB("named") readOnly (s =>
        s.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      )
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // localTx with an IO monad example

  it should "execute single in localTx block with an IO monad" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val myIOResult = NamedDB("named").localTx[MyIO[Option[String]]] { s =>
        MyIO(
          s.single("select id from " + tableName + " where id = ?", 1)(
            _.string("id")
          )
        )
      }(MyIO.myIOTxBoundary)
      myIOResult.run() should equal(Some("1"))
    }
  }

  it should "execute list in localTx block with an IO monad" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val myIOResult = NamedDB("named").localTx[MyIO[List[Option[String]]]] {
        s =>
          MyIO(
            s.list("select id from " + tableName + "")(rs =>
              Some(rs.string("id"))
            )
          )
      }(boundary = MyIO.myIOTxBoundary)
      myIOResult.run().size should equal(2)
    }
  }

  it should "execute update in localTx block with an IO monad" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val myIOCount = NamedDB("named").localTx[MyIO[Int]] { s =>
        MyIO(
          s.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        )
      }(boundary = MyIO.myIOTxBoundary)
      val result1 = myIOCount.run()

      result1 should equal(1)

      val myIOName = MyIO(result1).flatMap { _ =>
        DB.localTx[MyIO[Option[String]]](s =>
          MyIO(
            s.single("select name from " + tableName + " where id = ?", 1)(
              _.string("name")
            )
          )
        )(boundary = MyIO.myIOTxBoundary)
      }
      myIOName.run() should be(Some("foo"))
    }
  }

  it should "not be able to rollback in localTx block with an IO monad" in {
    val tableName = tableNamePrefix + "_singleInIOLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      futureUsing(DB(ConnectionPool("named").borrow())) { db =>
        val myIOCount = NamedDB("named").localTx[MyIO[Int]] { s =>
          MyIO(
            s.update(
              "update " + tableName + " set name = ? where id = ?",
              "foo",
              1
            )
          )
        }
        myIOCount.run() should equal(1)

        db.rollbackIfActive()
        val myIOName = NamedDB("named").localTx[MyIO[Option[String]]] { s =>
          MyIO(
            s.single("select name from " + tableName + " where id = ?", 1)(
              _.string("name")
            )
          )
        }
        myIOName.run() should equal(Some("foo"))
        Future(myIOName.run())
      }
    }
  }

  it should "do rollback in localTx block with an IO monad" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure = NamedDB("named").localTx[MyIO[Int]] { implicit s =>
        MyIO(
          s.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        )
          .map(_ => s.update("update foo should be rolled back"))
      }(MyIO.myIOTxBoundary)
      intercept[Exception] {
        failure.run()
      }
      val res = NamedDB("named") readOnly (s =>
        s.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      )
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
        NamedDB("named") withinTx { session =>
          session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB("named")) { db =>
        db.begin()
        val result = db withinTx { session =>
          session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
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
      using(NamedDB("named")) { db =>
        db.begin()
        val session = db.withinTxSession()
        val result = session.list("select * from " + tableName + "")(rs =>
          Some(rs.string("name"))
        )
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB("named")) { db =>
        db.begin()
        val result = db withinTx {
          _.single("select id from " + tableName + " where id = ?", 1)(
            _.string("id")
          )
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
      using(NamedDB("named")) { db =>
        db.begin()
        val result = db withinTx {
          _.list("select id from " + tableName + "")(rs =>
            Some(rs.string("id"))
          )
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
      using(NamedDB("named")) { db =>
        db.begin()
        val count = db withinTx {
          _.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        }
        count should equal(1)
        val name = (db withinTx {
          _.single("select name from " + tableName + " where id = ?", 1)(
            _.string("name")
          )
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
        DB(ConnectionPool.borrow("named")) autoCommit {
          _.execute("drop table " + tableName)
        }
      }
    }) {
      NamedDB("named") autoCommit { session =>
        handling(classOf[Throwable]) by { t =>
          try {
            session.execute(
              "create table " + tableName + " (id integer primary key, name varchar(30))"
            )
          } catch {
            case e: Exception =>
              session.execute(
                "create table " + tableName + " (id integer primary key, name varchar(30))"
              )
          }
          session.update("delete from " + tableName)
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            1,
            "name1"
          )
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            2,
            "name2"
          )
        } apply {
          session.single("select count(1) from " + tableName)(_.int(1))
          session.update("delete from " + tableName)
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            1,
            "name1"
          )
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            2,
            "name2"
          )
        }
      }
      using(NamedDB("named")) { db =>
        db.begin()
        val count = db withinTx {
          _.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
        }
        count should equal(1)
        db.rollback()
        db.begin()
        val name = (db withinTx {
          _.single("select name from " + tableName + " where id = ?", 1)(
            _.string("name")
          )
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
        DB(ConnectionPool.borrow("named")) autoCommit {
          _.execute("drop table " + tableName)
        }
      }
    }) {
      NamedDB("named") autoCommit { session =>
        handling(classOf[Throwable]) by { t =>
          try {
            session.execute(
              "create table " + tableName + " (id integer primary key, name varchar(30))"
            )
          } catch {
            case e: Exception =>
              try {
                session.execute(
                  "create table " + tableName + " (id int primary key, name varchar(30))"
                )
              } catch {
                case e: Exception =>
              }
          }
          session.update("delete from " + tableName)
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            1,
            "name1"
          )
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            2,
            "name2"
          )
        } apply {
          session.single("select count(1) from " + tableName)(_.int(1))
          session.update("delete from " + tableName)
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            1,
            "name1"
          )
          session.update(
            "insert into " + tableName + " (id, name) values (?, ?)",
            2,
            "name2"
          )
        }
      }
      import scala.concurrent.ExecutionContext.Implicits.global
      scala.concurrent.Future {
        using(NamedDB("named")) { db =>
          db.begin()
          val session = db.withinTxSession()
          session.update(
            "update " + tableName + " set name = ? where id = ?",
            "foo",
            1
          )
          Thread.sleep(1000L)
          val name = session.single(
            "select name from " + tableName + " where id = ?",
            1
          )(_.string("name"))
          assert(name.get == "foo")
          db.rollback()
        }
      }
      scala.concurrent.Future {
        using(NamedDB("named")) { db =>
          db.begin()
          val session = db.withinTxSession()
          Thread.sleep(200L)
          val name = session.single(
            "select name from " + tableName + " where id = ?",
            1
          )(_.string("name"))
          assert(name.get == "name1")
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      val name = NamedDB("named") autoCommit { session =>
        session.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      }
      assert(name.get == "name1")
    }
  }

}
