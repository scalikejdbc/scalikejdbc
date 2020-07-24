package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ResultSetCursorSpec extends AnyFlatSpec with Matchers {

  behavior of "ResultSetCursor"

  it should "be available" in {
    val index: Int = 0
    val instance = new ResultSetCursor(index)
    instance should not be null
  }

}
