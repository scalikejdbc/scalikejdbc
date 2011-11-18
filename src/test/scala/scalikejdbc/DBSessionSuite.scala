package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql._
import org.scalatest.BeforeAndAfter

@RunWith(classOf[JUnitRunner])
class DBSessionSuite extends FunSuite with ShouldMatchers with BeforeAndAfter {

  type ? = this.type // for IntelliJ IDEA

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val url = "jdbc:hsqldb:mem:hsqldb:DBSessionSuite"
  val user = ""
  val password = ""

  before {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    // already started transaction
    db.begin()
    val autoCommitSession = db.autoCommitSession()
    try {
      autoCommitSession.execute("create table emp (id integer primary key, name varchar(30))")
    } catch {
      case _: Exception =>
    }
    autoCommitSession.execute("truncate table emp")
    autoCommitSession.execute("insert into emp (id, name) values (?, ?)", 1, "name1")
    autoCommitSession.execute("insert into emp (id, name) values (?, ?)", 2, "name2")
  }

  test("available") {
    val conn = DriverManager.getConnection(url, user, password)
    val session = new DBSession(conn)
    session should not be null
  }

  // --------------------
  // auto commit

  test("asOne in auto commit mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val session = db.autoCommitSession()
    val result = session.asOne("select id from emp where id = ?", 1) {
      rs => Some(rs.getString("id"))
    }
    result.get should equal("1")
  }

  test("asList in auto commit mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val session = db.autoCommitSession()
    val result = session.asList("select id from emp") {
      rs => Some(rs.getString("id"))
    }
    result.size should equal(2)
  }

  test("update in auto commit mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val session = db.autoCommitSession()
    val count = session.update("update emp set name = ? where id = ?", "foo", 1)
    db.rollbackIfActive()
    count should equal(1)
    val name = session.asOne("select name from emp where id = ?", 1) {
      rs => Some(rs.getString("name"))
    }.get
    name should equal("foo")
  }

  // --------------------
  // within tx mode

  test("asOne in within tx mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val session = db.withinTxSession()
    val result = session.asOne("select id from emp where id = ?", 1) {
      rs => Some(rs.getString("id"))
    }
    result.get should equal("1")
    db.rollbackIfActive()
  }

  test("asList in within tx mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val session = db.withinTxSession()
    val result = session.asList("select id from emp") {
      rs => Some(rs.getString("id"))
    }
    result.size should equal(2)
    db.rollbackIfActive()
  }

  test("update in within tx mode") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val session = db.withinTxSession()
    val count = session.update("update emp set name = ? where id = ?", "foo", 1)
    count should equal(1)
    db.rollbackIfActive()

    val name = session.asOne("select name from emp where id = ?", 1) {
      rs => Some(rs.getString("name"))
    }.get
    name should equal("name1")
  }

}
