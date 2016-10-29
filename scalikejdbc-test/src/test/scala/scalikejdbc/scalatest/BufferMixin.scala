package scalikejdbc.scalatest

import org.scalatest.Outcome
import org.scalatest.TestSuite
import org.scalatest.TestSuiteMixin
import scala.collection.mutable.ListBuffer

trait BufferMixin extends TestSuiteMixin { this: TestSuite =>

  val buffer = new ListBuffer[String]

  abstract override def withFixture(test: NoArgTest): Outcome = {
    buffer.append("test")
    try super.withFixture(test)
    finally buffer.clear()
  }
}
