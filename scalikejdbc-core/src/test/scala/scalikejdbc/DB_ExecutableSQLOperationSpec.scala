package scalikejdbc

import org.scalatest._
import util.control.Exception._
import scalikejdbc.LoanPattern._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DB_ExecutableSQLOperationSpec
  extends AnyFlatSpec
  with Matchers
  with BeforeAndAfter
  with Settings {

  val tableNamePrefix =
    "emp_DB_ExecSQLOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(Executable SQL Operation)"

  it should "be available" in {
    using(DB(ConnectionPool.borrow())) { db =>
      db should not be null
    }
  }

  it should "execute queries" in {
    val tableName = tableNamePrefix + "_query"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(DB(ConnectionPool.borrow())) { db =>
        val idOpt = db autoCommit { implicit session =>
          SQL("select id from " + tableName + " where id = /*'id*/123")
            .bindByName("id" -> 1)
            .map(rs => rs.int("id"))
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
        intercept[Exception] {
          SQL(
            "select id from " + tableName + " where id = /*'id*/123 and name = /*'name*/'AAA'"
          )
            .bindByName("id" -> 1)
            .map(rs => rs.int("id"))
            .toOption
            .apply()
        }
        intercept[Exception] {
          SQL("select id from " + tableName + " where id = /*'id*/123")
            .bindByName("idd" -> 1)
            .map(rs => rs.int("id"))
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
          SQL(
            "update " + tableName + " set name = /* 'name */'Alice' where id = /* 'id */123"
          )
            .bindByName("name" -> "foo", "id" -> 1)
            .executeUpdate
            .apply()
        }
        count should equal(1)
      }

      using(ConnectionPool.borrow()) { conn =>
        val name = (DB(conn) autoCommit { implicit session =>
          SQL("select name from " + tableName + " where id = /* 'id */123")
            .bindByName("id" -> 1)
            .map(rs => rs.string("name"))
            .single
            .apply()
        }).get
        name should equal("foo")
      }
    }
  }

}
