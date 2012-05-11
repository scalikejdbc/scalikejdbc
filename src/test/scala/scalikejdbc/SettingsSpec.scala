package scalikejdbc

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec

class SettingsSpec extends FlatSpec with ShouldMatchers with Settings {

  behavior of "Settings"

  it should "be available" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table settings_example").execute.apply()
        } catch { case e => }
        SQL("create table settings_example (id int primary key, name varchar(13) not null)")
          .execute.apply()
        1 to 100000 foreach { i =>
          SQL("insert into settings_example values (?,?)").bind(i, "id_%010d".format(i)).update.apply()
        }
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = 'INFO,
          warningThresholdMillis = 10L
        )
        SQL("select  * from settings_example").map(rs => rs.int("id")).list.apply()
      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
        try {
          SQL("drop table settings_example").execute.apply()
        } catch { case e => }
      }
    }
  }

}
