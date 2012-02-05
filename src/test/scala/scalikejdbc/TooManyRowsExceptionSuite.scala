package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TooManyRowsExceptionSuite extends FunSuite with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  test("available") {
    val expected: Int = 0
    val actual: Int = 0
    val instance = new TooManyRowsException(expected,actual)
    instance should not be null
  }

}
