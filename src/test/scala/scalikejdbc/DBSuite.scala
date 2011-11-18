package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.BeforeAndAfter
import scala.concurrent.ops._
import java.sql.{SQLException, DriverManager}

@RunWith(classOf[JUnitRunner])
class DBSuite extends FunSuite with ShouldMatchers with BeforeAndAfter {

  type ? = this.type // for IntelliJ IDEA

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val url = "jdbc:hsqldb:mem:hsqldb:DBSuite"
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
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db should not be null
  }

  // --------------------
  // tx

  test("cannot call DB#begin twice") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    intercept[IllegalStateException] {
      db.begin()
    }
    db.rollback()
  }

  test("can call DB#beginIfNotYet several times") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.beginIfNotYet()
    db.rollback()
    db.rollbackIfActive()
  }

  test("before beginning tx, DB#tx is not available") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    intercept[IllegalStateException] {
      db.tx.begin()
    }
    db.rollbackIfActive()
  }

  // --------------------
  // readOnly

  test("query in readOnly block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db readOnly {
      session => session.asList("select * from emp") {
        rs => Some(rs.getString("name"))
      }
    }
    result.size should be > 0
  }

  test("query in readOnlySession") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val session = db.readOnlySession()
    val result = session.asList("select * from emp") {
      rs => Some(rs.getString("name"))
    }
    result.size should be > 0
  }

  test("cannot update in readOnly block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    intercept[SQLException] {
      db readOnly {
        session => session.update("update emp set name = ?", "xxx")
      }
    }
  }

  // --------------------
  // autoCommit

  test("query in autoCommit block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db autoCommit {
      session => session.asList("select * from emp") {
        rs => Some(rs.getString("name"))
      }
    }
    result.size should be > 0
  }

  test("query in autoCommitSession") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val session = db.autoCommitSession()
    val list = session.asList("select id from emp")(rs => Some(rs.getInt("id")))
    list(0) should equal(1)
    list(1) should equal(2)
  }

  test("asOne in autoCommit block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db autoCommit {
      _.asOne("select id from emp where id = ?", 1) {
        rs => Some(rs.getInt("id"))
      }
    }
    result.get should equal(1)
  }

  test("asOne returns too many results in autoCommit block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    intercept[TooManyRowsException] {
      db autoCommit {
        _.asOne("select id from emp") {
          rs => Some(rs.getInt("id"))
        }
      }
    }
  }

  test("asOne in autoCommit block 2") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val extractName = (rs: java.sql.ResultSet) => Some(rs.getString("name"))
    val name: Option[String] = db readOnly {
      _.asOne("select * from emp where id = ?", 1)(extractName)
    }
    name.get should be === "name1"
  }

  test("asList in autoCommit block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db autoCommit {
      _.asList("select id from emp") {
        rs => Some(rs.getInt("id"))
      }
    }
    result.size should equal(2)
  }

  test("update in autoCommit block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val count = db autoCommit {
      _.update("update emp set name = ? where id = ?", "foo", 1)
    }
    count should equal(1)
    val name = (db autoCommit {
      _.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
    }).get
    name should equal("foo")
  }

  // --------------------
  // localTx

  test("asOne in localTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db localTx {
      _.asOne("select id from emp where id = ?", 1) {
        rs => Some(rs.getString("id"))
      }
    }
    result.get should equal("1")
  }

  test("asList in localTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val result = db localTx {
      _.asList("select id from emp") {
        rs => Some(rs.getString("id"))
      }
    }
    result.size should equal(2)
  }

  test("update in localTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val count = db localTx {
      _.update("update emp set name = ? where id = ?", "foo", 1)
    }
    count should be === 1
    val name = (db localTx {
      _.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
    }).get
    name should equal("foo")
  }

  test("rollback in localTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    val count = db localTx {
      _.update("update emp set name = ? where id = ?", "foo", 1)
    }
    count should be === 1
    db.rollbackIfActive()
    val name = (db localTx {
      _.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
    }).get
    name should equal("foo")
  }

  // --------------------
  // withinTx

  test("query in withinTx block before beginning tx") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    intercept[IllegalStateException] {
      db withinTx {
        session => session.asList("select * from emp") {
          rs => Some(rs.getString("name"))
        }
      }
    }
  }

  test("query in withinTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val result = db withinTx {
      session => session.asList("select * from emp") {
        rs => Some(rs.getString("name"))
      }
    }
    result.size should be > 0
    db.rollbackIfActive()
  }

  test("query in withinTxSession") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val session = db.withinTxSession()
    val result = session.asList("select * from emp") {
      rs => Some(rs.getString("name"))
    }
    result.size should be > 0
    db.rollbackIfActive()
  }

  test("asOne in withinTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val result = db withinTx {
      _.asOne("select id from emp where id = ?", 1) {
        rs => Some(rs.getString("id"))
      }
    }
    result.get should equal("1")
    db.rollbackIfActive()
  }

  test("asList in withinTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val result = db withinTx {
      _.asList("select id from emp") {
        rs => Some(rs.getString("id"))
      }
    }
    result.size should equal(2)
    db.rollbackIfActive()
  }

  test("update in withinTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()

    val count = db withinTx {
      _.update("update emp set name = ? where id = ?", "foo", 1)
    }
    count should be === 1
    val name = (db withinTx {
      _.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
    }).get
    name should equal("foo")
    db.rollback()
  }

  test("rollback in withinTx block") {
    val conn = DriverManager.getConnection(url, user, password)
    val db = new DB(conn)
    db.begin()
    val count = db withinTx {
      _.update("update emp set name = ? where id = ?", "foo", 1)
    }
    count should be === 1
    db.rollback()
    db.begin()
    val name = (db withinTx {
      _.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
    }).get
    name should equal("name1")
  }

  // --------------------
  // multi threads

  test("testing with multi threads") {

    spawn {
      val conn = DriverManager.getConnection(url, user, password)
      val db = new DB(conn)
      db.begin()
      val session = db.withinTxSession()
      session.update("update emp set name = ? where id = ?", "foo", 1)
      Thread.sleep(1000L)
      val name = session.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
      assert(name.get == "foo")
      db.rollback()
    }
    spawn {
      val conn = DriverManager.getConnection(url, user, password)
      val db = new DB(conn)
      db.begin()
      val session = db.withinTxSession()
      Thread.sleep(200L)
      val name = session.asOne("select name from emp where id = ?", 1) {
        rs => Some(rs.getString("name"))
      }
      assert(name.get == "name1")
      db.rollback()
    }

    Thread.sleep(2000L)

    val conn = DriverManager.getConnection(url, user, password)
    val name = new DB(conn) autoCommit {
      session => {
        session.asOne("select name from emp where id = ?", 1) {
          rs => Some(rs.getString("name"))
        }
      }
    }
    assert(name.get == "name1")

  }

}
