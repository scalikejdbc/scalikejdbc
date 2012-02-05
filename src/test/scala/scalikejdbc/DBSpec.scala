package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.Connection
import java.lang.IllegalStateException
import scala.util.control.Exception._

@RunWith(classOf[JUnitRunner])
class DBSpec extends FlatSpec with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  "DB" should "be available" in {
    DB.isInstanceOf[Singleton] should equal(true)
  }

}
