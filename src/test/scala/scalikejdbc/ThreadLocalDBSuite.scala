package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.concurrent.ops._
import java.sql.DriverManager
import org.scalatest.BeforeAndAfter
import util.control.Exception._
import scalikejdbc.LoanPattern._

@RunWith(classOf[JUnitRunner])
class ThreadLocalDBSuite extends FunSuite with ShouldMatchers with BeforeAndAfter with Settings {

  type ? = this.type // for IntelliJ IDEA

  val tableNamePrefix = "emp_ThreadLocalDBSuite"

  test("available") {
    ThreadLocalDB.isInstanceOf[Singleton] should equal(true)
  }

  test("with multi threads") {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_";
    ultimately(TestUtils.deleteTable(conn, tableName)) {
      TestUtils.initialize(conn, tableName)
      spawn {
        val createdDB = ThreadLocalDB.create(ConnectionPool.borrow())
        createdDB.begin();
        // ... do something
        using(ThreadLocalDB.load()) {
          db =>
            {
              val session = db.withinTxSession()
              session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
              Thread.sleep(1000L)
              val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
                rs => Some(rs.getString("name"))
              }
              assert(name.get == "foo")
              db.rollback()
            }
        }
      }

      spawn {
        ThreadLocalDB.create(ConnectionPool.borrow())
        using(ThreadLocalDB.load()) {
          db =>
            {
              db.begin()
              val session = db.withinTxSession()
              Thread.sleep(200L)
              val name = session.asOne("select name from " + tableName + " where id = ?", 1) {
                rs => Some(rs.getString("name"))
              }
              assert(name.get == "name1")
              db.rollback()
            }
        }
      }

      Thread.sleep(2000L)

      ThreadLocalDB.create(ConnectionPool.borrow())
      using(ThreadLocalDB.load()) {
        db =>
          {
            val name = db autoCommit {
              session =>
                {
                  session.asOne("select name from " + tableName + " where id = ?", 1) {
                    rs => Some(rs.getString("name"))
                  }
                }
            }
            assert(name.get == "name1")
          }
      }
    }

  }

}
