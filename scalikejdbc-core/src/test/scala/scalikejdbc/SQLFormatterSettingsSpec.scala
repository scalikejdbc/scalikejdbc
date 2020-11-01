package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLFormatterSettingsSpec extends AnyFlatSpec with Matchers {

  behavior of "SQLFormatterSettings"

  it should "be available" in {
    val formatterClassName: Option[String] = None
    val instance = new SQLFormatterSettings(formatterClassName)
    instance should not be null
  }

}
