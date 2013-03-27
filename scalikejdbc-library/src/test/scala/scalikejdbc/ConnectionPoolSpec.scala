package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import java.util.Properties
import javax.sql.DataSource
import java.sql.Connection

class ConnectionPoolSpec extends FlatSpec with ShouldMatchers {

  behavior of "ConnectionPool"

  val props = new Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))

  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)

  // TODO pending for concurrency
  /*
  it should "be available" in {
    val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
    ConnectionPool.singleton(url, user, password, poolSettings)
    ConnectionPool.borrow() should not be (null)
    ConnectionPool.add('secondary, url, user, password, poolSettings)
    ConnectionPool.borrow('secondary) should not be (null)

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
  }
 */

  it should "be acceptable external ConnectionPoolFactory" in {

    class MyConnectionPool(url: String,
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
    ConnectionPool.add('xxxx, url, user, password)
    val conn = ConnectionPool.borrow('xxxx)
    conn should be(null)

  }

}
