package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.DriverManager

@RunWith(classOf[JUnitRunner])
class LoanPatternSuite extends FunSuite with ShouldMatchers with Settings {

  type ? = this.type // for IntelliJ IDEA

  test("available") {
    LoanPattern.isInstanceOf[Singleton] should equal(true)
  }

  test("using") {
    import LoanPattern._
    val conn = DriverManager.getConnection(url, user, password)
    using(conn) {
      conn => println("do something with " + conn.toString)
    }
  }

}
