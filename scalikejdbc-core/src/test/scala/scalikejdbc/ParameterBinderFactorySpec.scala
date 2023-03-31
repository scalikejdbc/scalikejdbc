package scalikejdbc

import java.io.InputStream
import java.sql.PreparedStatement
import org.mockito.Mockito._
import scalikejdbc.JavaUtilDateConverterImplicits._
import scalikejdbc.interpolation.SQLSyntax
import org.scalatest.flatspec.AnyFlatSpec

class ParameterBinderFactorySpec extends AnyFlatSpec with MockitoSugar {

  behavior of "ParameterBinderFactory"

  it should "have instance for Long" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Long]].apply(42L)(stmt, 1)
    verify(stmt).setLong(1, 42L)
  }

  it should "have instance for java.lang.Long" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Long]].apply(42L)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Long]].apply(null)(stmt, 2)
    verify(stmt).setLong(1, 42L)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Int" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Int]].apply(42)(stmt, 1)
    verify(stmt).setInt(1, 42)
  }

  it should "have instance for java.lang.Integer" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Integer]].apply(42)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Integer]].apply(null)(stmt, 2)
    verify(stmt).setInt(1, 42)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Short" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Short]].apply(42)(stmt, 1)
    verify(stmt).setShort(1, 42)
  }

  it should "have instance for java.lang.Short" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Short]]
      .apply(42.toShort)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Short]].apply(null)(stmt, 2)
    verify(stmt).setShort(1, 42)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Byte" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Byte]].apply(42)(stmt, 1)
    verify(stmt).setByte(1, 42)
  }

  it should "have instance for java.lang.Byte" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Byte]].apply(42.toByte)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Byte]].apply(null)(stmt, 2)
    verify(stmt).setByte(1, 42)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Double" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Double]].apply(42d)(stmt, 1)
    verify(stmt).setDouble(1, 42d)
  }

  it should "have instance for java.lang.Double" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Double]].apply(42d)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Double]].apply(null)(stmt, 2)
    verify(stmt).setDouble(1, 42d)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Float" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Float]].apply(42f)(stmt, 1)
    verify(stmt).setFloat(1, 42f)
  }

  it should "have instance for java.lang.Float" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Float]].apply(42f)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Float]].apply(null)(stmt, 2)
    verify(stmt).setFloat(1, 42f)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Boolean" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Boolean]].apply(true)(stmt, 1)
    verify(stmt).setBoolean(1, true)
  }

  it should "have instance for java.lang.Boolean" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[java.lang.Boolean]].apply(true)(stmt, 1)
    implicitly[ParameterBinderFactory[java.lang.Boolean]].apply(null)(stmt, 2)
    verify(stmt).setBoolean(1, true)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for String" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[String]].apply("foo")(stmt, 1)
    implicitly[ParameterBinderFactory[String]].apply(null)(stmt, 2)
    verify(stmt).setString(1, "foo")
    verify(stmt).setObject(2, null)
  }

  it should "have instance for BigInt" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[BigInt]].apply(42)(stmt, 1)
    implicitly[ParameterBinderFactory[BigInt]].apply(null)(stmt, 2)
    verify(stmt).setBigDecimal(1, new java.math.BigDecimal(42))
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.math.BigInteger" in {
    val stmt = mock[PreparedStatement]
    val value = java.math.BigInteger.valueOf(42)
    implicitly[ParameterBinderFactory[java.math.BigInteger]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.math.BigInteger]]
      .apply(null)(stmt, 2)
    verify(stmt).setBigDecimal(1, new java.math.BigDecimal(value))
    verify(stmt).setObject(2, null)
  }

  it should "have instance for BigDecimal" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[BigDecimal]].apply(42d)(stmt, 1)
    implicitly[ParameterBinderFactory[BigDecimal]].apply(null)(stmt, 2)
    verify(stmt).setBigDecimal(1, BigDecimal(42d).bigDecimal)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.math.BigDecimal" in {
    val stmt = mock[PreparedStatement]
    val value = new java.math.BigDecimal(42d)
    implicitly[ParameterBinderFactory[java.math.BigDecimal]]
      .apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.math.BigDecimal]]
      .apply(null)(stmt, 2)
    verify(stmt).setBigDecimal(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.net.URL" in {
    val stmt = mock[PreparedStatement]
    val value = new java.net.URI("http://www.example.com").toURL
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

  it should "have instance for InputStream" in {
    val stmt = mock[PreparedStatement]
    val value = mock[InputStream]
    implicitly[ParameterBinderFactory[InputStream]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[InputStream]].apply(null)(stmt, 2)
    verify(stmt).setBinaryStream(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Blob" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.Blob]
    implicitly[ParameterBinderFactory[java.sql.Blob]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Blob]].apply(null)(stmt, 2)
    verify(stmt).setBlob(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Clob" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.Clob]
    implicitly[ParameterBinderFactory[java.sql.Clob]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Clob]].apply(null)(stmt, 2)
    verify(stmt).setClob(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.NClob" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.NClob]
    implicitly[ParameterBinderFactory[java.sql.NClob]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.NClob]].apply(null)(stmt, 2)
    verify(stmt).setNClob(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.Ref" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.Ref]
    implicitly[ParameterBinderFactory[java.sql.Ref]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.Ref]].apply(null)(stmt, 2)
    verify(stmt).setRef(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.sql.RowId" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.sql.RowId]
    implicitly[ParameterBinderFactory[java.sql.RowId]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.sql.RowId]].apply(null)(stmt, 2)
    verify(stmt).setRowId(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for Array[Byte]" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Array[Byte]]]
      .apply(Array[Byte](42, 123))(stmt, 1)
    implicitly[ParameterBinderFactory[Array[Byte]]].apply(null)(stmt, 2)
    verify(stmt).setBytes(1, Array[Byte](42, 123))
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.io.Reader" in {
    val stmt = mock[PreparedStatement]
    val value = mock[java.io.Reader]
    implicitly[ParameterBinderFactory[java.io.Reader]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.io.Reader]].apply(null)(stmt, 2)
    verify(stmt).setCharacterStream(1, value)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for java.util.Calendar" in {
    val stmt = mock[PreparedStatement]
    val value = java.util.Calendar.getInstance()
    implicitly[ParameterBinderFactory[java.util.Calendar]].apply(value)(stmt, 1)
    implicitly[ParameterBinderFactory[java.util.Calendar]].apply(null)(stmt, 2)
    verify(stmt).setTimestamp(1, value.getTime.toSqlTimestamp)
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
    implicitly[ParameterBinderFactory[None.type]]
      .apply(null.asInstanceOf[None.type])(stmt, 2)
    verify(stmt).setObject(1, null)
    verify(stmt).setObject(2, null)
  }

  it should "have instance for SQLSyntax" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[SQLSyntax]]
      .apply(SQLSyntax.empty)(stmt, 1)
    implicitly[ParameterBinderFactory[SQLSyntax]].apply(null)(stmt, 2)
    verifyNoMoreInteractions(stmt)
  }

  it should "have instance for Option[SQLSyntax]" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[Option[SQLSyntax]]]
      .apply(Some(SQLSyntax.empty))(stmt, 1)
    implicitly[ParameterBinderFactory[Option[SQLSyntax]]].apply(None)(stmt, 2)
    implicitly[ParameterBinderFactory[Option[SQLSyntax]]].apply(null)(stmt, 3)
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

  it should "have instance for EnumLike" in {
    val stmt = mock[PreparedStatement]
    implicitly[ParameterBinderFactory[EnumLike]].apply(EnumLike.Foo)(stmt, 1)
    implicitly[ParameterBinderFactory[EnumLike]].apply(EnumLike.Bar)(stmt, 2)
    verify(stmt).setString(1, "Foo")
    verify(stmt).setString(2, "Bar")

    assert(
      SQLSyntax.eq(SQLSyntax.empty, EnumLike.Foo).parameters === Seq(
        EnumLike.Foo
      )
    )
  }
}

sealed trait EnumLike
object EnumLike {

  case object Foo extends EnumLike
  case object Bar extends EnumLike

  implicit def FooEnumParameterBinderFactory[A <: EnumLike]
    : ParameterBinderFactory[A] = {
    ParameterBinderFactory.stringParameterBinderFactory.contramap(_.toString)
  }

}
