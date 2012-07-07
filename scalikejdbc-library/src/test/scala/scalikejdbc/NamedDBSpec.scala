package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.scalatest.BeforeAndAfter
import scala.concurrent.ops._
import java.sql.SQLException
import util.control.Exception._

class NamedDBSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_NamedDBSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "NamedDB"

  it should "be available" in {
    using(ConnectionPool.borrow('named)) { conn =>
      using(new DB(conn)) { db =>
        db should not be null
      }
    }
  }

  // --------------------
  // tx

  "#tx" should "not be available before beginning tx" in {
    using(NamedDB('named)) { db =>
      intercept[IllegalStateException] {
        db.tx.begin()
      }
      db.rollbackIfActive()
    }
  }

  // --------------------
  // readOnly

  it should "execute query in readOnly block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        val result = db readOnly {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        }
        result.size should be > 0
      }
    }
  }

  it should "execute query in readOnlyWithConnection block" in {
    val tableName = tableNamePrefix + "_queryInReadOnlyWithConnectionBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB('named) readOnlyWithConnection {
        implicit conn =>
          import anorm._
          SQL("select * from " + tableName)().toList
      }
      result.size should be > 0
    }
  }

  it should "execute query in readOnly session" in {
    val tableName = tableNamePrefix + "_queryInReadOnlySession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = NamedDB('named).readOnlySession()
      try {
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
      } finally { session.close() }
    }
  }

  it should "not execute update in readOnly block" in {
    val tableName = tableNamePrefix + "_cannotUpdateInReadOnlyBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[SQLException] {
        NamedDB('named) readOnly {
          session => session.update("update " + tableName + " set name = ?", "xxx")
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
      val result = NamedDB('named) autoCommit {
        session =>
          session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommitWithConnection block" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitWithConnectionBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB('named) autoCommitWithConnection {
        implicit conn =>
          import anorm._
          SQL("select * from " + tableName)().toList
      }
      result.size should be > 0
    }
  }

  it should "execute query in autoCommit session" in {
    val tableName = tableNamePrefix + "_queryInAutoCommitSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val session = NamedDB('named).autoCommitSession()
      try {
        val list = session.list("select id from " + tableName + " order by id")(rs => rs.int("id"))
        list(0) should equal(1)
        list(1) should equal(2)
      } finally { session.close() }
    }
  }

  it should "execute single in autoCommit block" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB('named) autoCommit {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.int("id"))
      }
      result.get should equal(1)
    }
  }

  "single" should "return too many results in autoCommit block" in {
    val tableName = tableNamePrefix + "_tooManyResultsInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[TooManyRowsException] {
        NamedDB('named) autoCommit {
          _.single("select id from " + tableName + "")(rs => Some(rs.int("id")))
        }
      }
    }
  }

  it should "execute single in autoCommit block 2" in {
    val tableName = tableNamePrefix + "_singleInAutoCommitBlock2"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val extractName = (rs: WrappedResultSet) => rs.string("name")
      val name: Option[String] = NamedDB('named) readOnly {
        _.single("select * from " + tableName + " where id = ?", 1)(extractName)
      }
      name.get should be === "name1"
    }
  }

  it should "execute list in autoCommit block" in {
    val tableName = tableNamePrefix + "_listInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB('named) autoCommit {
        _.list("select id from " + tableName + "")(rs => Some(rs.int("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute foreach in autoCommit block" in {
    val tableName = tableNamePrefix + "_asIterInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      NamedDB('named) autoCommit {
        _.foreach("select id from " + tableName + "")(rs => println(rs.int("id")))
      }
    }
  }

  it should "execute update in autoCommit block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        val count = NamedDB('named) autoCommit {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should equal(1)
        val name = (db autoCommit {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).get
        name should equal("foo")
      }
    }
  }

  it should "execute update in autoCommitWithConnection block" in {
    val tableName = tableNamePrefix + "_updateInAutoCommitBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = NamedDB('named) autoCommitWithConnection {
        implicit conn =>
          import anorm._
          SQL("update " + tableName + " set name = {name} where id = {id}").on('name -> "foo", 'id -> 1).executeUpdate()
      }
      count should equal(1)
      val name = (NamedDB('named) autoCommit {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).get
      name should equal("foo")
    }
  }

  it should "execute update in autoCommit block after readOnly" in {
    val tableName = tableNamePrefix + "_updatInAutoCommitAfterReadOnly"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val name = (NamedDB('named) readOnly {
        _.single("select name from " + tableName + " where id = ?", 1)(_.string("name"))
      }).get
      name should equal("name1")
      val count = NamedDB('named) autoCommit {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
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
      val result = NamedDB('named) localTx {
        _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
      }
      result.get should equal("1")
    }
  }

  it should "execute list in localTx block" in {
    val tableName = tableNamePrefix + "_listInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val result = NamedDB('named) localTx {
        _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
      }
      result.size should equal(2)
    }
  }

  it should "execute update in localTx block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = NamedDB('named) localTx {
        _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
      }
      count should be === 1
      val name = (NamedDB('named) localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "execute update in localTxWithConnection block" in {
    val tableName = tableNamePrefix + "_updateInLocalTxWithConnectionBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      val count = NamedDB('named) localTxWithConnection {
        implicit conn =>
          import anorm._
          SQL("update " + tableName + " set name = {name} where id = {id}").on('name -> "foo", 'id -> 1).executeUpdate()
      }
      count should be === 1
      val name = (NamedDB('named) localTx {
        _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }).getOrElse("---")
      name should equal("foo")
    }
  }

  it should "rollback in localTx block" in {
    val tableName = tableNamePrefix + "_rollbackInLocalTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        val count = db localTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should be === 1
        db.rollbackIfActive()
        val name = (NamedDB('named) localTx {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).getOrElse("---")
        name should equal("foo")
      }
    }
  }

  // --------------------
  // withinTx

  it should "not execute query in withinTx block before beginning tx" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBeforeBeginningTx"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      intercept[IllegalStateException] {
        NamedDB('named) withinTx {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        }
      }
    }
  }

  it should "execute query in withinTx block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val result = db withinTx {
          session =>
            session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        }
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute query in withinTxWithConnection block" in {
    val tableName = tableNamePrefix + "_queryInWithinTxWithConnectionBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val result = db withinTxWithConnection {
          implicit conn =>
            import anorm._
            SQL("select * from " + tableName)().toList
        }
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute query in withinTx session" in {
    val tableName = tableNamePrefix + "_queryInWithinTxSession"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val session = db.withinTxSession()
        val result = session.list("select * from " + tableName + "")(rs => Some(rs.string("name")))
        result.size should be > 0
        db.rollbackIfActive()
      }
    }
  }

  it should "execute single in withinTx block" in {
    val tableName = tableNamePrefix + "_singleInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val result = db withinTx {
          _.single("select id from " + tableName + " where id = ?", 1)(rs => rs.string("id"))
        }
        result.get should equal("1")
        db.rollbackIfActive()
      }
    }
  }

  it should "execute list in withinTx block" in {
    val tableName = tableNamePrefix + "_listInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val result = db withinTx {
          _.list("select id from " + tableName + "")(rs => Some(rs.string("id")))
        }
        result.size should equal(2)
        db.rollbackIfActive()
      }
    }
  }

  it should "execute update in withinTx block" in {
    val tableName = tableNamePrefix + "_updateInWithinTxBlock"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      using(NamedDB('named)) { db =>
        db.begin()
        val count = db withinTx {
          _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
        }
        count should be === 1
        val name = (db withinTx {
          _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
        }).get
        name should equal("foo")
        db.rollback()
      }
    }
  }

  it should "rollback in withinTx block" in {
    val tableName = tableNamePrefix + "_rollbackInWithinTxBlock"
    ultimately({
      ignoring(classOf[Throwable]) {
        DB(ConnectionPool.borrow('named)) autoCommit { _.execute("drop table " + tableName) }
      }
    }) {
      NamedDB('named) autoCommit {
        session =>
          handling(classOf[Throwable]) by {
            t =>
              try {
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              } catch {
                case e =>
                  session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              }
              session.update("delete from " + tableName)
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          } apply {
            session.single("select count(1) from " + tableName)(rs => rs.int(1))
            session.update("delete from " + tableName)
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          }
      }
      using(NamedDB('named)) {
        db =>
          db.begin()
          val count = db withinTx {
            _.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          }
          count should be === 1
          db.rollback()
          db.begin()
          val name = (db withinTx {
            _.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          }).get
          name should equal("name1")
      }
    }
  }

  // --------------------
  // multi threads

  it should "work with multi threads" in {
    val tableName = tableNamePrefix + "_testingWithMultiThreads"
    ultimately({
      ignoring(classOf[Throwable]) {
        DB(ConnectionPool.borrow('named)) autoCommit { _.execute("drop table " + tableName) }
      }
    }) {
      NamedDB('named) autoCommit {
        session =>
          handling(classOf[Throwable]) by {
            t =>
              try {
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              } catch {
                case e =>
                  try {
                    session.execute("create table " + tableName + " (id int primary key, name varchar(30))")
                  } catch {
                    case e =>
                  }
              }
              session.update("delete from " + tableName)
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          } apply {
            session.single("select count(1) from " + tableName)(rs => rs.int(1))
            session.update("delete from " + tableName)
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          }
      }
      spawn {
        using(NamedDB('named)) { db =>
          db.begin()
          val session = db.withinTxSession()
          session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
          Thread.sleep(1000L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "foo")
          db.rollback()
        }
      }
      spawn {
        using(NamedDB('named)) { db =>
          db.begin()
          val session = db.withinTxSession()
          Thread.sleep(200L)
          val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          assert(name.get == "name1")
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      val name = NamedDB('named) autoCommit {
        session =>
          session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
      }
      assert(name.get == "name1")
    }
  }

  it should "work with multi threads when using Anorm API" in {
    import anorm._
    import anorm.SqlParser._
    val tableName = tableNamePrefix + "_testingWithMultiThreadsAnorm"
    ultimately({
      ignoring(classOf[Throwable]) {
        NamedDB('named) autoCommit { _.execute("drop table " + tableName) }
      }
    }) {
      NamedDB('named) autoCommit {
        session =>
          handling(classOf[Throwable]) by {
            t =>
              try {
                session.execute("create table " + tableName + " (id integer primary key, name varchar(30))")
              } catch {
                case e =>
                  try {
                    session.execute("create table " + tableName + " (id int primary key, name varchar(30))")
                  } catch {
                    case e =>
                  }
              }
              session.update("delete from " + tableName)
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
              session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          } apply {
            session.single("select count(1) from " + tableName)(rs => rs.int(1))
            session.update("delete from " + tableName)
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 1, "name1")
            session.update("insert into " + tableName + " (id, name) values (?, ?)", 2, "name2")
          }
      }
      spawn {
        using(NamedDB('named)) { db =>
          db.begin()
          db.withinTxWithConnection {
            implicit conn =>
              SQL("update " + tableName + " set name = {name} where id = {id}").on('name -> "foo", 'id -> 1).executeUpdate()
              Thread.sleep(1000L)
              val name = SQL("select name from " + tableName + " where id = {id}").on('id -> 1).as(get[String]("name").singleOpt)
              assert(name.get == "foo")
          }
          db.rollback()
        }
      }
      spawn {
        using(NamedDB('named)) { db =>
          db.begin()
          db.withinTxWithConnection {
            implicit conn =>
              Thread.sleep(200L)
              val name = SQL("select name from " + tableName + " where id = {id}").on('id -> 1).as(get[String]("name").singleOpt)
              assert(name.get == "name1")
          }
          db.rollback()
        }
      }

      Thread.sleep(2000L)

      val name = NamedDB('named) autoCommitWithConnection {
        implicit conn =>
          SQL("select name from " + tableName + " where id = {id}").on('id -> 1).as(get[String]("name").singleOpt)
      }
      assert(name.get == "name1")
    }
  }

}
