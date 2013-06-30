package scalikejdbc

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.mockito.Mockito._

import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Calendar, Date => utilDate }
import org.joda.time._

class UnixTimeInMillisConverterSpec extends FlatSpec with ShouldMatchers {

  import scalikejdbc._

  behavior of "UnixTimeInMillisConverter"

  it should "have #toJavaUtilDate" in {
    val d: java.util.Date = new java.util.Date().toJavaUtilDate
    d should not be null
  }

  it should "have #toDateTime" in {
    val d: DateTime = new java.util.Date().toDateTime
    d should not be null
  }

  it should "have #toDateTimeWithTimeZone" in {
    val d: DateTime = new java.util.Date().toDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toLocalDateTime" in {
    val d: LocalDateTime = new java.util.Date().toLocalDateTime
    d should not be null
  }

  it should "have #toLocalDateTimeWithTimeZone" in {
    val d: LocalDateTime = new java.util.Date().toLocalDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toLocalDate" in {
    val d: LocalDate = new java.util.Date().toLocalDate
    d should not be null
  }

  it should "have #toLocalDateWithTimeZone" in {
    val d: LocalDate = new java.util.Date().toLocalDateWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toLocalTime" in {
    val d: LocalTime = new java.util.Date().toLocalTime
    d should not be null
  }

  it should "have #toLocalTimeWithTimeZone" in {
    val d: LocalTime = new java.util.Date().toLocalTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toSqlDate" in {
    val d: java.sql.Date = new java.util.Date().toSqlDate
    d should not be null
  }

  it should "have #toSqlTime" in {
    val d: java.sql.Time = new java.util.Date().toSqlTime
    d should not be null
  }

  it should "have #toSqlTimestamp" in {
    val d: java.sql.Timestamp = new java.util.Date().toSqlTimestamp
    d should not be null
  }

  it should "have #toSqlTimestamp for LocalTime" in {
    val d: java.sql.Timestamp = LocalTime.now.toSqlTimestamp
    d should not be null
  }

}
