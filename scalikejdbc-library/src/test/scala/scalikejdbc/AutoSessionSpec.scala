package scalikejdbc

import org.scalatest._

class AutoSessionSpec extends FlatSpec with Matchers {

  behavior of "AutoSession"

  it should "be available" in {
    AutoSession
  }

}
