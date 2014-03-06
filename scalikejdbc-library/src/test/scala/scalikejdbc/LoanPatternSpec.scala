package scalikejdbc

import org.scalatest._
import java.sql.DriverManager

class LoanPatternSpec extends FlatSpec with Matchers with Settings {

  behavior of "LoanPattern"

  it should "be available" in {
    LoanPattern.isInstanceOf[Singleton] should equal(true)
  }

  "using" should "be available" in {
    val conn = DriverManager.getConnection(url, user, password)
    using(conn) {
      conn => println("do something with " + conn.toString)
    }
  }

}
