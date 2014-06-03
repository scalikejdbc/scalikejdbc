package scalikejdbc

import org.scalatest._

class ErrorMessageSpec extends FlatSpec with Matchers {

  behavior of "ErrorMessage"

  it should "be available" in {
    ErrorMessage.isInstanceOf[Singleton] should equal(true)
  }

}
