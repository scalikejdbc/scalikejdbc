package scalikejdbc

import org.mockito.Mockito.{ mock, when }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ FlatSpec, Matchers }
import scalikejdbc.TxBoundary.Try.tryTxBoundary

import scala.util.{ Failure, Success }

class TryTxBoundarySpec extends FlatSpec with Matchers with ScalaFutures {

  behavior of "finishTx()"

  it should "returns Failure when commit() throws an exception" in {
    val commitException = new RuntimeException
    val tx = mock(classOf[Tx])
    when(tx.commit()).thenThrow(commitException)

    val result = tryTxBoundary[Int].finishTx(Success(1), tx)
    result should be(Failure(commitException))
  }

  it should "returns Failure when rollback() throws an exception" in {
    val originException = new RuntimeException
    val rollbackException = new RuntimeException
    val tx = mock(classOf[Tx])
    when(tx.rollback()).thenThrow(rollbackException)

    val result = tryTxBoundary[Int].finishTx(Failure(originException), tx)
    result should be(Failure(originException))
    result.failed.get.getSuppressed should contain(rollbackException)
  }

  behavior of "closeConnection()"

  it should "returns Failure when onClose() throws an exception" in {
    val exception = new RuntimeException
    val result = tryTxBoundary[Int].closeConnection(Success(1), () => throw exception)
    result should be(Failure(exception))
  }
}
