package scalikejdbc

import org.scalatest._
import util.control.Exception._
import scalikejdbc.LoanPattern._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DB_AnormSQLOperationSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings {

  val tableNamePrefix =
    "emp_DB_AnromSQLOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(Anorm like SQL Operation)"

  it should "be available" in {
    using(ConnectionPool.borrow()) { conn =>
      val db = DB(conn)
      db should not be null
    }
  }

  it should "execute queries" in {
    val tableName = tableNamePrefix + "_query"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        val idOpt = db autoCommit { implicit session =>
          SQL("select id from " + tableName + " where id = {id}")
            .bindByName("id" -> 1)
            .map(_.int("id"))
            .toOption
            .apply()
        }
        idOpt.get should equal(1)
      }
    }
  }

  it should "throw Exception when a bind name is not found" in {
    val tableName = tableNamePrefix + "_query"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB readOnly { implicit session =>
        intercept[IllegalArgumentException] {
          SQL(
            "select id from " + tableName + " where id = {id} and name = {name}"
          )
            .bindByName("id" -> 1)
            .map(_.int("id"))
            .toOption
            .apply()
        }
        intercept[IllegalStateException] {
          SQL("select id from " + tableName + " where id = {id}")
            .bindByName("idd" -> 1)
            .map(_.int("id"))
            .toOption
            .apply()
        }
      }
    }
  }

  it should "execute update" in {
    val tableName = tableNamePrefix + "_update"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      using(ConnectionPool.borrow()) { conn =>
        val count = DB(conn) autoCommit { implicit session =>
          SQL("update " + tableName + " set name = {name} where id = {id}")
            .bindByName("name" -> "foo", "id" -> 1)
            .executeUpdate
            .apply()
        }
        count should equal(1)
      }

      using(ConnectionPool.borrow()) { conn =>
        val name = (DB(conn) autoCommit { implicit session =>
          SQL("select name from " + tableName + " where id = {id}")
            .bindByName("id" -> 1)
            .map(_.string("name"))
            .single
            .apply()
        }).get
        name should equal("foo")
      }
    }
  }

}
