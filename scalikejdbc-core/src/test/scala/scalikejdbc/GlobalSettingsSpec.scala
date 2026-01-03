package scalikejdbc

import java.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GlobalSettingsSpec
  extends AnyFlatSpec
  with Matchers
  with Settings
  with LogSupport {

  behavior of "GlobalSettings"

  it should "be available" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table settings_example").execute.apply()
        } catch { case e: Exception => }
        SQL(
          "create table settings_example (id int primary key, name varchar(13) not null)"
        ).execute.apply()
        1 to 20000 foreach { i =>
          GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
          SQL("insert into settings_example values (?,?)")
            .bind(i, "id_%010d".format(i))
            .update
            .apply()
        }
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = "INFO",
          warningThresholdMillis = 10L
        )
        SQL("select  * from settings_example")
          .map(_.int("id"))
          .list
          .apply()
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
        SQL(
          "create table issue22 (id int primary key, created_at timestamp)"
        ).execute.apply()
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          warningEnabled = true,
          warningLogLevel = "INFO",
          warningThresholdMillis = 0L
        )
        SQL("insert into issue22 values (?,?)")
          .bind(1, LocalDateTime.now)
          .update
          .apply()
        SQL("insert into issue22 values (?,?)")
          .bind(2, new java.util.Date)
          .update
          .apply()
        SQL("insert into issue22 values (?,?)")
          .bind(11, Option(LocalDateTime.now))
          .update
          .apply()
        SQL("insert into issue22 values (?,?)")
          .bind(12, Option(new java.util.Date))
          .update
          .apply()
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
      logLevel = "ERROR"
    )

    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table issue118").execute.apply()
        } catch { case e: Exception => }
        SQL(
          "create table issue118 (id int primary key, created_at timestamp)"
        ).execute.apply()
        SQL("insert into issue118 values (?,?)")
          .bind(1, LocalDateTime.now)
          .update
          .apply()
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
    } catch { case e: Exception => }
    finally {
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
        SQL(
          "create table query_completion_listener (id int primary key, created_at timestamp)"
        ).execute.apply()
        SQL("insert into query_completion_listener values (?,?)")
          .bind(1, LocalDateTime.now)
          .update
          .apply()

        var result: String = ""
        GlobalSettings.queryCompletionListener =
          (sql: String, params: collection.Seq[Any], millis: Long) => {
            result = sql + params + millis
          }
        SQL("select * from query_completion_listener")
          .map(_.toMap())
          .list
          .apply()
        result.size should be > 0

        var errorResult: String = ""
        GlobalSettings.queryFailureListener =
          (sql: String, params: collection.Seq[Any], e: Throwable) => {
            errorResult = sql + params + e.getMessage
          }
        try {
          SQL("select * from query_failure_listener")
            .map(_.toMap())
            .list
            .apply()
        } catch { case e: Exception => }
        errorResult.size should be > 0

      } finally {
        GlobalSettings.queryCompletionListener =
          (sql: String, params: collection.Seq[Any], millis: Long) => ()
        GlobalSettings.queryFailureListener =
          (sql: String, params: collection.Seq[Any], e: Throwable) => ()
        try {
          SQL("drop table query_completion_listener").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  it should "have taggedQueryCompletionListener" in {
    DB autoCommit { implicit session =>
      try {
        try {
          SQL("drop table tagged_query_completion_listener")
            .tags("foo", "bar")
            .execute
            .apply()
        } catch { case e: Exception => }

        var result: Int = -1
        GlobalSettings.taggedQueryCompletionListener = (
          sql: String,
          params: collection.Seq[Any],
          millis: Long,
          tags: collection.Seq[String]
        ) => {
          if (result == -1) {
            result = tags.size
          }
        }

        result = -1
        GlobalSettings.taggedQueryCompletionListener.synchronized {
          SQL(
            "create table tagged_query_completion_listener (id int primary key, created_at timestamp)"
          )
            .tags("1", "2", "3", "4")
            .execute
            .apply()
          result should equal(4)
        }

        result = -1
        GlobalSettings.taggedQueryCompletionListener.synchronized {
          SQL("insert into tagged_query_completion_listener values (?,?)")
            .tags("1", "2", "3")
            .bind(1, LocalDateTime.now)
            .update
            .apply()
          result should equal(3)
        }

        result = -1
        GlobalSettings.taggedQueryCompletionListener.synchronized {
          SQL("select * from tagged_query_completion_listener")
            .tags("foo", "bar")
            .map(_.toMap())
            .list
            .apply()
          result should equal(2)
        }

        var errorResult: Int = -1
        GlobalSettings.taggedQueryFailureListener = (
          sql: String,
          params: collection.Seq[Any],
          e: Throwable,
          tags: collection.Seq[String]
        ) => {
          if (errorResult == -1) {
            errorResult = tags.size
          }
        }
        GlobalSettings.taggedQueryFailureListener.synchronized {
          try {
            SQL("select * from tagged_query_failure_listener")
              .tags("foo", "bar", "baz")
              .map(_.toMap())
              .list
              .apply()
          } catch { case e: Exception => }
          errorResult should equal(3)
        }

        // Session scope tagging
        result = -1
        session.tags("foo")
        GlobalSettings.taggedQueryCompletionListener.synchronized {
          SQL("select * from tagged_query_completion_listener")
            .tags("bar", "baz")
            .map(_.toMap())
            .list
            .apply()
          result should equal(3)
        }
        result = -1
        GlobalSettings.taggedQueryCompletionListener.synchronized {
          SQL("select * from tagged_query_completion_listener")
            .map(_.toMap())
            .list
            .apply()
          result should equal(1)
        }

      } finally {
        GlobalSettings.taggedQueryCompletionListener = (
          sql: String,
          params: collection.Seq[Any],
          millis: Long,
          tags: collection.Seq[String]
        ) => ()
        GlobalSettings.taggedQueryFailureListener = (
          sql: String,
          params: collection.Seq[Any],
          e: Throwable,
          tags: collection.Seq[String]
        ) => ()
        try {
          SQL("drop table tagged_query_completion_listener").execute.apply()
        } catch { case e: Exception => }
      }
    }
  }

  it should "have stacktrace logging configuration" in {
    DB autoCommit { implicit session =>
      try {
        try SQL("drop table logging_stacktrace").execute.apply()
        catch { case e: Exception => }

        SQL(
          "create table logging_stacktrace (id int primary key, name varchar(13) not null)"
        ).execute.apply()

        1 to 20000 foreach { i =>
          GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
          SQL("insert into logging_stacktrace values (?,?)")
            .bind(i, "id_%010d".format(i))
            .update
            .apply()
        }
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
          enabled = true,
          logLevel = "WARN",
          printUnprocessedStackTrace = true,
          stackTraceDepth = 500
        )
        SQL("select  * from logging_stacktrace")
          .map(_.int("id"))
          .list
          .apply()

      } finally {
        GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings()
        try SQL("drop table logging_stacktrace").execute.apply()
        catch { case e: Exception => }
      }
    }
  }

}
