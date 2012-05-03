package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import java.sql.ResultSet

class ResultSetTraversableSpec extends FlatSpec with ShouldMatchers with Settings {

  val tableNamePrefix = "emp_ResultSetTraversableSpec" + System.currentTimeMillis()

  behavior of "ResultSetTraversable"

  it should "be available (result size 0)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " where id = 9999999999").executeQuery()
      new ResultSetTraversable(rs).foreach(rs => rs.int("id") should not be null)
    }
  }

  it should "be available (result size 1)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize1"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " order by id limit 1").executeQuery()
      new ResultSetTraversable(rs).foreach(_.int("id") should not equal (null))
    }
  }

  it should "be available (result size 2)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize2"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " order by id limit 2").executeQuery()
      new ResultSetTraversable(rs).foreach(_.int("id") should not equal (null))
    }
  }
}
