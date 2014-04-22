package scalikejdbc

import org.scalatest._

class ConnectionPoolFactorySpec extends FlatSpec with Matchers {

  behavior of "ConnectionPoolFactory"

  class ConnectionPoolFactoryImpl extends ConnectionPoolFactory {
    override def apply(url: String, user: String, password: String,
      settings: ConnectionPoolSettings = ConnectionPoolSettings()): ConnectionPool = throw new RuntimeException
  }

  it should "be available" in {
    val factory = new ConnectionPoolFactoryImpl
    factory should not be null
  }

}
