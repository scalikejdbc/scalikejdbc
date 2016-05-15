package scalikejdbc

import java.io.InputStream
import java.sql.PreparedStatement
import org.mockito.Mockito._
import org.scalatest.FlatSpec
import org.scalatest.mock.MockitoSugar
import scalikejdbc.UnixTimeInMillisConverterImplicits._
import scalikejdbc.interpolation.SQLSyntax

class ParameterBinderFactorySpec extends FlatSpec with MockitoSugar {

  behavior of "ParameterBinderFactory"

  it should "have instance for Long" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Long]].apply(42L)(stmt, 1)
    verify(stmt).setLong(1, 42L)
  }

  it should "have instance for Int" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Int]].apply(42)(stmt, 1)
    verify(stmt).setInt(1, 42)
  }

  it should "have instance for Short" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Short]].apply(42)(stmt, 1)
    verify(stmt).setShort(1, 42)
  }

  it should "have instance for Byte" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Byte]].apply(42)(stmt, 1)
    verify(stmt).setByte(1, 42)
  }

  it should "have instance for Double" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Double]].apply(42d)(stmt, 1)
    verify(stmt).setDouble(1, 42d)
  }

  it should "have instance for Float" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Float]].apply(42f)(stmt, 1)
    verify(stmt).setFloat(1, 42f)
  }

  it should "have instance for Boolean" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Boolean]].apply(true)(stmt, 1)
    verify(stmt).setBoolean(1, true)
  }

  it should "have instance for String" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[String]].apply("foo")(stmt, 1)
    implicitly[ParameterBinderFactory[String]].apply(null)(stmt, 2)
    verify(stmt).setString(1, "foo")
    verify(stmt).setObject(2, null)
  }

  it should "have instance for BigDecimal" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[BigDecimal]].apply(42d)(stmt, 1)
    implicitly[ParameterBinderFactory[BigDecimal]].apply(null)(stmt, 2)
    verify(stmt).setBigDecimal(1, BigDecimal(42d).bigDecimal)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.net.URL" in {
    val stmt = mock[PreparedStatement]
    val value = new java.net.URL("http://www.example.com")
    implicitly[ParameterBinderFactory[java.net.URL]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.net.URL]].apply(null)(stmt, 2)
    verify(stmt).setURL(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Array" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.Array]
    implicitly[ParameterBinderFactory[java.sql.Array]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Array]].apply(null)(stmt, 2)
    verify(stmt).setArray(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.SQLXML" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.SQLXML]
    implicitly[ParameterBinderFactory[java.sql.SQLXML]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.SQLXML]].apply(null)(stmt, 2)
    verify(stmt).setSQLXML(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Date" in {
    val stmt = mock[PreparedStatement]
    val value = java.sql.Date.valueOf("2016-05-15")
    implicitly[ParameterBinderFactory[java.sql.Date]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Date]].apply(null)(stmt, 2)
    verify(stmt).setDate(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Time" in {
    val stmt = mock[PreparedStatement]
    val value = java.sql.Time.valueOf("19:17:42")
    implicitly[ParameterBinderFactory[java.sql.Time]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Time]].apply(null)(stmt, 2)
    verify(stmt).setTime(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Timestamp" in {
    val stmt = mock[PreparedStatement]
    val value = java.sql.Timestamp.valueOf("2016-05-15 19:17:42")
    implicitly[ParameterBinderFactory[java.sql.Timestamp]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Timestamp]].apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.util.Date" in {
    val stmt = mock[PreparedStatement]
    val value = new java.util.Date(42L)
    implicitly[ParameterBinderFactory[java.util.Date]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.util.Date]].apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.toSqlTimestamp)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.DateTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.DateTime.parse("2016-05-15T19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.DateTime]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.DateTime]].apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.toDate.toSqlTimestamp)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalDateTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalDateTime.parse("2016-05-15T19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.LocalDateTime]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalDateTime]].apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.toDate.toSqlTimestamp)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalDate" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalDate.parse("2016-05-15")
    implicitly[ParameterBinderFactory[org.joda.time.LocalDate]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalDate]].apply(null)(stmt, 2)
    verify(stmt).setDate(1, value.toDate.toSqlDate)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for org.joda.time.LocalTime" in {
    val stmt = mock[PreparedStatement]
    val value = org.joda.time.LocalTime.parse("19:17:42")
    implicitly[ParameterBinderFactory[org.joda.time.LocalTime]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[org.joda.time.LocalTime]].apply(null)(stmt, 2)
    verify(stmt).setTime(1, value.toSqlTime)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for InputStream" in {
    val stmt = mock[PreparedStatement]
    val value = mock[InputStream]
    implicitly[ParameterBinderFactory[InputStream]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[InputStream]].apply(null)(stmt, 2)
    verify(stmt).setBinaryStream(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Null" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Null]].apply(null)(stmt, 1)
    verify(stmt).setObject(1, null)
  }

  it should "have instance for None.type" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[None.type]].apply(None)(stmt, 1)
    implicitly[ParameterBinderFactory[None.type]].apply(null)(stmt, 2)
    verify(stmt).setObject(1, null)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for SQLSyntax" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[SQLSyntax]].apply(SQLSyntax.empty)(stmt, 1)
    implicitly[ParameterBinderFactory[SQLSyntax]].apply(null)(stmt, 2)
    verifyNoMoreInteractions(stmt)
  }

  it should "have instance for Option[SQLSyntax]" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Option[SQLSyntax]]].apply(Some(SQLSyntax.empty))(stmt, 1)
    implicitly[ParameterBinderFactory[Option[SQLSyntax]]].apply(None)(stmt, 2)
    // implicitly[ParameterBinderFactory[Option[SQLSyntax]]].apply(null)(stmt, 3)
    verifyNoMoreInteractions(stmt)
  }

  it should "have instance for Option[A]" in {
    val stmt = mock[PreparedStatement]
    type A = String
    implicitly[ParameterBinderFactory[Option[A]]].apply(Some("foo"))(stmt, 1)
    implicitly[ParameterBinderFactory[Option[A]]].apply(None)(stmt, 2)
    implicitly[ParameterBinderFactory[Option[A]]].apply(null)(stmt, 3)
    verify(stmt).setString(1, "foo")
    verify(stmt).setObject(2, null)
    verify(stmt).setObject(3, null)
  }

}
