package scalikejdbc

import org.slf4j._
import org.mockito.Mockito.{ mock, verify, times, when }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LogSpec extends AnyFlatSpec with Matchers {

  behavior of "Log"

  it should "be available" in {
    val logger: Logger = LoggerFactory.getLogger(classOf[LogSpec])
    val instance = new Log(logger)
    instance should not be null
  }

  it should "have withLevel methods" in {
    val ex = new RuntimeException("This is a sample error")

    val logger: Logger = mock(classOf[Logger])
    when(logger.isDebugEnabled()).thenReturn(true)

    val log = new Log(logger)
    log.isDebugEnabled = true

    log.withLevel("debug")("Hi")
    log.withLevel("debug")("Hi Hi", ex)

    verify(logger, times(1)).debug("Hi")
    verify(logger, times(1)).debug("Hi Hi", ex)
  }

}
