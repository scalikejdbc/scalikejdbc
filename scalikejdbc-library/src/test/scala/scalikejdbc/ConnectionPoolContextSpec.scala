package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

object ConnectionPoolContextSpecUtils {

  def createTable(tableName: String) = {
    DB autoCommit { implicit s =>
      try {
        SQL("drop table " + tableName).execute.apply()
      } catch { case e => }
      SQL("create table " + tableName + " (id integer primary key, name varchar(30))").execute.apply()
    }
  }

  def insertData(tableName: String, num: Int) = {
    DB localTx { implicit s =>
      (1 to num).foreach { n =>
        SQL("insert into " + tableName + " (id, name) values (?, ?)").bind(n, "name" + n).update.apply()
      }
    }
  }

  def dropTable(tableName: String) = {
    try {
      DB autoCommit { implicit s =>
        SQL("drop table " + tableName).execute.apply()
      }
    } catch { case e => }
  }

}

trait NamedCPContextAsDefault {
  implicit val context = new MultipleConnectionPoolContext
  context.set('named, ConnectionPool())
}

class ConnectionPoolContextMixinSpec extends FlatSpec with ShouldMatchers with Settings {

  import ConnectionPoolContextSpecUtils._

  val tableNamePrefix = "emp_CPContextSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB with ConnectionPoolContext(mixin)"

  it should "work with NamedConnectionPoolContext" in {
    val tableName = tableNamePrefix + "_withNamedCPContextMixin"
    try {
      createTable(tableName)
      insertData(tableName, 6)

      val result1 = DB readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result1.size should equal(6)

      val result11 = NamedDB(ConnectionPool.DEFAULT_NAME) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result11.size should equal(6)
      result1.zip(result11).foreach { case (a, b) => a should equal(b) }

      val result2 = NamedDB('named)(NoConnectionPoolContext) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result2.size should equal(6)
      result1.zip(result2).foreach { case (a, b) => a should equal(b) }

    } finally {
      dropTable(tableName)
    }
  }

}

class ConnectionPoolContextSpec extends FlatSpec with ShouldMatchers with Settings {

  import ConnectionPoolContextSpecUtils._

  val tableNamePrefix = "emp_CPContextSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DB with ConnectionPoolContext"

  it should "work with NoConnectionPoolContext" in {
    val tableName = tableNamePrefix + "_withNoCPContext"
    try {
      createTable(tableName)
      insertData(tableName, 4)

      val result1 = DB readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result1.size should equal(4)

      val result11 = NamedDB(ConnectionPool.DEFAULT_NAME) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result11.size should equal(4)
      result1.zip(result11).foreach { case (a, b) => a should equal(b) }

      val result2 = NamedDB(ConnectionPool.DEFAULT_NAME)(NoConnectionPoolContext) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result2.size should equal(4)
      result1.zip(result2).foreach { case (a, b) => a should equal(b) }

    } finally {
      dropTable(tableName)
    }
  }

  it should "work with DefaultConnectionPoolContext" in {
    val tableName = tableNamePrefix + "_withDefaultCPContext"
    implicit val context = new MultipleConnectionPoolContext
    context.set(ConnectionPool.DEFAULT_NAME, ConnectionPool())
    try {
      createTable(tableName)
      insertData(tableName, 5)

      val result1 = DB readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result1.size should equal(5)

      val result11 = NamedDB(ConnectionPool.DEFAULT_NAME) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result11.size should equal(5)
      result1.zip(result11).foreach { case (a, b) => a should equal(b) }

      val result2 = NamedDB(ConnectionPool.DEFAULT_NAME)(NoConnectionPoolContext) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result2.size should equal(5)
      result1.zip(result2).foreach { case (a, b) => a should equal(b) }

    } finally {
      dropTable(tableName)
    }
  }

  it should "work with NamedConnectionPoolContext" in {
    val tableName = tableNamePrefix + "_withNamedCPContext"
    implicit val context = new MultipleConnectionPoolContext
    context.set(ConnectionPool.DEFAULT_NAME, ConnectionPool())
    try {
      createTable(tableName)
      insertData(tableName, 6)

      val result1 = DB readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result1.size should equal(6)

      val result11 = NamedDB(ConnectionPool.DEFAULT_NAME) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result11.size should equal(6)
      result1.zip(result11).foreach { case (a, b) => a should equal(b) }

      val result2 = NamedDB('named)(NoConnectionPoolContext) readOnly { implicit s =>
        SQL("select * from " + tableName).map(rs => rs.string("name")).list.apply()
      }
      result2.size should equal(6)
      result1.zip(result2).foreach { case (a, b) => a should equal(b) }

    } finally {
      dropTable(tableName)
    }
  }

}
