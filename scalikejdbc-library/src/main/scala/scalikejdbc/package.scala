import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Calendar, Date => utilDate }
import org.joda.time._

/**
 * ScalikeJDBC - A thin JDBC wrapper in Scala
 *
 * Just write SQL:
 *
 * This is a thin JDBC wrapper library which just uses java.sql.PreparedStatement internally.
 * Users only need to write SQL and map from java.sql.ResultSet objects to Scala objects.
 * It's pretty simple, really.
 *
 * Basic Usage:
 *
 * Using [[scalikejdbc.DBSession]]:
 *
 * {{{
 * import scalikejdbc._
 * import org.joda.time.DateTime
 * case class User(id: Long, name: String, birthday: Option[DateTime])
 *
 * val activeUsers: List[User] = DB readOnly { session =>
 *   session.list("select * from user where active = ?", true) { rs =>
 *     User(
 *       id = rs.long("id"),
 *       name = rs.string("name"),
 *       birthday = Option(rs.date("birthday")).map(_.toDateTime)
 *     )
 *   }
 * }
 * }}}
 *
 * Using [[scalikejdbc.SQL]]:
 *
 * {{{
 * import scalikejdbc._
 * import org.joda.time.DateTime
 * case class User(id: Long, name: String, birthday: Option[DateTime])
 *
 * val activeUsers: List[User] = DB readOnly { implicit session =>
 *   SQL("select * from user where active = ?").bind(true)
 *     .map { rs => User(
 *       id = rs.long("id"),
 *       name = rs.string("name"),
 *       birthday = Option(rs.date("birthday")).map(_.toDateTime))
 *     }.list.apply()
 * }
 * }}}
 *
 * or
 *
 * {{{
 * val activeUsers: List[User] = DB readOnly { implicit session =>
 *   SQL("select * from user where active = /*'active*/true")
 *     .bindByName('active -> true)
 *     .map { rs => User(
 *       id = rs.long("id"),
 *       name = rs.string("name"),
 *       birthday = Option(rs.date("birthday")).map(_.toDateTime))
 *     }.list.apply()
 * }
 * }}}
 */
package object scalikejdbc {

  // -----
  // enable to use using anywhere

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

  // -----
  // enable implicit conversions for date/time

  /**
   * Unix Time Converter to several types.
   *
   * @param t something has #getTime(): Long
   */
  class UnixTimeInMillisConverter(t: { def getTime(): Long }) {

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

  implicit def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t)

  implicit def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t)

  implicit def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t)

  implicit def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t)

  /**
   * [[org.joda.time.LocalTime]] converter.
   * @param t LocalTime object
   */
  class LocalTimeConverter(t: LocalTime) {

    def toSqlTime: sqlTime = new java.sql.Time(t.toDateTimeToday.getMillis)

    def toSqlTimestamp: sqlTimestamp = new java.sql.Timestamp(t.toDateTimeToday.getMillis)

  }

  implicit def convertLocalTimeToConverter(t: LocalTime): LocalTimeConverter = new LocalTimeConverter(t)

  /**
   * BigDecimal converter.
   * @param bd big decimal value
   */
  class ScalaBigDecimalConverter(bd: java.math.BigDecimal) {

    def toScalaBigDecimal: scala.math.BigDecimal = {
      if (bd == null) null.asInstanceOf[scala.math.BigDecimal]
      else new scala.math.BigDecimal(bd)
    }

  }

  implicit def convertBigDecimal(bd: java.math.BigDecimal): ScalaBigDecimalConverter = {
    new ScalaBigDecimalConverter(bd)
  }

  /**
   * [[scala.Option]] value converter.
   * @param v nullable raw value
   * @tparam A raw type
   * @return optional value
   */
  def opt[A](v: Any): Option[A] = Option(v.asInstanceOf[A])

}
