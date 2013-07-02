package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class InvalidColumnNameExceptionSpec extends FlatSpec with ShouldMatchers {

  behavior of "InvalidColumnNameException"

  it should "be available" in {
    val message = "foo"
    val exception = new InvalidColumnNameException(message)
    exception should not be null
  }

}

