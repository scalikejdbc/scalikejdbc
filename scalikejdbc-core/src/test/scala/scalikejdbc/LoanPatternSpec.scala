package scalikejdbc

import java.io.Closeable

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

  "using" should "not close the resource immediately when its argument is a future" in {
    class StatefulCloseable extends Closeable {
      @volatile
      var closed: Boolean = false

      def close() = closed = true
    }
    val sc = new StatefulCloseable
    val fResult = using(sc) { conn =>
      Future {
        Thread.sleep(15L)
        3
      }
    }
    sc.closed should be(false)
    whenReady(fResult) { r =>
      r should be(3)
      sc.closed should be(true)
    }
  }

}
