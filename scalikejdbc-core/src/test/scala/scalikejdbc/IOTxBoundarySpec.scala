package scalikejdbc

import org.scalatest.concurrent.ScalaFutures
import scalikejdbc.iomonads.MyIO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MyIOTxBoundarySpec extends AnyFlatSpec with Matchers with ScalaFutures {

  behavior of "closeConnection()"

  it should "returns Failure when onClose() throws an exception" in {
    val exception = new RuntimeException
    val myIOTxBoundary = implicitly[TxBoundary[MyIO[Int]]]
    val result = myIOTxBoundary.closeConnection(MyIO(1), () => throw exception)
    result.attempt.run() should be(Left(exception))
  }
}
