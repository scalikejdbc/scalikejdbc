package scalikejdbc

import org.scalatest._

class IllegalRelationshipExceptionSpec extends FlatSpec with Matchers {

  behavior of "IllegalRelationshipException"

  it should "be available" in {
    val message = "foo"
    val exception = new IllegalRelationshipException(message)
    exception should not be null
  }

}

