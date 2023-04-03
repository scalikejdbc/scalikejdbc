package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AutoSessionSpec extends AnyFlatSpec with Matchers {

  behavior of "AutoSession"

  it should "be available" in {
    AutoSession
  }

}
