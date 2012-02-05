package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j.LoggerFactory

@RunWith(classOf[JUnitRunner])
class LogSupportSuite extends FunSuite with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  test("available") {
    val mixedin = new Object with LogSupport
    mixedin should not be null
  }

}
