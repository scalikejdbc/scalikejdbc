package scalikejdbc

import java.sql.DriverManager

import org.mockito.Mockito.{ mock, verify }
import org.scalatest.concurrent.ScalaFutures
import org.slf4j.Logger

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LoanPatternSpec
  extends AnyFlatSpec
  with Matchers
  with Settings
  with LoanPattern
  with ScalaFutures {

  implicit val patienceTimeout: PatienceConfig = PatienceConfig(30.seconds)

  behavior of "LoanPattern"

  "using" should "be available" in {
    val conn = DriverManager.getConnection(url, user, password)
    using(conn) { conn =>
      println("do something with " + conn.toString)
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

  class ExceptionResource extends AutoCloseable {
    override def close(): Unit = {
      throw new RuntimeException("test")
    }
  }

  "close" should "throw exceptions" in {
    val mockLogger = mock(classOf[Logger])

    val loadPattern = new LoanPattern {
      override val loanPatternLogger: Logger = mockLogger
    }
    loadPattern.using(new ExceptionResource()) { _ => }
    verify(mockLogger).warn(
      "Failed to close a resource (resource: scalikejdbc.LoanPatternSpec$ExceptionResource error: test)"
    )
  }

}
