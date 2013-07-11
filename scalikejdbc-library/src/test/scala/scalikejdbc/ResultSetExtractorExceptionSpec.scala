package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class ResultSetExtractorExceptionSpec extends FlatSpec with ShouldMatchers {

  behavior of "ResultSetExtractorException"

  it should "be available" in {
    val message = "foo"
    val e = Some(new RuntimeException)
    val exception = new ResultSetExtractorException(message, e)
    exception should not be null
  }

}

