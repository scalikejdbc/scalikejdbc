package scalikejdbc

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.joda.time.DateTime

class GlobalSettingsSpec extends FlatSpec with ShouldMatchers with Settings {

  behavior of "GlobalSettings"

  it should "be available" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table settings_example").execute.apply()
        } catch { case e => }
        SQL("create table settings_example (id int primary key, name varchar(13) not null)")
          .execute.apply()
        1 to 20000 foreach { i =>
          GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
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

  it should "fix the issue 22" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table issue22").execute.apply()
        } catch { case e => }
        SQL("create table issue22 (id int primary key, created_at timestamp)").execute.apply()
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = 'INFO,
          warningThresholdMillis = 0L
        )
        SQL("insert into issue22 values (?,?)").bind(1, DateTime.now).update.apply()
        SQL("insert into issue22 values (?,?)").bind(2, new java.util.Date).update.apply()
        SQL("insert into issue22 values (?,?)").bind(11, Option(DateTime.now)).update.apply()
        SQL("insert into issue22 values (?,?)").bind(12, Option(new java.util.Date)).update.apply()
      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
        try {
          SQL("drop table issue22").execute.apply()
        } catch { case e => }
      }
    }
  }

}
