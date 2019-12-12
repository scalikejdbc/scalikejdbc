package scalikejdbc

import cats.effect.IO
import org.mockito.Mockito.{ mock, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }
import scalikejdbc.TxBoundary.IO.ioTxBoundary

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IOTxBoundarySpec extends FlatSpec with Matchers with ScalaFutures {

  behavior of "closeConnection()"

  it should "returns Failure when onClose() throws an exception" in {
    val exception = new RuntimeException
    val result = ioTxBoundary[Int].closeConnection(IO(1), () => throw exception)
    result.attempt.unsafeRunSync() should be(Left(exception))
  }
}
