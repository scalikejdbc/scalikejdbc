package scalikejdbc

import org.scalatest._
import java.sql.SQLException
import util.control.Exception._
import scalikejdbc.LoanPattern._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DB_SessionOperationSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings {

  val tableNamePrefix =
    "emp_DB_SesOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(Session operation)"

  it should "be available" in {
    using(ConnectionPool.borrow()) { conn =>
      val db = DB(conn)
      db should not be null
      db.close()
    }
  }

  // --------------------
  // tx

  it should "be impossible to call #begin twice" in {
    using(ConnectionPool.borrow()) { conn =>
      val db = DB(conn)
      db.begin()
      intercept[IllegalStateException] {
        db.begin()
      }
      db.rollback()
    }
  }

  it should "be possible to call #beginIfNotYet several times" in {
    using(ConnectionPool("default").borrow()) { conn =>
      val db = DB(conn)
      db.begin()
      db.beginIfNotYet()
      db.beginIfNotYet()
      db.beginIfNotYet()
      db.rollback()
      db.rollbackIfActive()
    }
  }

  "#tx" should "not be available before beginning tx" in {
    using(ConnectionPool("named").borrow()) { conn =>
      val db = DB(conn)
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val session = db.readOnlySession()
        try {
          val result = session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
          result.size should be > 0
        } finally { session.close() }
      }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_neverUpdateInReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        intercept[SQLException] {
          db readOnly {
            _.update("update " + tableName + " set name = ?", "xxx")
          }
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val result = db autoCommit { session =>
          session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
        }
        result.size should be > 0
      }
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val session = db.autoCommitSession()
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
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val result = db autoCommit {
          _.single("select id from " + tableName + " where id = ?", 1)(
            _.int("id")
          )
        }
        result.get should equal(1)
      }
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommit"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        intercept[TooManyRowsException] {
          db autoCommit {
            _.single("select id from " + tableName + "")(rs =>
              Some(rs.int("id"))
            )
          }
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val extractName = (rs: WrappedResultSet) => rs.string("name")
        val name: Option[String] = db readOnly {
          _.single("select * from " + tableName + " where id = ?", 1)(
            extractName
          )
        }
        name.get should equal("name1")
      }
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        val result = db autoCommit {
          _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
        }
        result.size should equal(2)
      }
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        db autoCommit {
          _.foreach("select id from " + tableName + "")(rs =>
            println(rs.int("id"))
          )
        }
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
        _.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (DB readOnly {
        _.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
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
      val count = DB localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (DB localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(
          _.string("name")
        )
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        intercept[IllegalStateException] {
          db withinTx { session =>
            session.list("select * from " + tableName + "")(rs =>
              Some(rs.string("name"))
            )
          }
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
        db.begin()
        val session = db.withinTxSession()
        try {
          val result = session.list("select * from " + tableName + "")(rs =>
            Some(rs.string("name"))
          )
          result.size should be > 0
          db.rollbackIfActive()
        } finally { session.close() }
      }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val db = DB(conn)
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
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      import scala.concurrent.ExecutionContext.Implicits.global
      scala.concurrent.Future {
        using(ConnectionPool.borrow()) { conn =>
          val db = DB(conn)
          db.begin()
          val session = db.withinTxSession()
          try {
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
          } finally { session.close() }
        }
      }
      scala.concurrent.Future {
        using(ConnectionPool.borrow()) { conn =>
          val db = DB(conn)
          db.begin()
          val session = db.withinTxSession()
          try {
            Thread.sleep(200L)
            val name = session.single(
              "select name from " + tableName + " where id = ?",
              1
            )(_.string("name"))
            assert(name.get == "name1")
            db.rollback()
          } finally { session.close() }
        }
      }

      Thread.sleep(2000L)

      using(ConnectionPool.borrow()) { conn =>
        val name = DB(conn) autoCommit { session =>
          session.single("select name from " + tableName + " where id = ?", 1)(
            _.string("name")
          )
        }
        assert(name.get == "name1")
      }
    }
  }

}
