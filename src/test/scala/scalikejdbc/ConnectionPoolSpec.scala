package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ConnectionPoolSpec extends FlatSpec with ShouldMatchers {

  behavior of "ConnectionPool"

  it should "be available" in {
    ConnectionPool.isInstanceOf[Singleton] should equal(true)
  }

}
