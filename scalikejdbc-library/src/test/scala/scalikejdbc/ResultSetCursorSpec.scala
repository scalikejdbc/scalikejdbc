package scalikejdbc

import org.scalatest._

class ResultSetCursorSpec extends FlatSpec with Matchers {

  behavior of "ResultSetCursor"

  it should "be available" in {
    val index: Int = 0
    val instance = new ResultSetCursor(index)
    instance should not be null
  }

}
