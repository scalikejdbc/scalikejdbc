package scalikejdbc

import org.scalatest._

class TooManyRowsExceptionSpec extends FlatSpec with Matchers {

  behavior of "TooManyRowsException"

  it should "be available" in {
    val expected: Int = 0
    val actual: Int = 0
    val instance = new TooManyRowsException(expected, actual)
    instance should not be null
  }

}
