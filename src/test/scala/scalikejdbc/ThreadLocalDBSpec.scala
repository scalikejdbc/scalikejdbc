package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import scala.concurrent.ops._
import org.scalatest.BeforeAndAfter
import util.control.Exception._

class ThreadLocalDBSpec extends FlatSpec with ShouldMatchers with BeforeAndAfter with Settings {

  val tableNamePrefix = "emp_ThreadLocalDBSpec" + System.currentTimeMillis()

  behavior of "ThreadLocalDB"

  it should "be available" in {
    ThreadLocalDB.isInstanceOf[Singleton] should equal(true)
  }

  it should "work with multi threads" in {
    val conn = ConnectionPool.borrow()
    val tableName = tableNamePrefix + "_workWithMultiThreads"
    TestUtils.deleteTable(tableName)
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      spawn {
        ThreadLocalDB.create(ConnectionPool.borrow()).begin()
        // ... do something
        using(ThreadLocalDB.load()) {
          db =>
            val session = db.withinTxSession()
            session.update("update " + tableName + " set name = ? where id = ?", "foo", 1)
            Thread.sleep(1000L)
            val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
            assert(name.get == "foo")
            db.rollback()
        }
      }

      spawn {
        ThreadLocalDB.create(ConnectionPool.borrow())
        using(ThreadLocalDB.load()) {
          db =>
            db.begin()
            val session = db.withinTxSession()
            Thread.sleep(200L)
            val name = session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
            assert(name.get == "name1")
            db.rollback()
        }
      }

      Thread.sleep(2000L)

      ThreadLocalDB.create(ConnectionPool.borrow())
      using(ThreadLocalDB.load()) {
        db =>
          val name = db autoCommit {
            session =>
              session.single("select name from " + tableName + " where id = ?", 1)(rs => rs.string("name"))
          }
          assert(name.get == "name1")
      }
    }

  }

}
