package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class SQLFormatterSettingsSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLFormatterSettings"

  it should "be available" in {
    val formatterClassName: Option[String] = None
    val instance = new SQLFormatterSettings(formatterClassName)
    instance should not be null
  }

}
