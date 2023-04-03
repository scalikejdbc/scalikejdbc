package scalikejdbc

import org.joda.time.{ DateTime, LocalDate, LocalDateTime, LocalTime }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StatementExecutorSpec extends AnyFlatSpec with Matchers {
  it should "print sql DateTime" in {
    val sql = StatementExecutor.PrintableQueryBuilder.build(
      template = "select * from users where created_at between ? and ?",
      settingsProvider = SettingsProvider.default,
      params = Seq(DateTime.parse("2020-01-01"), DateTime.parse("2020-01-31"))
    )
    sql should equal(
      "select * from users where created_at between '2020-01-01 00:00:00.0' and '2020-01-31 00:00:00.0'"
    )
  }

  it should "print sql LocalDateTime" in {
    val sql = StatementExecutor.PrintableQueryBuilder.build(
      template = "select * from users where created_at between ? and ?",
      settingsProvider = SettingsProvider.default,
      params = Seq(
        LocalDateTime.parse("2020-01-01"),
        LocalDateTime.parse("2020-01-31")
      )
    )
    sql should equal(
      "select * from users where created_at between '2020-01-01 00:00:00.0' and '2020-01-31 00:00:00.0'"
    )
  }

  it should "print sql LocalDate" in {
    val sql = StatementExecutor.PrintableQueryBuilder.build(
      template = "select * from users where created_at between ? and ?",
      settingsProvider = SettingsProvider.default,
      params = Seq(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-31"))
    )
    sql should equal(
      "select * from users where created_at between '2020-01-01' and '2020-01-31'"
    )
  }

  it should "print sql LocalTime" in {
    val sql = StatementExecutor.PrintableQueryBuilder.build(
      template = "select * from users where created_at between ? and ?",
      settingsProvider = SettingsProvider.default,
      params = Seq(LocalTime.parse("00:00"), LocalTime.parse("23:00"))
    )
    sql should equal(
      "select * from users where created_at between '00:00:00' and '23:00:00'"
    )
  }
}
