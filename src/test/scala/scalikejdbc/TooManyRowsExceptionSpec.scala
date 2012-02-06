package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TooManyRowsExceptionSpec extends FlatSpec with ShouldMatchers {

  behavior of "TooManyRowsException"

  it should "be available" in {
    val expected: Int = 0
    val actual: Int = 0
    val instance = new TooManyRowsException(expected, actual)
    instance should not be null
  }

}
