package scalikejdbc

import org.scalatest._

class CommonsConnectionPoolFactorySpec extends FlatSpec with Matchers {

  behavior of "CommonsConnectionPoolFactory"

  it should "be available" in {
    CommonsConnectionPoolFactory
  }

}
