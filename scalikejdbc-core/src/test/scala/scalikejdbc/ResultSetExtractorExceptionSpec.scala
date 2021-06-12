package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ResultSetExtractorExceptionSpec extends AnyFlatSpec with Matchers {

  behavior of "ResultSetExtractorException"

  it should "be available" in {
    val message = "foo"
    val e = Some(new RuntimeException)
    val exception = new ResultSetExtractorException(message, e)
    exception should not be null
  }

}
