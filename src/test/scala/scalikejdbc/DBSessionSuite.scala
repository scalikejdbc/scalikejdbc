package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql._
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class DBSessionSuite extends FunSuite with ShouldMatchers with BeforeAndAfter with Settings {

  type ? = this.type // for IntelliJ IDEA

  val tableNamePrefix = "emp_DBSessionSuite"

  test("available") {
    val conn = ConnectionPool.borrow()
    val session = new DBSession(conn)
    session should not be null
  }

  // --------------------
  // auto commit

  test("asOne in auto commit mode") {
    val tableName = tableNamePrefix + "_asOneInAutoCommit";
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val result = session.asOne("select id from " + tableName + " where id = ?", 1)(rs => Some(rs.getString("id")))
      result.get should equal("1")
    }
  }

  test("asList in auto commit mode") {
    val tableName = tableNamePrefix + "_asListInAutoCommit";
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val result = session.asList("select id from " + tableName) {
        rs => Some(rs.getString("id"))
      }
      result.size should equal(2)
    }
  }

  test("update in auto commit mode") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommit";
    val db = new DB(conn)
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val session = new DB(ConnectionPool.borrow()).autoCommitSession()
      val count = session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      db.rollbackIfActive()
      count should equal(1)
      val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
        rs => Some(rs.getString("name"))
      } getOrElse "---"
      name should equal("foo")
    }

  }

  // --------------------
  // within tx mode

  test("asOne in within tx mode") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.asOne("select id from " + tableName + " where id = ?", 1) {
        rs => Some(rs.getString("id"))
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  test("asList in within tx mode") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asListInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.asList("select id from " + tableName + "") {
        rs => Some(rs.getString("id"))
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  test("update in within tx mode") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val nameBefore = session.asOne("select name from " + tableName + " where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }.get
      nameBefore should equal("name1")
      val count = session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      count should equal(1)
      db.rollbackIfActive()
      val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }.get
      name should equal("name1")
    }
  }

}
