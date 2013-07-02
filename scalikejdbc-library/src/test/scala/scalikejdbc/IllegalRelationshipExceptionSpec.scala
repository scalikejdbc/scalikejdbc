package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class IllegalRelationshipExceptionSpec extends FlatSpec with ShouldMatchers {

  behavior of "IllegalRelationshipException"

  it should "be available" in {
    val message = "foo"
    val exception = new IllegalRelationshipException(message)
    exception should not be null
  }

}

