package scalikejdbc.scalatest

import org.scalatest.FutureOutcome
import org.scalatest.AsyncTestSuite
import org.scalatest.AsyncTestSuiteMixin
import scala.collection.mutable.ListBuffer

trait AsyncBufferMixin extends AsyncTestSuiteMixin { this: AsyncTestSuite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgAsyncTest): FutureOutcome = {
    buffer.append("test")
    super.withFixture(test).onCompletedThen { _ =>
      buffer.clear()
    }
  }
}
