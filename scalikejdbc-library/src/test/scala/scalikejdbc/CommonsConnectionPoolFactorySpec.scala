package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class CommonsConnectionPoolFactorySpec extends FlatSpec with ShouldMatchers {

  behavior of "CommonsConnectionPoolFactory"

  it should "be available" in {
    CommonsConnectionPoolFactory
  }

}
