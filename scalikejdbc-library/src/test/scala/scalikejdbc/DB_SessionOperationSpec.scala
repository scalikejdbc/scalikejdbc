package scalikejdbc

import org.scalatest._
import org.scalatest.BeforeAndAfter
import scala.concurrent.Future
import java.sql.SQLException
import util.control.Exception._
import scala.concurrent.ExecutionContext.Implicits.global

class DB_SessionOperationSpec extends FlatSpec with Matchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DB_SesOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(Session operation)"

  it should "be available" in {
    val db = DB(ConnectionPool.borrow())
    db should not be null
    db.close()
  }

  // --------------------
  // tx

  it should "be impossible to call #begin twice" in {
    val db = DB(ConnectionPool.borrow())
    db.begin()
    intercept[IllegalStateException] {
      db.begin()
    }
    try {
      db.rollback()
    } finally {
      db.close()
    }
  }

  it should "be possible to call #beginIfNotYet several times" in {
    val db = DB(ConnectionPool('default).borrow())
    try {
      db.begin()
      db.beginIfNotYet()
      db.beginIfNotYet()
      db.beginIfNotYet()
      db.rollback()
      db.rollbackIfActive()
    } finally {
      db.close()
    }
  }

  "#tx" should "not be available before beginning tx" in {
    val db = DB(ConnectionPool('named).borrow())
    intercept[IllegalStateException] {
      db.tx.begin()
    }
    db.rollbackIfActive()
    db.close()
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db readOnly {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnlyWithConnection block" in {
    val tableName = tableNamePrefix + "_queryReadOnlyConn"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db readOnlyWithConnection {
        implicit conn =>
          import anorm._
          SQL("select * from " + tableName)().toList
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val session = db.readOnlySession()
      try {
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_neverUpdateInReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      intercept[SQLException] {
        db readOnly {
          session => session.update("update " + tableName + " set name = ?", "xxx")
        }
      }
    }
  }

  // --------------------
  // autoCommit

  it should "execute query in autoCommit block" in {
    val tableName = tableNamePrefix + "_queryInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db autoCommit {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommitWithConnection block" in {
    val tableName = tableNamePrefix + "_queryAutoCommitConn"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db autoCommitWithConnection {
        implicit conn =>
          import anorm._
          SQL("select * from " + tableName)().toList
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val session = db.autoCommitSession()
      try {
        val list = session.list("select id from " + tableName + " order by id")(rs => rs.int("id"))
        list(0) should equal(1)
        list(1) should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.int("id"))
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      intercept[TooManyRowsException] {
        db autoCommit {
          _.single("select id from " + tableName + "")(rs => Some(rs.int("id")))
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = db readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should equal("name1")
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      db autoCommit {
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

  it should "execute update in autoCommitWithConnection block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB autoCommitWithConnection {
        implicit conn =>
          import anorm._
          SQL("update " + tableName + " set name = {name} where id = {id}").on('name -> "foo", 'id -> 1).executeUpdate()
      }
      count should equal(1)
      val name = (DB autoCommit {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateAfterReadOnly"
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

  it should "execute update in localTxWithConnection block" in {
    val tableName = tableNamePrefix + "_updateLocalTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB localTxWithConnection {
        implicit conn =>
          import anorm._
          SQL("update " + tableName + " set name = {name} where id = {id}").on('name -> "foo", 'id -> 1).executeUpdate()
      }
      count should equal(1)
      val name = (DB localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
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

  // --------------------
  // withinTx

  it should "not execute query in withinTx block before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      intercept[IllegalStateException] {
        db withinTx {
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
      val db = DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  it should "execute query in withinTxWithConnection block" in {
    val tableName = tableNamePrefix + "_queryWithinConnBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTxWithConnection {
        implicit conn =>
          import anorm._
          SQL("select * from " + tableName)().toList
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  it should "execute query in withinTx session" in {
    val tableName = tableNamePrefix + "_queryInWithinTxSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      try {
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
        db.rollbackIfActive()
      } finally { session.close() }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute list in withinTx block" in {
    val tableName = tableNamePrefix + "_listInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in withinTx block" in {
    val tableName = tableNamePrefix + "_updateInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
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

  it should "rollback in withinTx block" in {
    val tableName = tableNamePrefix + "_rollbackInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
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

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      Future {
        val db = DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        try {
          session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          Thread.sleep(1000L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "foo")
          db.rollback()
        } finally { session.close() }
      }
      Future {
        val db = DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        try {
          Thread.sleep(200L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "name1")
          db.rollback()
        } finally { session.close() }
      }

      Thread.sleep(2000L)

      val name = DB(ConnectionPool.borrow()) autoCommit {
        session =>
          session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }
      assert(name.get == "name1")
    }
  }

}
