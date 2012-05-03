package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class ConnectionPoolSettingsSpec extends FlatSpec with ShouldMatchers {

  behavior of "ConnectionPoolSettings"

  it should "be available" in {
    val initialSize: Int = 0
    val maxSize: Int = 0
    val validationQuery: String = ""
    val instance = new ConnectionPoolSettings(initialSize, maxSize, validationQuery)
    instance should not be null
  }

}
