package scalikejdbc

import org.scalatest._

class LogSupportSpec extends FlatSpec with Matchers {

  behavior of "LogSupport"

  it should "be available" in {
    val mixedin = new Object with LogSupport
    mixedin should not be null
  }

}
