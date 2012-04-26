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
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " where id = 9999999999").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach (_ => iterator.hasNext should equal(false))
      try {
        iterator.next()
        fail("NoSuchElementException is expected.")
      } catch { case e: NoSuchElementException => }
    }
  }

  it should "be available (result size 1)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize1"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " limit 1").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach (_ => iterator.hasNext should equal(true))
      iterator.next()
      1 to 5 foreach (_ => iterator.hasNext should equal(false))
    }
  }

  it should "be available (result size 2)" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_fetchSize2"
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " limit 2").executeQuery()
      val iterator = new ResultSetIterator(rs)
      1 to 5 foreach (_ => iterator.hasNext should equal(true))

      val rs1 = iterator.next()
      rs1.string("name") should not be (null)

      1 to 5 foreach (_ => iterator.hasNext should equal(true))

      val rs2 = iterator.next()

      try {
        rs1.string("name")
        fail("IllegalStateException is expected here.")
      } catch {
        case e: IllegalStateException =>
          e.getMessage should equal("Invalid cursor position (actual:2,expected:1)")
      }

      rs2.string("name") should not be (null)

      1 to 5 foreach (_ => iterator.hasNext should equal(false))
    }
  }

}

