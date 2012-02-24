package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class DBSessionSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DBSessionSpec" + System.currentTimeMillis()

  behavior of "DBSession"

  it should "available" in {
    val conn = ConnectionPool.borrow()
    val session = new DBSession(conn)
    session should not be null
  }

  it should "exceute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues";
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()

      session.execute("insert into " + tableName + " values (?, ?)", 3, Option("Ben"))
      val benOpt = session.asOne("select id,name from " + tableName + " where id = ?", 3)(rs => (rs.int("id"), rs.string("name")))
      benOpt.get._1 should equal(3)
      benOpt.get._2 should equal("Ben")

      session.execute("insert into " + tableName + " values (?, ?)", 4, Option(null))
      val maybeNone = session.asOne("select id,name from " + tableName + " where id = ?", 4)(rs => (rs.int("id"), rs.string("name")))
      maybeNone.get._1 should equal(4)
      maybeNone.get._2 should equal(null)
    }
  }

  // --------------------
  // auto commit

  it should "execute asOne in auto commit mode" in {
    val tableName = tableNamePrefix + "_asOneInAutoCommit";
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val result = session.asOne("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      result.get should equal("1")
    }
  }

  it should "execute asList in auto commit mode" in {
    val tableName = tableNamePrefix + "_asListInAutoCommit";
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      val session = db.autoCommitSession()
      val result = session.asList("select id from " + tableName) {
        rs => rs.string("id")
      }
      result.size should equal(2)
    }
  }

  it should "execute update in auto commit mode" in {
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
        rs => rs.string("name")
      } getOrElse "---"
      name should equal("foo")
    }

  }

  // --------------------
  // within tx mode

  it should "execute asOne in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asOneInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.asOne("select id from " + tableName + " where id = ?", 1) {
        rs => rs.string("id")
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute asList in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_asListInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = session.asList("select id from " + tableName + "") {
        rs => rs.string("id")
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInWithinTx";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val nameBefore = session.asOne("select name from " + tableName + " where id = ?", 1) {
        rs => rs.string("name")
      }.get
      nameBefore should equal("name1")
      val count = session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      count should equal(1)
      db.rollbackIfActive()
      val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
        rs => rs.string("name")
      }.get
      name should equal("name1")
    }
  }

}
