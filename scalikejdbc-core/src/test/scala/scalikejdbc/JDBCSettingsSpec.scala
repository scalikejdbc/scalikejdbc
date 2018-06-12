package scalikejdbc

import org.scalatest._

class JDBCSettingsSpec extends FlatSpec with Matchers {

  behavior of "JDBCSettings"

  it should "be available" in {
    val url: String = "myUrl"
    val user: String = "myUser"
    val password: String = "myPassword123456"
    val driverName: String = "myDriverName"
    val instance = new JDBCSettings(url, user, password, driverName)
    instance should not be null
    instance.toString should be("JDBCSettings(myUrl,myUser,[REDACTED],myDriverName)")
  }

}
