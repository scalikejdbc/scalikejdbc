package scalikejdbc

import util.control.Exception._
import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.ResultSet

@RunWith(classOf[JUnitRunner])
class ResultSetIteratorSpec extends FlatSpec with ShouldMatchers with Settings {

  val tableNamePrefix = "emp_ResultSetIteratorSpec" + System.currentTimeMillis()

  behavior of "ResultSetIterator"

  it should "be available (result size 0)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize0";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " where id = 9999999").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach { i =>
        iterator.hasNext should equal(true)
      }
    }
  }

  it should "be available (result size 1)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize1";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " limit 1").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach { i =>
        iterator.hasNext should equal(true)
      }
    }
  }

  it should "be available (result size 2)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize2";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " limit 2").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach { i =>
        iterator.hasNext should equal(true)
      }
    }
  }

}

