package scalikejdbc

import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CommonsConnectionPoolFactorySpec extends AnyFlatSpec with Matchers {

  behavior of "CommonsConnectionPoolFactory"

  it should "be available" in {
    CommonsConnectionPoolFactory
  }

}
