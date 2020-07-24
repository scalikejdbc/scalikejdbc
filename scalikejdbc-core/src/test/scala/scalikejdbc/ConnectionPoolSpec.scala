package scalikejdbc

import java.util.Properties
import javax.sql.DataSource
import java.sql.Connection
import scalikejdbc.LoanPattern._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionPoolSpec extends AnyFlatSpec with Matchers {

  behavior of "ConnectionPool"

  val props = new Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))

  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)

  it should "be available" in {
    val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
    ConnectionPool.singleton(url, user, password, poolSettings)

    using(ConnectionPool.borrow()) { conn =>
      conn should not be (null)
    }

    Thread.sleep(100L)
    ConnectionPool.singleton(url, user, password, poolSettings)
    Thread.sleep(100L)
    ConnectionPool.singleton(url, user, password, poolSettings)
    Thread.sleep(100L)
    ConnectionPool.singleton(url, user, password, poolSettings)

    ConnectionPool.add(Symbol("secondary"), url, user, password, poolSettings)

    using(ConnectionPool.borrow(Symbol("secondary"))) { conn =>
      conn should not be (null)
    }

    Thread.sleep(100L)
    ConnectionPool.add(Symbol("secondary"), url, user, password, poolSettings)
    Thread.sleep(100L)
    ConnectionPool.add(Symbol("secondary"), url, user, password, poolSettings)
    Thread.sleep(100L)
    ConnectionPool.add(Symbol("secondary"), url, user, password, poolSettings)

    // this test code affects other tests
    /*
    ConnectionPool.apply(ConnectionPool.DEFAULT_NAME).synchronized {
      // close default connection
      ConnectionPool.close()
      intercept[java.lang.IllegalStateException] { ConnectionPool.borrow() }

      // close secondary connection
      ConnectionPool.close('secondary)
      intercept[java.lang.IllegalStateException] { ConnectionPool.borrow('secondary) }

      // recover for concurrent tests
      ConnectionPool.singleton(url, user, password, poolSettings)
    }
 */
  }

  it should "accept javax.sql.DataSource" in {
    ConnectionPool.add(Symbol("sample"), url, user, password)
    try {
      NamedDB(Symbol("sample")) autoCommit { implicit s =>
        try SQL("create table data_source_test(id bigint not null)").execute.apply()
        catch { case e: Exception => e.printStackTrace }
        SQL("insert into data_source_test values (123)").update.apply()
      }
      val ds = new org.apache.commons.dbcp.BasicDataSource
      ds.setUrl(url)
      ds.setUsername(user)
      ds.setPassword(password)
      ConnectionPool.add(Symbol("ds"), new DataSourceConnectionPool(ds))

      NamedDB(Symbol("ds")) readOnly { implicit s =>
        val count = SQL("select count(1) from data_source_test").map(_.long(1)).single.apply().get
        count should equal(1L)
      }

    } finally {
      NamedDB(Symbol("sample")) autoCommit { implicit s =>
        try SQL("drop table data_source_test").execute.apply()
        catch { case e: Exception => e.printStackTrace }
      }
    }
  }

  it should "be acceptable external ConnectionPoolFactory" in {

    class MyConnectionPool(
      url: String,
      user: String,
      password: String,
      settings: ConnectionPoolSettings = ConnectionPoolSettings())
      extends ConnectionPool(url, user, password, settings) {
      def borrow(): Connection = null
      def dataSource: DataSource = null
    }

    class MyConnectionPoolFactory extends ConnectionPoolFactory {
      def apply(url: String, user: String, password: String, settings: ConnectionPoolSettings) = {
        new MyConnectionPool(url, user, password)
      }
    }

    implicit val factory = new MyConnectionPoolFactory
    ConnectionPool.add(Symbol("xxxx"), url, user, password)
    val conn = ConnectionPool.borrow(Symbol("xxxx"))
    conn should be(null)

  }

  it should "throw exception if invalid connectionPoolFactoryName is given" in {
    intercept[IllegalArgumentException](
      ConnectionPool.add(Symbol("xxxx"), url, user, password,
        ConnectionPoolSettings(connectionPoolFactoryName = "invalid")))
  }

}
