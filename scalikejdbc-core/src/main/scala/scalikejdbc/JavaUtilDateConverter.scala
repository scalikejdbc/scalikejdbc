package scalikejdbc

import java.util.Calendar

/**
 * `java.util.Date` Converter to several types.
 */
class JavaUtilDateConverter(private val value: java.util.Date) extends AnyVal {
  private[this] def millis: Long = value.getTime

  // --------------------
  // java.util.Date
  // --------------------

  def toJavaUtilDate: java.util.Date = value

  // --------------------
  // java.time
  // --------------------

  private def defaultZoneId: java.time.ZoneId = java.time.ZoneId.systemDefault()

  def toInstant: java.time.Instant = {
    value match {
      case t: java.sql.Timestamp =>
        t.toInstant
      case _ =>
        java.time.Instant.ofEpochMilli(millis)
    }
  }

  def toZonedDateTimeWithZoneId(
    zoneId: java.time.ZoneId
  ): java.time.ZonedDateTime =
    java.time.ZonedDateTime.ofInstant(toInstant, zoneId)

  def toZonedDateTime: java.time.ZonedDateTime =
    java.time.ZonedDateTime.ofInstant(toInstant, defaultZoneId)

  def toOffsetDateTimeWithZoneId(
    zoneId: java.time.ZoneId
  ): java.time.OffsetDateTime =
    java.time.OffsetDateTime.ofInstant(toInstant, zoneId)

  def toOffsetDateTime: java.time.OffsetDateTime =
    java.time.OffsetDateTime.ofInstant(toInstant, defaultZoneId)

  def toLocalDateWithZoneId(zoneId: java.time.ZoneId): java.time.LocalDate =
    toInstant.atZone(zoneId).toLocalDate

  def toLocalDate: java.time.LocalDate =
    toInstant.atZone(defaultZoneId).toLocalDate

  def toLocalTimeWithZoneId(zoneId: java.time.ZoneId): java.time.LocalTime =
    toInstant.atZone(zoneId).toLocalTime

  def toLocalTime: java.time.LocalTime =
    toInstant.atZone(defaultZoneId).toLocalTime

  def toLocalDateTimeWithZoneId(
    zoneId: java.time.ZoneId
  ): java.time.LocalDateTime = toInstant.atZone(zoneId).toLocalDateTime

  def toLocalDateTime: java.time.LocalDateTime =
    toInstant.atZone(defaultZoneId).toLocalDateTime

  // --------------------
  // java.sql
  // --------------------

  def toSqlDate: java.sql.Date = {
    value match {
      case t: java.sql.Date =>
        t
      case _ =>
        // @see https://docs.oracle.com/javase/8/docs/api/java/sql/Date.html
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
  }

  def toSqlTime: java.sql.Time = {
    value match {
      case t: java.sql.Time =>
        t
      case _ =>
        new java.sql.Time(millis)
    }
  }

  def toSqlTimestamp: java.sql.Timestamp = {
    value match {
      case t: java.sql.Timestamp =>
        t
      case _ =>
        new java.sql.Timestamp(millis)
    }
  }

}
