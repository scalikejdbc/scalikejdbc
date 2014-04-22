package scalikejdbc

import org.scalatest._

class JDBCSettingsSpec extends FlatSpec with Matchers {

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
