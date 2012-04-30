package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ResultSetCursorSpec extends FlatSpec with ShouldMatchers {

  behavior of "ResultSetCursor"

  it should "be available" in {
    val index: Int = 0
    val instance = new ResultSetCursor(index)
    instance should not be null
  }

}
