package scalikejdbc

import java.util.Calendar

/**
 * Unix Time Converter to several types.
 *
 * @param millis the milliseconds from 1970-01-01T00:00:00Z
 */
class UnixTimeInMillisConverter(val millis: Long) extends AnyVal {

  // --------------------
  // java.util.Date
  // --------------------

  def toJavaUtilDate: java.util.Date = new java.util.Date(millis)

  // --------------------
  // java.time
  // --------------------

  private def defaultZoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()

  def toInstant: java.time.Instant = java.time.Instant.ofEpochMilli(millis)

  def toZonedDateTimeWithZoneId(zoneId: java.time.ZoneId): java.time.ZonedDateTime = java.time.ZonedDateTime.ofInstant(toInstant, zoneId)

  def toZonedDateTime: java.time.ZonedDateTime = java.time.ZonedDateTime.ofInstant(toInstant, defaultZoneId)

  def toOffsetDateTimeWithZoneId(zoneId: java.time.ZoneId): java.time.OffsetDateTime = java.time.OffsetDateTime.ofInstant(toInstant, zoneId)

  def toOffsetDateTime: java.time.OffsetDateTime = java.time.OffsetDateTime.ofInstant(toInstant, defaultZoneId)

  def toLocalDateWithZoneId(zoneId: java.time.ZoneId): java.time.LocalDate = toInstant.atZone(zoneId).toLocalDate

  def toLocalDate: java.time.LocalDate = toInstant.atZone(defaultZoneId).toLocalDate

  def toLocalTimeWithZoneId(zoneId: java.time.ZoneId): java.time.LocalTime = toInstant.atZone(zoneId).toLocalTime

  def toLocalTime: java.time.LocalTime = toInstant.atZone(defaultZoneId).toLocalTime

  def toLocalDateTimeWithZoneId(zoneId: java.time.ZoneId): java.time.LocalDateTime = toInstant.atZone(zoneId).toLocalDateTime

  def toLocalDateTime: java.time.LocalDateTime = toInstant.atZone(defaultZoneId).toLocalDateTime

  // --------------------
  // java.sql
  // --------------------

  def toSqlDate: java.sql.Date = {
    // @see http://docs.oracle.com/javase/7/docs/api/java/sql/Date.html
    // -----
    // To conform with the definition of SQL DATE,
    // the millisecond values wrapped by a java.sql.Date instance must be 'normalized'
    // by setting the hours, minutes, seconds, and milliseconds to zero
    // in the particular time zone with which the instance is associated.
    // -----
    val cal = Calendar.getInstance()
    cal.setTimeInMillis(millis)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    new java.sql.Date(cal.getTimeInMillis)
  }

  def toSqlTime: java.sql.Time = new java.sql.Time(millis)

  def toSqlTimestamp: java.sql.Timestamp = new java.sql.Timestamp(millis)

}
