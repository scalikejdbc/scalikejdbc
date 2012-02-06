package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.DriverManager

@RunWith(classOf[JUnitRunner])
class LoanPatternSpec extends FlatSpec with ShouldMatchers with Settings {

  behavior of "LoanPattern"

  it should "be available" in {
    LoanPattern.isInstanceOf[Singleton] should equal(true)
  }

  "using" should "be available" in {
    import LoanPattern._
    val conn = DriverManager.getConnection(url, user, password)
    using(conn) {
      conn => println("do something with " + conn.toString)
    }
  }

}
