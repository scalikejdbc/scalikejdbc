package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import java.sql._
import util.control.Exception._

class AutoSessionSpec extends FlatSpec with ShouldMatchers {

  behavior of "AutoSession"

  it should "be available" in {
    AutoSession
  }

}
