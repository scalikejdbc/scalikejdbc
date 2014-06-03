/*
 * Copyright 2011 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
import java.sql.{ Timestamp => sqlTimestamp, Time => sqlTime, Date => sqlDate }
import java.util.{ Calendar, Date => utilDate }
import org.joda.time._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * ScalikeJDBC - SQL-Based DB Access Library for Scala
 *
 * Just write SQL:
 *
 * ScalikeJDBC is a SQL-based DB access library for Scala developers.
 * This library naturally wraps JDBC APIs and provides you easy-to-use APIs.
 * Users do nothing other than writing SQL and mapping from java.sql.ResultSet objects to Scala values.
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
 *   session.list("select * from users where active = ?", true) { rs =>
 *     User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toDateTime))
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
 *   SQL("select * from users where active = ?")
 *     .bind(true)
 *     .map { rs => User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toDateTime)) }.list.apply()
 * }
 * }}}
 *
 * or
 *
 * {{{
 * val activeUsers: List[User] = DB readOnly { implicit session =>
 *   SQL("select * from users where active = /*'active*/true")
 *     .bindByName('active -> true)
 *     .map { rs => User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toDateTime)) }.list.apply()
 * }
 * }}}
 */
package object scalikejdbc {

  // -----
  // enable to use using anywhere

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)
  def futureUsing[R <: Closable, A](resource: R)(f: R => Future[A]): Future[A] = LoanPattern.futureUsing(resource)(f)

  // -----
  // enable implicit conversions for date/time

  /**
   * Unix Time Converter to several types.
   *
   * @param ms the milliseconds from 1970-01-01T00:00:00Z
   */
  class UnixTimeInMillisConverter(ms: Long) {

    def toJavaUtilDate: utilDate = new java.util.Date(ms)

    def toDateTime: DateTime = new DateTime(ms)

    def toDateTimeWithTimeZone(timezone: DateTimeZone): DateTime = new DateTime(ms, timezone)

    def toLocalDateTime: LocalDateTime = new LocalDateTime(ms)

    def toLocalDateTimeWithTimeZone(timezone: DateTimeZone): LocalDateTime = new LocalDateTime(ms, timezone)

    def toLocalDate: LocalDate = new LocalDate(ms)

    def toLocalDateWithTimeZone(timezone: DateTimeZone): LocalDate = new LocalDate(ms, timezone)

    def toLocalTime: LocalTime = new LocalTime(ms)

    def toLocalTimeWithTimeZone(timezone: DateTimeZone): LocalTime = new LocalTime(ms, timezone)

    def toSqlDate: java.sql.Date = {
      // @see http://docs.oracle.com/javase/7/docs/api/java/sql/Date.html
      // -----
      // To conform with the definition of SQL DATE,
      // the millisecond values wrapped by a java.sql.Date instance must be 'normalized'
      // by setting the hours, minutes, seconds, and milliseconds to zero
      // in the particular time zone with which the instance is associated.
      // -----
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(ms)
      cal.set(Calendar.HOUR_OF_DAY, 0)
      cal.set(Calendar.MINUTE, 0)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)
      new java.sql.Date(cal.getTimeInMillis)
    }

    def toSqlTime: java.sql.Time = new java.sql.Time(ms)

    def toSqlTimestamp: java.sql.Timestamp = new java.sql.Timestamp(ms)

  }

  implicit def convertJavaUtilDateToConverter(t: utilDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlDateToConverter(t: sqlDate): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimeToConverter(t: sqlTime): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  implicit def convertJavaSqlTimestampToConverter(t: sqlTimestamp): UnixTimeInMillisConverter = new UnixTimeInMillisConverter(t.getTime)

  /**
   * org.joda.time.LocalTime converter.
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
   * Option value converter.
   * @param v nullable raw value
   * @tparam A raw type
   * @return optional value
   */
  def opt[A](v: Any): Option[A] = Option(v.asInstanceOf[A])

  @deprecated(message = "ExecutableSQLParser renamed ifself SQLTemplateParser", since = "1.4.0")
  val ExecutableSQLParser = SQLTemplateParser

}
