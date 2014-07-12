package scalikejdbc

import util.control.Exception._
import org.scalatest._
import java.sql.ResultSet
import scalikejdbc.LoanPattern._

class ResultSetTraversableSpec extends FlatSpec with Matchers with Settings {

  val tableNamePrefix = "emp_ResultSetTraversableSpec" + System.currentTimeMillis()

  behavior of "ResultSetTraversable"

  it should "be available (result size 0)" in {
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " where id = 9999999999").executeQuery()
        new ResultSetTraversable(rs).foreach(rs => rs.int("id") should not equal (null))
      }
    }
  }

  it should "be available (result size 1)" in {
    val tableName = tableNamePrefix + "_fetchSize1"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = {
          try {
            conn.prepareStatement("select * from " + tableName + " order by id limit 1").executeQuery()
          } catch {
            case e: Exception =>
              conn.prepareStatement("select * from " + tableName + " order by id fetch first 1 rows only").executeQuery()
          }
        }
        new ResultSetTraversable(rs).foreach(_.int("id") should not equal (null))
      }
    }
  }

  it should "be available (result size 2)" in {
    val tableName = tableNamePrefix + "_fetchSize2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = {
          try {
            conn.prepareStatement("select * from " + tableName + " order by id limit 2").executeQuery()
          } catch {
            case e: Exception =>
              conn.prepareStatement("select * from " + tableName + " order by id fetch first 2 rows only").executeQuery()
          }
        }
        new ResultSetTraversable(rs).foreach(_.int("id") should not equal (null))
      }
    }
  }

  it should "be enable to fold (result size 0)" in {
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = conn.prepareStatement("select * from " + tableName + " where id = 9999999999").executeQuery()
        new ResultSetTraversable(rs).foldLeft[List[Int]](Nil) { case (r, rs) => rs.int("id") :: r } should not be null
      }
    }
  }

  it should "be enable to fold (result size 1)" in {
    val tableName = tableNamePrefix + "_fetchSize1"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = {
          try {
            conn.prepareStatement("select * from " + tableName + " order by id limit 1").executeQuery()
          } catch {
            case e: Exception =>
              conn.prepareStatement("select * from " + tableName + " order by id fetch first 1 rows only").executeQuery()
          }
        }
        new ResultSetTraversable(rs).foldLeft[List[Int]](Nil) { case (r, rs) => rs.int("id") :: r } should not be null
      }
    }
  }

  it should "be enable to fold (result size 2)" in {
    val tableName = tableNamePrefix + "_fetchSize2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = {
          try {
            conn.prepareStatement("select * from " + tableName + " order by id limit 2").executeQuery()
          } catch {
            case e: Exception =>
              conn.prepareStatement("select * from " + tableName + " order by id fetch first 2 rows only").executeQuery()
          }
        }
        new ResultSetTraversable(rs).foldLeft[List[Int]](Nil) { case (r, rs) => rs.int("id") :: r } should not be null
      }
    }
  }

}
