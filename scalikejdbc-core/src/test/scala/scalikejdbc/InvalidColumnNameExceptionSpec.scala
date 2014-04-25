package scalikejdbc

import org.scalatest._

class InvalidColumnNameExceptionSpec extends FlatSpec with Matchers {

  behavior of "InvalidColumnNameException"

  it should "be available" in {
    val message = "foo"
    val exception = new InvalidColumnNameException(message)
    exception should not be null
  }

}

