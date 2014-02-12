package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class BoneCPConnectionPoolFactorySpec extends FlatSpec with ShouldMatchers {

  behavior of "BoneCPConnectionPoolFactory"

  it should "be available" in {
    BoneCPConnectionPoolFactory
  }

}
