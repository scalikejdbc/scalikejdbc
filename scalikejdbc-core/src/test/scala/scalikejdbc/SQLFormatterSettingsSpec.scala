package scalikejdbc

import org.scalatest._

class SQLFormatterSettingsSpec extends FlatSpec with Matchers {

  behavior of "SQLFormatterSettings"

  it should "be available" in {
    val formatterClassName: Option[String] = None
    val instance = new SQLFormatterSettings(formatterClassName)
    instance should not be null
  }

}
