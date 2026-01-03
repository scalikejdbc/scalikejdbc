package scalikejdbc

import util.control.Exception._
import java.sql.ResultSet
import java.util.NoSuchElementException
import scalikejdbc.LoanPattern._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ResultSetIteratorSpec extends AnyFlatSpec with Matchers with Settings {

  val tableNamePrefix = "emp_ResultSetIteratorSpec" + System.currentTimeMillis()

  behavior of "ResultSetIterator"

  it can "call hasNext many times" in {
    val tableName = tableNamePrefix + "_hasNext_many_times"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = conn
          .prepareStatement(
            "select * from " + tableName + " where id = 9999999999"
          )
          .executeQuery()
        val i = new ResultSetIterator(rs)
        assert(i.hasNext === false)
        assert(i.hasNext === false)
        assert(i.hasNext === false)
        assert(i.size === 0)
        intercept[NoSuchElementException] {
          i.next()
        }
        intercept[NoSuchElementException] {
          i.next()
        }
      }

      (1 to 2).foreach { count =>
        using(ConnectionPool.borrow()) { conn =>
          val rs: ResultSet = {
            try {
              conn
                .prepareStatement(
                  s"select * from $tableName order by id limit $count"
                )
                .executeQuery()
            } catch {
              case e: Exception =>
                conn
                  .prepareStatement(
                    s"select * from $tableName order by id fetch first $count rows only"
                  )
                  .executeQuery()
            }
          }

          val i = new ResultSetIterator(rs)
          assert(i.hasNext === true)
          assert(i.hasNext === true)
          assert(i.hasNext === true)
          assert(i.size === count) // consume all elements
          assert(i.hasNext === false)
          assert(i.hasNext === false)
          assert(i.hasNext === false)
          intercept[NoSuchElementException] {
            i.next()
          }
          intercept[NoSuchElementException] {
            i.next()
          }
        }
      }
    }
  }

  it should "be available (result size 0)" in {
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = conn
          .prepareStatement(
            "select * from " + tableName + " where id = 9999999999"
          )
          .executeQuery()
        new ResultSetIterator(rs).foreach(rs =>
          rs.int("id") should not equal null
        )
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
            conn
              .prepareStatement(
                "select * from " + tableName + " order by id limit 1"
              )
              .executeQuery()
          } catch {
            case e: Exception =>
              conn
                .prepareStatement(
                  "select * from " + tableName + " order by id fetch first 1 rows only"
                )
                .executeQuery()
          }
        }
        new ResultSetIterator(rs).foreach(_.int("id") should not equal null)
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
            conn
              .prepareStatement(
                "select * from " + tableName + " order by id limit 2"
              )
              .executeQuery()
          } catch {
            case e: Exception =>
              conn
                .prepareStatement(
                  "select * from " + tableName + " order by id fetch first 2 rows only"
                )
                .executeQuery()
          }
        }
        new ResultSetIterator(rs).foreach(_.int("id") should not equal null)
      }
    }
  }

  it should "be enable to fold (result size 0)" in {
    val tableName = tableNamePrefix + "_fetchSize0"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(ConnectionPool.borrow()) { conn =>
        val rs: ResultSet = conn
          .prepareStatement(
            "select * from " + tableName + " where id = 9999999999"
          )
          .executeQuery()
        new ResultSetIterator(rs).foldLeft[List[Int]](Nil) { case (r, rs) =>
          rs.int("id") :: r
        } should not be null
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
            conn
              .prepareStatement(
                "select * from " + tableName + " order by id limit 1"
              )
              .executeQuery()
          } catch {
            case e: Exception =>
              conn
                .prepareStatement(
                  "select * from " + tableName + " order by id fetch first 1 rows only"
                )
                .executeQuery()
          }
        }
        new ResultSetIterator(rs).foldLeft[List[Int]](Nil) { case (r, rs) =>
          rs.int("id") :: r
        } should not be null
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
            conn
              .prepareStatement(
                "select * from " + tableName + " order by id limit 2"
              )
              .executeQuery()
          } catch {
            case e: Exception =>
              conn
                .prepareStatement(
                  "select * from " + tableName + " order by id fetch first 2 rows only"
                )
                .executeQuery()
          }
        }
        new ResultSetIterator(rs).foldLeft[List[Int]](Nil) { case (r, rs) =>
          rs.int("id") :: r
        } should not be null
      }
    }
  }

}
