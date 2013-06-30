package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class JDBCSettingsSpec extends FlatSpec with ShouldMatchers {

  behavior of "JDBCSettings"

  it should "be available" in {
    val url: String = ""
    val user: String = ""
    val password: String = ""
    val driverName: String = ""
    val instance = new JDBCSettings(url, user, password, driverName)
    instance should not be null
  }

}
