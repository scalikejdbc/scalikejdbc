package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class LogSupportSpec extends FlatSpec with ShouldMatchers {

  behavior of "LogSupport"

  it should "be available" in {
    val mixedin = new Object with LogSupport
    mixedin should not be null
  }

}
