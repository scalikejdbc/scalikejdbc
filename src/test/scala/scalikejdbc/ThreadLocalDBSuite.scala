package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.concurrent.ops._
import java.sql.DriverManager
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class ThreadLocalDBSuite extends FunSuite with ShouldMatchers with BeforeAndAfter {

  type ? = this.type // for IntelliJ IDEA

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val url = "jdbc:hsqldb:mem:hsqldb:ThreadLocalDBSuite"
  val user = ""
  val password = ""

  before {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db autoCommit {
      session => {
        try {
          session.execute("create table emp (id integer primary key, name varchar(30))")
        } catch {
          case _: Exception =>
        }
        session.execute("truncate table emp")
        session.execute("insert into emp (id, name) values (?, ?)", 1, "name1")
        session.execute("insert into emp (id, name) values (?, ?)", 2, "name2")
      }
    }
  }

  test("available") {
    ThreadLocalDB.isInstanceOf[Singleton] should equal(true)
  }

  test("with multi threads") {

    spawn {

      val conn = DriverManager.getConnection(url, user, password)
      val createdDB = ThreadLocalDB.create(conn)
      createdDB.begin();

      // ... do something

      val db = ThreadLocalDB.load()
      val session = db.withinTxSession()
      session.update("update emp set name = ? where id = ?", "foo", 1)
      Thread.sleep(1000L)
      val name = session.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
      assert(name.get == "foo")

      db.currentTx.rollback()

    }

    spawn {

      val conn = DriverManager.getConnection(url, user, password)
      ThreadLocalDB.create(conn)

      val db = ThreadLocalDB.load()
      db.begin()
      val session = db.withinTxSession()
      Thread.sleep(200L)
      val name = session.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
      assert(name.get == "name1")

      db.currentTx.rollback()

    }

    Thread.sleep(2000L)

    val conn = DriverManager.getConnection(url, user, password)
    ThreadLocalDB.create(conn)
    val name = ThreadLocalDB.load autoCommit {
      session => {
        session.asOne("select name from emp where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }
    }
    assert(name.get == "name1")

  }

}
