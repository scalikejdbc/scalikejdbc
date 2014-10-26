package scalikejdbc

import org.scalatest._
import java.sql.SQLException
import org.slf4j.LoggerFactory

import scala.util.Try
import scala.util.control.Exception._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._
import org.scalatest.concurrent.ScalaFutures
import ExecutionContext.Implicits.global

class DBSpec extends FlatSpec with Matchers with BeforeAndAfter with Settings with LoanPattern with ScalaFutures {

  val logger = LoggerFactory.getLogger(classOf[DBSpec])

  val tableNamePrefix = "emp_DBObjectSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB"

  it should "be a trait" in {
    val tableName = tableNamePrefix + "_trait"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(DB(ConnectionPool.borrow())) { db =>
        val result = db readOnly {
          session => session.list("select * from " + tableName + "")(rs => rs.string("name"))
        }
        result.size should be > 0
      }
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

  implicit val patienceTimeout = PatienceConfig(10.seconds)

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

  it should "do rollback in futureLocalTx block" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure = DB futureLocalTx { implicit s =>
        Future(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .map(_ => s.update("update foo should be rolled back"))
      }
      intercept[Exception] {
        Await.result(failure, 10.seconds)
      }
      val res = DB readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // generalizedLocalTx

  // Try

  it should "execute single in generalizedLocalTx block for Try" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val result: Try[Option[String]] = DB generalizedLocalTx { s =>
        allCatch.withTry { s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")) }
      }
      result.isSuccess should equal(true)
      result.get should equal(Some("1"))
    }
  }

  it should "execute list in generalizedLocalTx block for Try" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val result: Try[List[Some[String]]] = DB generalizedLocalTx { s =>
        allCatch.withTry(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      result.isSuccess should equal(true)
      result.get.size should equal(2)
    }
  }

  it should "execute update in generalizedLocalTx block for Try" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val count: Try[Int] = DB generalizedLocalTx { s =>
        allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      count.get should equal(1)

      val name: Try[Option[String]] = count.flatMap { _ =>
        DB generalizedLocalTx (s => allCatch.withTry(
          s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        ))
      }
      name.get should be(Some("foo"))
    }
  }

  it should "not be able to rollback in generalizedLocalTx block for Try" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>

        val count: Try[Int] = DB generalizedLocalTx { s =>
          allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        count.get should equal(1)

        db.rollbackIfActive()
        val name: Try[Option[String]] = DB generalizedLocalTx { s =>
          allCatch.withTry(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        name.get should equal(Some("foo"))
      }
    }
  }

  it should "roll back in generalizedLocalTx block for Try" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val failure: Try[Int] = DB.generalizedLocalTx[Try[Int]] { implicit s =>
        allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .flatMap(_ => allCatch.withTry(s.update("update foo should be rolled back")))
      }
      failure.isFailure should be(true)
      val res = DB readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // Either

  it should "execute single in generalizedLocalTx block for Either" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result: Either[Throwable, Option[String]] = DB generalizedLocalTx { s =>
        allCatch.either { s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")) }
      }
      result should equal(Right(Some("1")))
    }
  }

  it should "execute list in generalizedLocalTx block for Either" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result: Either[Throwable, List[Some[String]]] = DB generalizedLocalTx { s =>
        allCatch.either(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      result.right.get.size should equal(2)
    }
  }

  it should "execute update in generalizedLocalTx block for Either" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count: Either[Throwable, Int] = DB generalizedLocalTx { s =>
        allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      count should equal(Right(1))
      val name: Either[Throwable, Option[String]] = count.right.flatMap { _ =>
        DB generalizedLocalTx (s => allCatch.either(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))))
      }
      name should be(Right(Some("foo")))
    }
  }

  it should "not be able to rollback in generalizedLocalTx block for Either" in {
    val tableName = tableNamePrefix + "_singleInGeneralizedLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        val count: Either[Throwable, Int] = DB generalizedLocalTx { s =>
          allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        count should equal(Right(1))
        db.rollbackIfActive()
        val name: Either[Throwable, Option[String]] = DB generalizedLocalTx { s =>
          allCatch.either(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        name should equal(Right(Some("foo")))
      }
    }
  }

  it should "do rollback in generalizedLocalTx block for Either" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure: Either[Throwable, Int] = DB.generalizedLocalTx[Either[Throwable, Int]] { implicit s =>
        allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .right.flatMap(_ => allCatch.either(s.update("update foo should be rolled back")))
      }
      failure should be('left)
      val res = DB readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // tryLocalTx

  it should "execute single in tryLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInTryLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val result: Try[Option[String]] = DB tryLocalTx { s =>
        allCatch.withTry { s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")) }
      }
      result.isSuccess should equal(true)
      result.get should equal(Some("1"))
    }
  }

  it should "execute list in tryLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInTryLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val result: Try[List[Some[String]]] = DB tryLocalTx { s =>
        allCatch.withTry(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      result.isSuccess should equal(true)
      result.get.size should equal(2)
    }
  }

  it should "execute update in tryLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInTryLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val count: Try[Int] = DB tryLocalTx { s =>
        allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      count.get should equal(1)

      val name: Try[Option[String]] = count.flatMap { _ =>
        DB tryLocalTx (s => allCatch.withTry(
          s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        ))
      }
      name.get should be(Some("foo"))
    }
  }

  it should "not be able to rollback in tryLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInTryLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>

        val count: Try[Int] = DB tryLocalTx { s =>
          allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        count.get should equal(1)

        db.rollbackIfActive()
        val name: Try[Option[String]] = DB tryLocalTx { s =>
          allCatch.withTry(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        name.get should equal(Some("foo"))
      }
    }
  }

  it should "roll back in tryLocalTx block" in {
    val tableName = tableNamePrefix + "_rollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      val failure: Try[Int] = DB.tryLocalTx { implicit s =>
        allCatch.withTry(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .flatMap(_ => allCatch.withTry(s.update("update foo should be rolled back")))
      }
      failure.isFailure should be(true)
      val res = DB readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
    }
  }

  // --------------------
  // eitherLocalTx

  it should "execute single in eitherLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInEitherLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result: Either[Throwable, Option[String]] = DB eitherLocalTx { s =>
        allCatch.either { s.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id")) }
      }
      result should equal(Right(Some("1")))
    }
  }

  it should "execute list in eitherLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInEitherLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result: Either[Throwable, List[Some[String]]] = DB eitherLocalTx { s =>
        allCatch.either(s.list("select id from " + tableName + "")(rs => Some(rs.string("id"))))
      }
      result.right.get.size should equal(2)
    }
  }

  it should "execute update in eitherLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInEitherLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count: Either[Throwable, Int] = DB eitherLocalTx { s =>
        allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
      }
      count should equal(Right(1))
      val name: Either[Throwable, Option[String]] = count.right.flatMap { _ =>
        DB eitherLocalTx (s => allCatch.either(
          s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        ))
      }
      name should be(Right(Some("foo")))
    }
  }

  it should "not be able to rollback in eitherLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInEitherLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        val count: Either[Throwable, Int] = DB eitherLocalTx { s =>
          allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
        }
        count should equal(Right(1))
        db.rollbackIfActive()
        val name: Either[Throwable, Option[String]] = DB eitherLocalTx { s =>
          allCatch.either(s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
        }
        name should equal(Right(Some("foo")))
      }
    }
  }

  it should "do rollback in eitherLocalTx block" in {
    val tableName = tableNamePrefix + "_singleInEitherLocalTxRollback"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val failure: Either[Throwable, Int] = DB eitherLocalTx { implicit s =>
        allCatch.either(s.update("update " + tableName + " set name = ? where id = ?", "foo", 1))
          .right.flatMap(_ => allCatch.either(s.update("update foo should be rolled back")))
      }
      failure should be('left)
      val res = DB readOnly (s => s.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name")))
      res.get should not be (Some("foo"))
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
      import scala.concurrent.ExecutionContext.Implicits.global
      scala.concurrent.Future {
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
      scala.concurrent.Future {
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

      using(ConnectionPool.borrow()) { conn =>
        val name = DB(conn) autoCommit {
          session =>
            session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }
        assert(name.get == "name1")
      }
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
      val tableName = s"issue245_${System.currentTimeMillis}"
      try {
        DB autoCommit { implicit s =>
          SQL(s"create table `${tableName}` (some_setting tinyint(1) NOT NULL DEFAULT '1')").execute.apply()
        }
        val table = DB.getTable(tableName)
        table.isDefined should equal(true)
      } finally {
        try DB autoCommit { implicit s => SQL(s"drop table ${tableName}").execute.apply() }
        catch { case e: Exception => }
      }
    }
  }

  // --------------------
  // https://groups.google.com/forum/#!topic/scalikejdbc-users-group/4qIgqXQ-TOY

  it should "be able to disable auto-close mode" in {
    val tableName = tableNamePrefix + "_disableAutoClose"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      // default behavior
      using(DB(ConnectionPool.borrow())) { db =>
        //db.autoClose(false)
        db.localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        // java.sql.SQLException: Connection is closed.
        intercept[java.sql.SQLException] {
          db.readOnly {
            _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          }
        }
      }

      // disable auto-close mode
      using(DB(ConnectionPool.borrow())) { db =>
        db.autoClose(false)
        db.localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        val name1 = db.readOnly {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }.get
        name1 should equal("foo")

        db.localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "bar", 1)
        }
        val name2 = db.readOnly {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }.get
        name2 should equal("bar")
      }
    }
  }

  // fetchSize

  it should "execute query with fetchSize" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      {
        val result: Seq[String] = DB readOnly { session =>
          session.fetchSize(111)
          session.list("select * from " + tableName + "")(rs => rs.string("name"))
        }
        result.size should be > 0
      }

      {
        val result: Seq[String] = DB readOnly { implicit session =>
          SQL("select * from " + tableName + "").fetchSize(222).map(rs => rs.string("name")).list.apply()
        }
        result.size should be > 0
      }
    }
  }

  // TimeZone

  /*
  it should "work with TimeZone" in {
    if (driverClassName != "com.mysql.jdbc.Driver") {

      val now = new Date
      val currentTimeZone = TimeZone.getDefault

      val tableName = tableNamePrefix + "_timezone"
      try {
        ultimately(TestUtils.deleteTable(tableName)) {
          DB autoCommit { implicit s =>
            SQL(s"create table ${tableName} (id bigint, t timestamp without time zone not null, tz timestamp not null)").execute.apply()
            SQL(s"insert into ${tableName} (id, t, tz) values (?, ?, ?)").bind(1, now, now).update.apply()
          }

          case class Record(id: Long, t: DateTime, tz: DateTime)

          val record1: Record = DB readOnly { implicit s =>
            SQL(s"select * from ${tableName}").map { rs =>
              Record(rs.get("id"), rs.jodaDateTime("t"), rs.jodaDateTime("tz"))
            }.list.apply().head
          }
          logger.info(s"record: ${record1}")

          record1.id should equal(1)
          record1.t.toDate should equal(new DateTime(now).toLocalDateTime.toDate)
          record1.tz.toDate should equal(now)
          record1.t.getMillis should equal(now.getTime)

          // side effect for other tests
          val utc = TimeZone.getTimeZone("UTC")
          TimeZone.setDefault(utc)
          DateTimeZone.setDefault(DateTimeZone.forTimeZone(utc))

          val record2: Record = DB readOnly { implicit s =>
            SQL(s"select * from ${tableName}").map { rs =>
              Record(rs.get("id"), rs.jodaDateTime("t"), rs.jodaDateTime("tz"))
            }.list.apply().head
          }
          logger.info(s"record: ${record2}")

          record2.id should equal(1)
          if (currentTimeZone.getID == utc.getID) {
            record2.t.toDate should not equal (new DateTime(now).toLocalDateTime.toDate)
          } else {
            record2.t.toDate should equal(new DateTime(now).toLocalDateTime.toDate)
          }
          record2.tz.toDate should equal(now)
          record2.t.getMillis should equal(now.getTime)
        }

      } finally {
        TimeZone.setDefault(currentTimeZone)
      }
    }
  }
  */

}
