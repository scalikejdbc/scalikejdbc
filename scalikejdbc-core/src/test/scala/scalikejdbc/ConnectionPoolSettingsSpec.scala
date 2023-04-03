package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConnectionPoolSettingsSpec extends AnyFlatSpec with Matchers {

  behavior of "ConnectionPoolSettings"

  it should "be available" in {
    val initialSize: Int = 0
    val maxSize: Int = 0
    val connectionTimeoutMillis: Long = 2000L
    val validationQuery: String = ""
    val instance = new ConnectionPoolSettings(
      initialSize,
      maxSize,
      connectionTimeoutMillis,
      validationQuery
    )
    instance should not be null
  }

}
