package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class IllegalRelationshipExceptionSpec extends AnyFlatSpec with Matchers {

  behavior of "IllegalRelationshipException"

  it should "be available" in {
    val message = "foo"
    val exception = new IllegalRelationshipException(message)
    exception should not be null
  }

}
