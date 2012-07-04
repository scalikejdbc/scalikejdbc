package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class ErrorMessageSpec extends FlatSpec with ShouldMatchers {

  behavior of "ErrorMessage"

  it should "be available" in {
    ErrorMessage.isInstanceOf[Singleton] should equal(true)
  }

}
