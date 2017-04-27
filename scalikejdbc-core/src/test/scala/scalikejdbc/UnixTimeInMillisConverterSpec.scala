package scalikejdbc

import org.scalatest._
import org.joda.time._

class UnixTimeInMillisConverterSpec extends FlatSpec with Matchers with UnixTimeInMillisConverterImplicits {

  behavior of "UnixTimeInMillisConverter"

  it should "have #toJavaUtilDate" in {
    val d: java.util.Date = new java.util.Date().toJavaUtilDate
    d should not be null
  }

  it should "have #toJodaDateTime" in {
    val d: DateTime = new java.util.Date().toJodaDateTime
    d should not be null
  }

  it should "have #toJodaDateTimeWithTimeZone" in {
    val d: DateTime = new java.util.Date().toJodaDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalDateTime" in {
    val d: LocalDateTime = new java.util.Date().toJodaLocalDateTime
    d should not be null
  }

  it should "have #toJodaLocalDateTimeWithTimeZone" in {
    val d: LocalDateTime = new java.util.Date().toJodaLocalDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalDate" in {
    val d: LocalDate = new java.util.Date().toJodaLocalDate
    d should not be null
  }

  it should "have #toJodaLocalDateWithTimeZone" in {
    val d: LocalDate = new java.util.Date().toJodaLocalDateWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalTime" in {
    val d: LocalTime = new java.util.Date().toJodaLocalTime
    d should not be null
  }

  it should "have #toJodaLocalTimeWithTimeZone" in {
    val d: LocalTime = new java.util.Date().toJodaLocalTimeWithTimeZone(DateTimeZone.UTC)
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
