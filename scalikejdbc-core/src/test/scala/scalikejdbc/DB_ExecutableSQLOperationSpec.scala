package scalikejdbc

import org.scalatest._
import util.control.Exception._

class DB_ExecutableSQLOperationSpec extends FlatSpec with Matchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DB_ExecSQLOp" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(Executable SQL Operation)"

  it should "be available" in {
    val db = DB(ConnectionPool.borrow())
    db should not be null
  }

  it should "execute queries" in {
    val tableName = tableNamePrefix + "_query"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = DB(ConnectionPool.borrow())
      val idOpt = db autoCommit {
        implicit session =>
          SQL("select id from " + tableName + " where id = /*'id*/123")
            .bindByName('id -> 1)
            .map(rs => rs.int("id")).toOption().apply()
      }
      idOpt.get should equal(1)
    }
  }

  it should "throw Exception when a bind name is not found" in {
    val tableName = tableNamePrefix + "_query"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      DB readOnly {
        implicit session =>
          intercept[Exception] {
            SQL("select id from " + tableName + " where id = /*'id*/123 and name = /*'name*/'AAA'")
              .bindByName('id -> 1)
              .map(rs => rs.int("id")).toOption().apply()
          }
          intercept[Exception] {
            SQL("select id from " + tableName + " where id = /*'id*/123")
              .bindByName('idd -> 1)
              .map(rs => rs.int("id")).toOption().apply()
          }
      }
    }
  }

  it should "execute update" in {
    val tableName = tableNamePrefix + "_update"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = DB(ConnectionPool.borrow()) autoCommit {
        implicit session =>
          SQL("update " + tableName + " set name = /* 'name */'Alice' where id = /* 'id */123")
            .bindByName(
              'name -> "foo",
              'id -> 1
            ).executeUpdate().apply()
      }
      count should equal(1)
      val name = (DB(ConnectionPool.borrow()) autoCommit {
        implicit session =>
          SQL("select name from " + tableName + " where id = /* 'id */123")
            .bindByName('id -> 1)
            .map(rs => rs.string("name")).single.apply()
      }).get
      name should equal("foo")
    }
  }

}
