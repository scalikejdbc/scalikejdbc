package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j._

@RunWith(classOf[JUnitRunner])
class LogSpec extends FlatSpec with ShouldMatchers {

  behavior of "Log"

  it should "be available" in {
    val logger: Logger = LoggerFactory.getLogger(classOf[LogSpec])
    val instance = new Log(logger)
    instance should not be null
  }

}
