package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j._

@RunWith(classOf[JUnitRunner])
class LogSuite extends FunSuite with ShouldMatchers {

  test("available") {
    val logger: Logger = LoggerFactory.getLogger(classOf[LogSuite])
    val instance = new Log(logger)
    instance should not be null
  }

}
