package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionPoolFactorySpec extends AnyFlatSpec with Matchers {

  behavior of "ConnectionPoolFactory"

  class ConnectionPoolFactoryImpl extends ConnectionPoolFactory {
    override def apply(
      url: String,
      user: String,
      password: String,
      settings: ConnectionPoolSettings = ConnectionPoolSettings()
    ): ConnectionPool = throw new RuntimeException
  }

  it should "be available" in {
    val factory = new ConnectionPoolFactoryImpl
    factory should not be null
  }

}
