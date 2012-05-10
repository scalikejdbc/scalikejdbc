package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter
import scala.concurrent.ops._
import java.sql.SQLException
import util.control.Exception._

class DB_SQLOperationSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_DB_SQLOperationSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB(SQL Operation)"

  it should "be available" in {
    val db = new DB(() => ConnectionPool.borrow())
    db should not be null
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db readOnly {
        implicit session =>
          SQL("select * from " + tableName + "").map(rs => Some(rs.string("name"))).toList.apply()
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      implicit val session = db.readOnlySession()
      try {
        val result = SQL("select * from " + tableName + "") map (rs => Some(rs.string("name"))) toList () apply ()
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      intercept[SQLException] {
        db readOnly {
          implicit session => SQL("update " + tableName + " set name = ?").bind("xxx").executeUpdate().apply()
        }
      }
    }
  }

  // --------------------
  // autoCommit

  it should "execute query in autoCommit block" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db autoCommit {
        implicit session =>
          SQL("select * from " + tableName + "").map(rs => Some(rs.string("name"))).toList().apply()
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      implicit val session = db.autoCommitSession()
      try {
        val list = SQL("select id from " + tableName + " order by id").map(rs => rs.int("id")).toList().apply()
        list(0) should equal(1)
        list(1) should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db autoCommit {
        implicit session =>
          SQL("select id from " + tableName + " where id = ?").bind(1).map(rs => rs.int("id")).toOption().apply()
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      intercept[TooManyRowsException] {
        db autoCommit {
          implicit session =>
            SQL("select id from " + tableName + "").map(rs => Some(rs.int("id"))).toOption().apply()
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = db readOnly {
        implicit session =>
          SQL("select * from " + tableName + " where id = ?").bind(1).map(extractName).toOption().apply()
      }
      name.get should be === "name1"
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db autoCommit {
        implicit session =>
          SQL("select id from " + tableName + "").map(rs => Some(rs.int("id"))).toList().apply()
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db autoCommit {
        implicit session =>
          SQL("select id from " + tableName + "").map(rs => rs.int("id")).toTraversable().apply()
            .foreach {
              case (id) => println(id)
            }
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = new DB(() => ConnectionPool.borrow()) autoCommit {
        implicit session =>
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      }
      count should equal(1)
      val name = (new DB(() => ConnectionPool.borrow()) autoCommit {
        implicit session =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).toOption().apply()
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlockAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (new DB(() => ConnectionPool.borrow()) readOnly {
        implicit s =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(_.string("name")).toOption().apply()
      }).get
      name should equal("name1")
      val count = new DB(() => ConnectionPool.borrow()) autoCommit {
        implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      }
      count should equal(1)
    }
  }

  // --------------------
  // localTx

  it should "execute single in localTx block" in {
    val tableName = tableNamePrefix + "_singleInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db localTx {
        implicit s =>
          SQL("select id from " + tableName + " where id = ?").bind(1).map(rs => rs.string("id")).toOption().apply()
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val result = db localTx {
        implicit s =>
          SQL("select id from " + tableName + "").map(rs => Some(rs.string("id"))).toList().apply()
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = new DB(() => ConnectionPool.borrow()) localTx {
        implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      }
      count should be === 1
      val name = (new DB(() => ConnectionPool.borrow()) localTx {
        implicit s =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).toOption().apply()
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      val count = db localTx {
        implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      }
      count should be === 1
      db.rollbackIfActive()
      val name = (new DB(() => ConnectionPool.borrow()) localTx {
        implicit s =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single().apply()
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      intercept[IllegalStateException] {
        db withinTx {
          implicit session =>
            SQL("select * from " + tableName + "").map(rs => Some(rs.string("name"))).list().apply()
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        implicit session =>
          SQL("select * from " + tableName + "").map(rs => Some(rs.string("name"))).list().apply()
      }
      result.size should be > 0
      db.rollbackIfActive()
    }
  }

  it should "execute query in withinTx session" in {
    val tableName = tableNamePrefix + "_queryInWithinTxSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      implicit val session = db.withinTxSession()
      try {
        val result = SQL("select * from " + tableName + "").map(rs => Some(rs.string("name"))).list().apply()
        result.size should be > 0
        db.rollbackIfActive()
      } finally { session.close() }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        implicit s =>
          SQL("select id from " + tableName + " where id = ?").bind(1).map(rs => rs.string("id")).single().apply()
      }
      result.get should equal("1")
      db.rollbackIfActive()
    }
  }

  it should "execute list in withinTx block" in {
    val tableName = tableNamePrefix + "_listInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val result = db withinTx {
        implicit s =>
          SQL("select id from " + tableName + "").map(rs => Some(rs.string("id"))).list().apply()
      }
      result.size should equal(2)
      db.rollbackIfActive()
    }
  }

  it should "execute update in withinTx block" in {
    val tableName = tableNamePrefix + "_updateInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val db = new DB(() => ConnectionPool.borrow())
      db.begin()
      val count = db withinTx {
        implicit s =>
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
      }
      count should be === 1
      val name = (db withinTx {
        implicit s =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single().apply()
      }).get
      name should equal("foo")
      db.rollback()
    }
  }

  it should "rollback in withinTx block" in {
    val tableName = tableNamePrefix + "_rollbackInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(new DB(() => ConnectionPool.borrow())) {
        db =>
          db.begin()
          val count = db withinTx {
            implicit s =>
              SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate().apply()
          }
          count should be === 1
          db.rollback()
          db.begin()
          val name = (db withinTx {
            implicit s =>
              SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single().apply()
          }).get
          name should equal("name1")
      }
    }
  }

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      spawn {
        val db = new DB(() => ConnectionPool.borrow())
        db.begin()
        implicit val session = db.withinTxSession()
        try {
          SQL("update " + tableName + " set name = ? where id = ?").bind("foo", 1).executeUpdate()
          Thread.sleep(1000L)
          val name = SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single().apply()
          name.get should equal("foo")
          db.rollback()
        } finally { session.close() }
      }
      spawn {
        val db = new DB(() => ConnectionPool.borrow())
        db.begin()
        implicit val session = db.withinTxSession()
        try {
          Thread.sleep(200L)
          val name = SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single().apply()
          name.get should equal("name1")
          db.rollback()
        } finally { session.close() }
      }

      Thread.sleep(2000L)

      val name = new DB(() => ConnectionPool.borrow()) autoCommit {
        implicit session =>
          SQL("select name from " + tableName + " where id = ?").bind(1).map(rs => rs.string("name")).single.apply()
      }
      name.get should equal("name1")
    }
  }

}
