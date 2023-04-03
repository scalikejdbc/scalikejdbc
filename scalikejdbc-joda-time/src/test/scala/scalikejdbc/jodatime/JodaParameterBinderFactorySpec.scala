package scalikejdbc
package jodatime

import java.sql.PreparedStatement
import org.mockito.Mockito._
import scalikejdbc.jodatime.JodaUnixTimeInMillisConverterImplicits._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import org.scalatest.flatspec.AnyFlatSpec

class JodaParameterBinderFactorySpec extends AnyFlatSpec with MockitoSugar {

  behavior of "ParameterBinderFactory"

  it should "have instance for org.joda.time.DateTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.DateTime.parse("2016-05-15T19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.DateTime]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.DateTime]]
      .apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.toDate.toSqlTimestamp)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalDateTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalDateTime.parse("2016-05-15T19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.LocalDateTime]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalDateTime]]
      .apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.toDate.toSqlTimestamp)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalDate" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalDate.parse("2016-05-15")
    implicitly[ParameterBinderFactory[org.joda.time.LocalDate]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalDate]]
      .apply(null)(stmt, 2)
    verify(stmt).setDate(1, value.toDate.toSqlDate)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalTime.parse("19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.LocalTime]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalTime]]
      .apply(null)(stmt, 2)
    verify(stmt).setTime(1, value.toSqlTime)
    verify(stmt).setObject(2, null)
  }

}
