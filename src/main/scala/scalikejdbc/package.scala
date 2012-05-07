import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Calendar, Date => utilDate }
import org.joda.time._

package object scalikejdbc {

  // -----
  // enable to use using anywhere

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

  // -----
  // enable implicit conversions for date/time

  class TimeInMillis(t: { def getTime(): Long }) {

    def toJavaUtilDate: utilDate = new java.util.Date(t.getTime)

    def toDateTime: DateTime = new DateTime(t.getTime)

    def toDateTimeWithTimeZone(timezone: DateTimeZone): DateTime = new DateTime(t.getTime, timezone)

    def toLocalDateTime: LocalDateTime = new LocalDateTime(t.getTime)

    def toLocalDateTimeWithTimeZone(timezone: DateTimeZone): LocalDateTime = new LocalDateTime(t.getTime, timezone)

    def toLocalDate: LocalDate = new LocalDate(t.getTime)

    def toLocalDateWithTimeZone(timezone: DateTimeZone): LocalDate = new LocalDate(t.getTime, timezone)

    def toLocalTime: LocalTime = new LocalTime(t.getTime)

    def toLocalTimeWithTimeZone(timezone: DateTimeZone): LocalTime = new LocalTime(t.getTime, timezone)

    def toSqlDate: java.sql.Date = {
      // @see http://docs.oracle.com/javase/7/docs/api/java/sql/Date.html
      // -----
      // To conform with the definition of SQL DATE,
      // the millisecond values wrapped by a java.sql.Date instance must be 'normalized'
      // by setting the hours, minutes, seconds, and milliseconds to zero
      // in the particular time zone with which the instance is associated.
      // -----
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(t.getTime)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      new java.sql.Date(cal.getTimeInMillis)
    }

    def toSqlTime: java.sql.Time = new java.sql.Time(t.getTime)

    def toSqlTimestamp: java.sql.Timestamp = new java.sql.Timestamp(t.getTime)

  }

  implicit def convertJavaUtilDate(t: utilDate): TimeInMillis = new TimeInMillis(t)

  implicit def convertJavaSqlDate(t: sqlDate): TimeInMillis = new TimeInMillis(t)

  implicit def convertJavaSqlTime(t: sqlTime): TimeInMillis = new TimeInMillis(t)

  implicit def convertJavaSqlTimestamp(t: sqlTimestamp): TimeInMillis = new TimeInMillis(t)

  class FromLocalTime(t: LocalTime) {

    def toSqlTime: sqlTime = new java.sql.Time(t.toDateTimeToday.getMillis)

    def toSqlTimestamp: sqlTimestamp = new java.sql.Timestamp(t.toDateTimeToday.getMillis)

  }

  implicit def convertLocalTime(t: LocalTime): FromLocalTime = new FromLocalTime(t)

  class ToScalaBigDecimal(bd: java.math.BigDecimal) {

    def toScalaBigDecimal: scala.math.BigDecimal = {
      if (bd == null) null.asInstanceOf[scala.math.BigDecimal]
      else new scala.math.BigDecimal(bd)
    }

  }

  implicit def convertBigDecimal(bd: java.math.BigDecimal): ToScalaBigDecimal = new ToScalaBigDecimal(bd)

}
