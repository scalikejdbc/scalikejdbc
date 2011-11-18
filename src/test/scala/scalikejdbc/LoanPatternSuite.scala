package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.DriverManager

@RunWith(classOf[JUnitRunner])
class LoanPatternSuite extends FunSuite with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  val url = "jdbc:hsqldb:mem:hsqldb:LoanPatternSuite"
  val user = ""
  val password = ""

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
