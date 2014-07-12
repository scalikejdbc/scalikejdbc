package scalikejdbc

import org.scalatest._
import java.sql.DriverManager
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.{ Future, ExecutionContext }
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

class LoanPatternSpec extends FlatSpec with Matchers with Settings with LoanPattern with ScalaFutures {

  implicit val patienceTimeout = PatienceConfig(30.seconds)

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

  "futureUsing" should "be available" in {
    val conn = DriverManager.getConnection(url, user, password)
    val fResult = futureUsing(conn) { conn =>
      Future.successful(3)
    }
    whenReady(fResult) { r =>
      r should be(3)
    }
  }

}
