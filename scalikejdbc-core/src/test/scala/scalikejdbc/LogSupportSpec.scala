package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LogSupportSpec extends AnyFlatSpec with Matchers {

  behavior of "LogSupport"

  it should "be available" in {
    val mixedin = new Object with LogSupport
    mixedin should not be null
  }

}
