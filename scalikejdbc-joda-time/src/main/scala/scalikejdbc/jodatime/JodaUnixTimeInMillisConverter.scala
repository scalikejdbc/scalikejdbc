package scalikejdbc
package jodatime

import java.time.ZoneId
import JodaUnixTimeInMillisConverter.zoneIdToJodaTimeZone

/**
 * Unix Time Converter to several types.
 *
 * @param millis the milliseconds from 1970-01-01T00:00:00Z
 */
class JodaUnixTimeInMillisConverter(private val millis: Long) extends AnyVal {

  def toJodaDateTime: org.joda.time.DateTime =
    new org.joda.time.DateTime(millis)

  def toJodaDateTimeWithTimeZone(
    timezone: org.joda.time.DateTimeZone
  ): org.joda.time.DateTime = new org.joda.time.DateTime(millis, timezone)

  def toJodaDateTimeWithZoneId(zoneId: ZoneId): org.joda.time.DateTime =
    new org.joda.time.DateTime(millis, zoneIdToJodaTimeZone(zoneId))

  def toJodaLocalDateTime: org.joda.time.LocalDateTime =
    new org.joda.time.LocalDateTime(millis)

  def toJodaLocalDateTimeWithTimeZone(
    timezone: org.joda.time.DateTimeZone
  ): org.joda.time.LocalDateTime =
    new org.joda.time.LocalDateTime(millis, timezone)

  def toJodaLocalDateTimeWithZoneId(
    zoneId: ZoneId
  ): org.joda.time.LocalDateTime =
    new org.joda.time.LocalDateTime(millis, zoneIdToJodaTimeZone(zoneId))

  def toJodaLocalDate: org.joda.time.LocalDate =
    new org.joda.time.LocalDate(millis)

  def toJodaLocalDateWithTimeZone(
    timezone: org.joda.time.DateTimeZone
  ): org.joda.time.LocalDate = new org.joda.time.LocalDate(millis, timezone)

  def toJodaLocalDateWithZoneId(zoneId: ZoneId): org.joda.time.LocalDate =
    new org.joda.time.LocalDate(millis, zoneIdToJodaTimeZone(zoneId))

  def toJodaLocalTime: org.joda.time.LocalTime =
    new org.joda.time.LocalTime(millis)

  def toJodaLocalTimeWithTimeZone(
    timezone: org.joda.time.DateTimeZone
  ): org.joda.time.LocalTime = new org.joda.time.LocalTime(millis, timezone)

  def toJodaLocalTimeWithZoneId(zoneId: ZoneId): org.joda.time.LocalTime =
    new org.joda.time.LocalTime(millis, zoneIdToJodaTimeZone(zoneId))

}

object JodaUnixTimeInMillisConverter {
  def zoneIdToJodaTimeZone(zoneId: ZoneId): org.joda.time.DateTimeZone =
    org.joda.time.DateTimeZone
      .forTimeZone(java.util.TimeZone.getTimeZone(zoneId))
}
