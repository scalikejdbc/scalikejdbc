package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import scala.concurrent.ops._
import java.sql.{ SQLException, DriverManager }
import util.control.Exception._
import scalikejdbc.LoanPattern._

@RunWith(classOf[JUnitRunner])
class DBSuite extends FunSuite with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DBSuite"

  test("available") {
    val conn = ConnectionPool.borrow()
    val db = new DB(conn)
    db should not be null
  }

  // --------------------
  // tx

  test("cannot call DB#begin twice") {
    val conn = ConnectionPool.borrow()
    val db = new DB(conn)
    db.begin()
    intercept[IllegalStateException] {
      db.begin()
    }
    db.rollback()
  }

  test("can call DB#beginIfNotYet several times") {
    val conn = ConnectionPool("default").borrow()
    val db = new DB(conn)
    db.begin()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.rollback()
    db.rollbackIfActive()
  }

  test("before beginning tx, DB#tx is not available") {
    val conn = ConnectionPool('named).borrow()
    val db = new DB(conn)
    intercept[IllegalStateException] {
      db.tx.begin()
    }
    db.rollbackIfActive()
  }

  // --------------------
  // readOnly

  test("query in readOnly block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db readOnly {
        session =>
          session.asList("select * from " + tableName + "") {
            rs => Some(rs.getString("name"))
          }
      }
      result.size should be > 0
    }
  }

  test("query in readOnlySession") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInReadOnlySession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val session = db.readOnlySession()
      val result = session.asList("select * from " + tableName + "") {
        rs => Some(rs.getString("name"))
      }
      result.size should be > 0
    }
  }

  test("cannot update in readOnly block") {
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

  test("query in autoCommit block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        session =>
          session.asList("select * from " + tableName + "") {
            rs => Some(rs.getString("name"))
          }
      }
      result.size should be > 0
    }
  }

  test("query in autoCommitSession") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInAutoCommitSession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val session = db.autoCommitSession()
      val list = session.asList("select id from " + tableName + " order by id")(rs => Some(rs.getInt("id")))
      list(0) should equal(1)
      list(1) should equal(2)
    }
  }

  test("asOne in autoCommit block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.asOne("select id from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getInt("id"))
        }
      }
      result.get should equal(1)
    }
  }

  test("asOne returns too many results in autoCommit block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      intercept[TooManyRowsException] {
        db autoCommit {
          _.asOne("select id from " + tableName + "") {
            rs => Some(rs.getInt("id"))
          }
        }
      }
    }
  }

  test("asOne in autoCommit block 2") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInAutoCommitBlock2";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val extractName = (rs: java.sql.ResultSet) => Some(rs.getString("name"))
      val name: Option[String] = db readOnly {
        _.asOne("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should be === "name1"
    }
  }

  test("asList in autoCommit block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asListInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db autoCommit {
        _.asList("select id from " + tableName + "") {
          rs => Some(rs.getInt("id"))
        }
      }
      result.size should equal(2)
    }
  }

  test("foreach in autoCommit block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db autoCommit {
        _.foreach("select id from " + tableName + "") {
          rs => println(rs.getInt("id"))
        }
      }
    }
  }

  test("update in autoCommit block") {
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
        _.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }).get
      name should equal("foo")
    }
  }

  // --------------------
  // localTx

  test("asOne in localTx block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db localTx {
        _.asOne("select id from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("id"))
        }
      }
      result.get should equal("1")
    }
  }

  test("asList in localTx block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asListInLocalTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      val result = db localTx {
        _.asList("select id from " + tableName + "") {
          rs => Some(rs.getString("id"))
        }
      }
      result.size should equal(2)
    }
  }

  test("update in localTx block") {
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
        _.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  test("rollback in localTx block") {
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
        _.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  // --------------------
  // withinTx

  test("query in withinTx block before beginning tx") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      intercept[IllegalStateException] {
        db withinTx {
          session =>
            session.asList("select * from " + tableName + "") {
              rs => Some(rs.getString("name"))
            }
        }
      }
    }
  }

  test("query in withinTx block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        session =>
          session.asList("select * from " + tableName + "") {
            rs => Some(rs.getString("name"))
          }
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  test("query in withinTxSession") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_queryInWithinTxSession";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      val result = session.asList("select * from " + tableName + "") {
        rs => Some(rs.getString("name"))
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  test("asOne in withinTx block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.asOne("select id from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("id"))
        }
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  test("asList in withinTx block") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asListInWithinTxBlock";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        _.asList("select id from " + tableName + "") {
          rs => Some(rs.getString("id"))
        }
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  test("update in withinTx block") {
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
        _.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }).get
      name should equal("foo")
      db.rollback()
    }
  }

  test("rollback in withinTx block") {
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
              _.asOne("select name from " + tableName + " where id = ?", 1) {
                rs => Some(rs.getString("name"))
              }
            }).get
            name should equal("name1")
          }
      }
    }
  }

  // --------------------
  // multi threads

  test("testing with multi threads") {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(ConnectionPool.borrow(), tableName)) {
      TestUtils.initialize(ConnectionPool.borrow(), tableName)
      spawn {
        val db = new DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        Thread.sleep(1000L)
        val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
        assert(name.get == "foo")
        db.rollback()
      }
      spawn {
        val db = new DB(ConnectionPool.borrow())
        db.begin()
        val session = db.withinTxSession()
        Thread.sleep(200L)
        val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
        assert(name.get == "name1")
        db.rollback()
      }

      Thread.sleep(2000L)

      val name = new DB(ConnectionPool.borrow()) autoCommit {
        session =>
          {
            session.asOne("select name from " + tableName + " where id = ?", 1) {
              rs => Some(rs.getString("name"))
            }
          }
      }
      assert(name.get == "name1")
    }
  }

}
