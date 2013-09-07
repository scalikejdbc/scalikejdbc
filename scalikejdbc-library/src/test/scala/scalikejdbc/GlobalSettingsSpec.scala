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
        } catch { case e: Exception => }
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
        } catch { case e: Exception => }
      }
    }
  }

  it should "fix the issue 22" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table issue22").execute.apply()
        } catch { case e: Exception => }
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
        } catch { case e: Exception => }
      }
    }
  }

  it should "support singleLineMode logging" in {
    GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
      enabled = true,
      singleLineMode = true,
      logLevel = 'ERROR
    )

    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table issue118").execute.apply()
        } catch { case e: Exception => }
        SQL("create table issue118 (id int primary key, created_at timestamp)").execute.apply()
        SQL("insert into issue118 values (?,?)").bind(1, DateTime.now).update.apply()
      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
        try {
          SQL("drop table issue118").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  it should "disable logging SQL errors" in {
    try {
      GlobalSettings.loggingSQLErrors = false
      DB autoCommit { implicit s =>
        SQL("drop table should_not_be_logged").execute.apply()
      }
    } catch { case e: Exception => } finally {
      GlobalSettings.loggingSQLErrors = true
    }
    try {
      DB autoCommit { implicit s =>
        SQL("drop table should_be_logged").execute.apply()
      }
    } catch { case e: Exception => }
  }

  it should "have queryCompletionListener" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table query_completion_listener").execute.apply()
        } catch { case e: Exception => }
        SQL("create table query_completion_listener (id int primary key, created_at timestamp)").execute.apply()
        SQL("insert into query_completion_listener values (?,?)").bind(1, DateTime.now).update.apply()

        var result: String = ""
        GlobalSettings.queryCompletionListener = (sql: String, params: Seq[Any], millis: Long) => {
          result = sql + params + millis
        }
        SQL("select * from query_completion_listener").map(_.toMap).list.apply()
        result.size should be > (0)

        var errorResult: String = ""
        GlobalSettings.queryFailureListener = (sql: String, params: Seq[Any], e: Exception) => {
          errorResult = sql + params + e.getMessage
        }
        try {
          SQL("select * from query_failure_listener").map(_.toMap).list.apply()
        } catch { case e: Exception => }
        errorResult.size should be > (0)

      } finally {
        GlobalSettings.queryCompletionListener = (sql: String, params: Seq[Any], millis: Long) => ()
        GlobalSettings.queryFailureListener = (sql: String, params: Seq[Any], e: Exception) => ()
        try {
          SQL("drop table query_completion_listener").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

}
