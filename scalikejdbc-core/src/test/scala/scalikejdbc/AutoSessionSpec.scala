package scalikejdbc

import org.scalatest._
import java.sql._
import util.control.Exception._

class AutoSessionSpec extends FlatSpec with Matchers {

  behavior of "AutoSession"

  it should "be available" in {
    AutoSession
  }

}
