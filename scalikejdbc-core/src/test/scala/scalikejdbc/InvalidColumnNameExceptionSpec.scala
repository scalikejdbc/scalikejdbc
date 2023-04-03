package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class InvalidColumnNameExceptionSpec extends AnyFlatSpec with Matchers {

  behavior of "InvalidColumnNameException"

  it should "be available" in {
    val message = "foo"
    val exception = new InvalidColumnNameException(message)
    exception should not be null
  }

}
