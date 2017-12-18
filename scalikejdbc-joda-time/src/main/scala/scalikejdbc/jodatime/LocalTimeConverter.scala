package scalikejdbc.jodatime

import org.joda.time.LocalTime

/**
 * org.joda.time.LocalTime converter.
 * @param value LocalTime object
 */
class LocalTimeConverter(val value: LocalTime) extends AnyVal {

  def toSqlTime: java.sql.Time = new java.sql.Time(value.toDateTimeToday.getMillis)

  def toSqlTimestamp: java.sql.Timestamp = new java.sql.Timestamp(value.toDateTimeToday.getMillis)

}
