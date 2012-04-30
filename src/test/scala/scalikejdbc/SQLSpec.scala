package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import java.sql.PreparedStatement

@RunWith(classOf[JUnitRunner])
class SQLSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_SQLSpec" + System.currentTimeMillis()

  behavior of "SQL"

  it should "available" in {
    val conn = ConnectionPool.borrow()
    val session = new DBSession(conn)
    session should not be null
  }

  it should "execute insert with nullable values" in {
    val tableName = tableNamePrefix + "_insertWithNullableValues"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      implicit val session = db.autoCommitSession()

      SQL("insert into " + tableName + " values (?, ?)").bind(3, Option("Ben")).execute().apply()

      val benOpt = SQL("select id,name from " + tableName + " where id = ?").bind(3)
        .map(rs => (rs.int("id"), rs.string("name"))).toOption()
        .apply()

      benOpt.get._1 should equal(3)
      benOpt.get._2 should equal("Ben")

      SQL("insert into " + tableName + " values (?, ?)").bind(4, Option(null)).execute().apply()

      val noName = SQL("select id,name from " + tableName + " where id = ?").bind(4)
        .map(rs => (rs.int("id"), rs.string("name"))).toOption
        .apply()

      noName.get._1 should equal(4)
      noName.get._2 should equal(null)

      val before = (s: PreparedStatement) => println("before")
      val after = (s: PreparedStatement) => println("before")
      SQL("insert into " + tableName + " values (?, ?)").bind(5, Option(null)).executeWithFilters(before, after).apply()

    }
  }

  // --------------------
  // auto commit

  it should "execute single in auto commit mode" in {
    val tableName = tableNamePrefix + "_singleInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      implicit val session = db.autoCommitSession()

      val singleResult = SQL("select id from " + tableName + " where id = ?").bind(1)
        .map(rs => rs.string("id")).toOption().apply()
      singleResult.get should equal("1")

      val firstResult = SQL("select id from " + tableName).map(rs => rs.string("id")).headOption().apply()
      firstResult.get should equal("1")
    }
  }

  it should "execute list in auto commit mode" in {
    val tableName = tableNamePrefix + "_listInAutoCommit"
    val conn = ConnectionPool.borrow()
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(conn)
      implicit val session = db.autoCommitSession()
      val result = SQL("select id from " + tableName).map(rs => rs.string("id")).toList().apply()
      result.size should equal(2)
    }
  }

  it should "execute executeUpdate in auto commit mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    val db = new DB(conn)
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      implicit val session = new DB(ConnectionPool.borrow()).autoCommitSession()
      val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate.apply()

      val before = (s: PreparedStatement) => println("before")
      val after = (s: PreparedStatement) => println("before")
      SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdateWithFilters(before, after).apply()

      db.rollbackIfActive()
      count should equal(1)
      val name = SQL("select name from " + tableName + " where id = ?").bind(1)
        .map(rs => rs.string("name")).toOption().apply().getOrElse("---")
      name should equal("foo")
    }

  }

  it should "execute update in auto commit mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInAutoCommit"
    val db = new DB(conn)
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      implicit val session = new DB(ConnectionPool.borrow()).autoCommitSession()
      val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).update.apply()

      val before = (s: PreparedStatement) => println("before")
      val after = (s: PreparedStatement) => println("before")
      SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).updateWithFilters(before, after).apply()

      db.rollbackIfActive()
      count should equal(1)
      val name = SQL("select name from " + tableName + " where id = ?").bind(1)
        .map(rs => rs.string("name")).toOption().apply().getOrElse("---")
      name should equal("foo")
    }

  }

  // --------------------
  // within tx mode

  it should "execute single in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_singleInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      implicit val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = SQL("select id from " + tableName + " where id = ?").bind(1).map(rs => rs.string("id")).toOption().apply()
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute list in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_listInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      implicit val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val result = SQL("select id from " + tableName + "").map {
        rs => rs.string("id")
      }.toList().apply()
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in within tx mode" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_updateInWithinTx"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val db = new DB(ConnectionPool.borrow())
      db.begin()
      implicit val session = db.withinTxSession()
      TestUtils.initializeEmpRecords(session, tableName)
      val nameBefore = SQL("select name from " + tableName + " where id = ?").bind(1).map {
        rs => rs.string("name")
      }.toOption().apply()
      nameBefore.get should equal("name1")
      val count = SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      count should equal(1)
      db.rollbackIfActive()
      val name = SQL("select name from " + tableName + " where id = ?").bind(1).map {
        rs => rs.string("name")
      }.toOption().apply()
      name.get should equal("name1")
    }
  }

}
