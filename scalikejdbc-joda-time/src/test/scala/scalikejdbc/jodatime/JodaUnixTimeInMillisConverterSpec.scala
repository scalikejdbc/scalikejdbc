package scalikejdbc
package jodatime

import org.joda.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JodaUnixTimeInMillisConverterSpec
  extends AnyFlatSpec
  with Matchers
  with JodaUnixTimeInMillisConverterImplicits {

  behavior of "JodaUnixTimeInMillisConverter"

  it should "have #toJodaDateTime" in {
    val d: DateTime = new java.util.Date().toJodaDateTime
    d should not be null
  }

  it should "have #toJodaDateTimeWithTimeZone" in {
    val d: DateTime =
      new java.util.Date().toJodaDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalDateTime" in {
    val d: LocalDateTime = new java.util.Date().toJodaLocalDateTime
    d should not be null
  }

  it should "have #toJodaLocalDateTimeWithTimeZone" in {
    val d: LocalDateTime =
      new java.util.Date().toJodaLocalDateTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalDate" in {
    val d: LocalDate = new java.util.Date().toJodaLocalDate
    d should not be null
  }

  it should "have #toJodaLocalDateWithTimeZone" in {
    val d: LocalDate =
      new java.util.Date().toJodaLocalDateWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toJodaLocalTime" in {
    val d: LocalTime = new java.util.Date().toJodaLocalTime
    d should not be null
  }

  it should "have #toJodaLocalTimeWithTimeZone" in {
    val d: LocalTime =
      new java.util.Date().toJodaLocalTimeWithTimeZone(DateTimeZone.UTC)
    d should not be null
  }

  it should "have #toSqlTimestamp for joda LocalTime" in {
    val d: java.sql.Timestamp = LocalTime.now.toSqlTimestamp
    d should not be null
  }

}
