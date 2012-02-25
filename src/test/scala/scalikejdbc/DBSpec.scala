package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import scala.concurrent.ops._
import java.sql.SQLException
import util.control.Exception._
import scalikejdbc.LoanPattern._

@RunWith(classOf[JUnitRunner])
class DBSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DBSpec" + System.currentTimeMillis()

  behavior of "DB"

  it should "be available" in {
    val conn = ConnectionPool.borrow()
    val db = new DB(conn)
    db should not be null
  }

  // --------------------
  // tx

  it should "be impossible to call #begin twice" in {
    val conn = ConnectionPool.borrow()
    val db = new DB(conn)
    db.begin()
    intercept[IllegalStateException] {
      db.begin()
    }
    db.rollback()
  }

  it should "be possible to call #beginIfNotYet several times" in {
    val conn = ConnectionPool("default").borrow()
    val db = new DB(conn)
    db.begin()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.rollback()
    db.rollbackIfActive()
  }

  "#tx" should "not be available before beginning tx" in {
    val conn = ConnectionPool('named).borrow()
    val db = new DB(conn)
    intercept[IllegalStateException] {
      db.tx.begin()
    }
    db.rollbackIfActive()
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db readOnly {
        session =>
          session.list("select * from " + tableName + "") {
            rs => Some(rs.string("name"))
          }
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnly session" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInReadOnlySession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val session = db.readOnlySession()
      val result = session.list("select * from " + tableName + "") {
        rs => Some(rs.string("name"))
      }
      result.size should be > 0
    }
  }

  it should "execute update in readOnly block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
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
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        session =>
          session.list("select * from " + tableName + "") {
            rs => Some(rs.string("name"))
          }
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInAutoCommitSession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val session = db.autoCommitSession()
      val list = session.list("select id from " + tableName + " order by id")(rs => rs.int("id"))
      list(0) should equal(1)
      list(1) should equal(2)
    }
  }

  it should "execute single in autoCommit block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.single("select id from " + tableName + " where id = ?", 1) {
          rs => rs.int("id")
        }
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      intercept[TooManyRowsException] {
        db autoCommit {
          _.single("select id from " + tableName + "") {
            rs => Some(rs.int("id"))
          }
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = db readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should be === "name1"
    }
  }

  it should "execute list in autoCommit block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_listInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.list("select id from " + tableName + "") {
          rs => Some(rs.int("id"))
        }
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db autoCommit {
        _.foreach("select id from " + tableName + "") {
          rs => println(rs.int("id"))
        }
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val count = db autoCommit {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
      val name = (db autoCommit {
        _.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommitBlockAfterReadOnly";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val name = (db readOnly { _.single("select name from " + tableName + " where id = ?", 1)(_.string("name")) }).get
      name should equal("name1")
      val count = db autoCommit {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should equal(1)
    }
  }

  // --------------------
  // localTx

  it should "execute single in localTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db localTx {
        _.single("select id from " + tableName + " where id = ?", 1) {
          rs => rs.string("id")
        }
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_listInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db localTx {
        _.list("select id from " + tableName + "") {
          rs => Some(rs.string("id"))
        }
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val count = db localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should be === 1
      val name = (db localTx {
        _.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val count = db localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should be === 1
      db.rollbackIfActive()
      val name = (db localTx {
        _.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block  before beginning tx" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      intercept[IllegalStateException] {
        db withinTx {
          session =>
            session.list("select * from " + tableName + "") {
              rs => Some(rs.string("name"))
            }
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        session =>
          session.list("select * from " + tableName + "") {
            rs => Some(rs.string("name"))
          }
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  it should "execute query in withinTx session" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxSession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      val result = session.list("select * from " + tableName + "") {
        rs => Some(rs.string("name"))
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  it should "execute single in withinTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.single("select id from " + tableName + " where id = ?", 1) {
          rs => rs.string("id")
        }
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute list in withinTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_listInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.list("select id from " + tableName + "") {
          rs => Some(rs.string("id"))
        }
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in withinTx block" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val count = db withinTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should be === 1
      val name = (db withinTx {
        _.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
      }).get
      name should equal("foo")
      db.rollback()
    }
  }

  it should "rollback in withinTx block" in {
    val tableName = tableNamePrefix + "_rollbackInWithinTxBlock";
    ultimately(TestUtils.deleteTable(ConnectionPool.borrow(), tableName)) {
      TestUtils.initialize(ConnectionPool.borrow(), tableName)
      using(new DB(ConnectionPool.borrow())) {
        db =>
          {
            db.begin()
            val count = db withinTx {
              _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
            }
            count should be === 1
            db.rollback()
            db.begin()
            val name = (db withinTx {
              _.single("select name from " + tableName + " where id = ?", 1) {
                rs => rs.string("name")
              }
            }).get
            name should equal("name1")
          }
      }
    }
  }

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(ConnectionPool.borrow(), tableName)) {
      TestUtils.initialize(ConnectionPool.borrow(), tableName)
      spawn {
        val db = new DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        Thread.sleep(1000L)
        val name = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
        assert(name.get == "foo")
        db.rollback()
      }
      spawn {
        val db = new DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        Thread.sleep(200L)
        val name = session.single("select name from " + tableName + " where id = ?", 1) {
          rs => rs.string("name")
        }
        assert(name.get == "name1")
        db.rollback()
      }

      Thread.sleep(2000L)

      val name = new DB(ConnectionPool.borrow()) autoCommit {
        session =>
          {
            session.single("select name from " + tableName + " where id = ?", 1) {
              rs => rs.string("name")
            }
          }
      }
      assert(name.get == "name1")
    }
  }

}
