package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

case class Foo(a: Int, b: Option[String])

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers {

  import scalikejdbc.SQLInterpolationMacro._

  behavior of "SQLInterpolationMacro"

  it should "work fine" in {
    validateField[Foo]("a")
    validateField[Foo]("b")
    //validateField[Foo]("c")
  }

}
