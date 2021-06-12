package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionPoolContextSpec
  extends AnyFlatSpec
  with Matchers
  with Settings {

  behavior of "ConnectionPoolContext"

  class DummyConnectionPoolContext extends ConnectionPoolContext {
    override def set(name: Any, pool: ConnectionPool): Unit =
      throw new RuntimeException
    override def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool =
      null
  }

  it should "be available" in {
    new DummyConnectionPoolContext
  }

  behavior of "NoConnectionPoolContext"

  it should "be available" in {
    intercept[IllegalStateException] {
      NoConnectionPoolContext.set("aaa", null)
    }
    intercept[IllegalStateException] { NoConnectionPoolContext.get("aaa") }
  }

  behavior of "MultipleConnectionPoolContext"

  it should "be available with active tx" in {
    val ctx = MultipleConnectionPoolContext("dummy1" -> null, "dummy2" -> null)
    ctx.set("dummy3", null)
  }

}
