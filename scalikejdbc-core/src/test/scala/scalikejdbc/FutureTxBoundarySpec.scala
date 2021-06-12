package scalikejdbc

import org.mockito.Mockito.{ mock, when }
import org.scalatest.concurrent.ScalaFutures
import scalikejdbc.TxBoundary.Future.futureTxBoundary

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class FutureTxBoundarySpec extends AnyFlatSpec with Matchers with ScalaFutures {

  behavior of "finishTx()"

  it should "returns Failure when commit() throws an exception" in {
    val commitException = new RuntimeException
    val tx = mock(classOf[Tx])
    when(tx.commit()).thenThrow(commitException)

    val result = futureTxBoundary[Int].finishTx(Future.successful(1), tx)
    result.failed.futureValue should be(commitException)
  }

  it should "returns Failure when rollback() throws an exception" in {
    val originException = new RuntimeException
    val rollbackException = new RuntimeException
    val tx = mock(classOf[Tx])
    when(tx.rollback()).thenThrow(rollbackException)

    val result =
      futureTxBoundary[Int].finishTx(Future.failed(originException), tx)
    result.failed.futureValue should be(originException)
    result.failed.futureValue.getSuppressed should contain(rollbackException)
  }

  behavior of "closeConnection()"

  it should "returns Failure when onClose() throws an exception" in {
    val exception = new RuntimeException
    val result = futureTxBoundary[Int].closeConnection(
      Future.successful(1),
      () => throw exception
    )
    result.failed.futureValue should be(exception)
  }
}
