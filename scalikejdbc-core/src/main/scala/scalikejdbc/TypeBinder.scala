package scalikejdbc

import java.sql.ResultSet
import org.joda.time.{ DateTime => JodaDateTime }
import org.joda.time.{ LocalDate => JodaLocalDate }
import org.joda.time.{ LocalTime => JodaLocalTime }
import org.joda.time.{ LocalDateTime => JodaLocalDateTime }

/**
 * Type binder for java.sql.ResultSet.
 */
trait TypeBinder[+A] {
  def apply(rs: ResultSet, columnIndex: Int): A
  def apply(rs: ResultSet, columnLabel: String): A
  def map[B](f: A => B): TypeBinder[B] = new TypeBinder[B] {
    def apply(rs: ResultSet, columnIndex: Int): B = f(TypeBinder.this.apply(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): B = f(TypeBinder.this.apply(rs, columnLabel))
  }
}

/**
 * Type binder for java.sql.ResultSet.
 */
object TypeBinder extends LowPriorityTypeBinderImplicits with UnixTimeInMillisConverterImplicits {
  import java.util.TimeZone
  import GlobalSettings.serverTimeZone

  private[this] def timeZoneConverter =
    TimeZoneConverter.from(serverTimeZone.getOrElse(TimeZone.getDefault)).to(TimeZone.getDefault)

  private[this] def timeZoneEnabled = serverTimeZone.isDefined

  def apply[A](index: (ResultSet, Int) => A)(label: (ResultSet, String) => A): TypeBinder[A] = new TypeBinder[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs, columnLabel)
  }

  private[scalikejdbc] val any: TypeBinder[Any] = TypeBinder(_ getObject _)(_ getObject _)
  implicit val array: TypeBinder[java.sql.Array] = TypeBinder(_ getArray _)(_ getArray _)

  implicit val bigDecimal: TypeBinder[java.math.BigDecimal] = TypeBinder(_ getBigDecimal _)(_ getBigDecimal _)
  implicit val scalaBigDecimal: TypeBinder[BigDecimal] = option(bigDecimal).map(_.map(BigDecimal.apply).orNull[BigDecimal])

  implicit val binaryStream: TypeBinder[java.io.InputStream] = TypeBinder(_ getBinaryStream _)(_ getBinaryStream _)
  implicit val blob: TypeBinder[java.sql.Blob] = TypeBinder(_ getBlob _)(_ getBlob _)
  implicit val nullableBoolean: TypeBinder[java.lang.Boolean] = any.map {
    case b if b == null => b.asInstanceOf[java.lang.Boolean]
    case b: java.lang.Boolean => b
    case b: Boolean => b.asInstanceOf[java.lang.Boolean]
    case s: String => {
      try s.toInt != 0
      catch { case e: NumberFormatException => !s.isEmpty }
    }.asInstanceOf[java.lang.Boolean]
    case n: Number => (n.intValue() != 0).asInstanceOf[java.lang.Boolean]
    case v => (v != 0).asInstanceOf[java.lang.Boolean]
  }
  implicit val boolean: TypeBinder[Boolean] = nullableBoolean.map(throwExceptionIfNull(_.asInstanceOf[Boolean]))
  implicit val optionBoolean: TypeBinder[Option[Boolean]] = nullableBoolean.map(v => Option(v).map(_.asInstanceOf[Boolean]))
  implicit val nullableByte: TypeBinder[java.lang.Byte] = any.map(v => if (v == null) null else java.lang.Byte.valueOf(v.toString))
  implicit val byte: TypeBinder[Byte] = nullableByte.map(throwExceptionIfNull(_.asInstanceOf[Byte]))
  implicit val optionByte: TypeBinder[Option[Byte]] = nullableByte.map(v => Option(v).map(_.asInstanceOf[Byte]))
  implicit val bytes: TypeBinder[Array[Byte]] = TypeBinder(_ getBytes _)(_ getBytes _)
  implicit val characterStream: TypeBinder[java.io.Reader] = TypeBinder(_ getCharacterStream _)(_ getCharacterStream _)
  implicit val clob: TypeBinder[java.sql.Clob] = TypeBinder(_ getClob _)(_ getClob _)
  implicit val date: TypeBinder[java.sql.Date] = TypeBinder { (rs, i) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getDate(i)) else rs.getDate(i)
  } { (rs, s) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getDate(s)) else rs.getDate(s)
  }
  implicit val nullableDouble: TypeBinder[java.lang.Double] = any.map(v => if (v == null) null else java.lang.Double.valueOf(v.toString))
  implicit val double: TypeBinder[Double] = nullableDouble.map(throwExceptionIfNull(_.asInstanceOf[Double]))
  implicit val optionDouble: TypeBinder[Option[Double]] = nullableDouble.map(v => Option(v).map(_.asInstanceOf[Double]))
  implicit val nullableFloat: TypeBinder[java.lang.Float] = any.map(v => if (v == null) null else java.lang.Float.valueOf(v.toString))
  implicit val float: TypeBinder[Float] = nullableFloat.map(throwExceptionIfNull(_.asInstanceOf[Float]))
  implicit val optionFloat: TypeBinder[Option[Float]] = nullableFloat.map(v => Option(v).map(_.asInstanceOf[Float]))
  implicit val nullableInt: TypeBinder[java.lang.Integer] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Integer]
    case v: Float => v.toInt.asInstanceOf[java.lang.Integer]
    case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
    case n: Number => n.intValue()
    case v => java.lang.Integer.valueOf(v.toString)
  }
  implicit val int: TypeBinder[Int] = nullableInt.map(throwExceptionIfNull(_.asInstanceOf[Int]))
  implicit val optionInt: TypeBinder[Option[Int]] = nullableInt.map(v => Option(v).map(_.asInstanceOf[Int]))
  implicit val nullableLong: TypeBinder[java.lang.Long] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Long]
    case v: Float => v.toLong.asInstanceOf[java.lang.Long]
    case v: Double => v.toLong.asInstanceOf[java.lang.Long]
    case n: Number => n.longValue()
    case v => java.lang.Long.valueOf(v.toString)
  }
  implicit val long: TypeBinder[Long] = nullableLong.map(throwExceptionIfNull(_.asInstanceOf[Long]))
  implicit val optionLong: TypeBinder[Option[Long]] = nullableLong.map(v => Option(v).map(_.asInstanceOf[Long]))
  implicit val nClob: TypeBinder[java.sql.NClob] = TypeBinder(_ getNClob _)(_ getNClob _)
  implicit val ref: TypeBinder[java.sql.Ref] = TypeBinder(_ getRef _)(_ getRef _)
  implicit val rowId: TypeBinder[java.sql.RowId] = TypeBinder(_ getRowId _)(_ getRowId _)
  implicit val nullableShort: TypeBinder[java.lang.Short] = any.map {
    case v if v == null => v.asInstanceOf[java.lang.Short]
    case v: Float => v.toShort.asInstanceOf[java.lang.Short]
    case v: Double => v.toShort.asInstanceOf[java.lang.Short]
    case n: Number => n.shortValue()
    case v => java.lang.Short.valueOf(v.toString)
  }
  implicit val short: TypeBinder[Short] = nullableShort.map(throwExceptionIfNull(_.asInstanceOf[Short]))
  implicit val optionShort: TypeBinder[Option[Short]] = nullableShort.map(v => Option(v).map(_.asInstanceOf[Short]))
  implicit val sqlXml: TypeBinder[java.sql.SQLXML] = TypeBinder(_ getSQLXML _)(_ getSQLXML _)

  implicit val string: TypeBinder[String] = TypeBinder(_ getString _)(_ getString _)

  implicit val time: TypeBinder[java.sql.Time] = TypeBinder { (rs, i) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getTime(i)) else rs.getTime(i)
  } { (rs, s) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getTime(s)) else rs.getTime(s)
  }
  implicit val timestamp: TypeBinder[java.sql.Timestamp] = TypeBinder { (rs, i) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getTimestamp(i)) else rs.getTimestamp(i)
  } { (rs, s) =>
    if (timeZoneEnabled) timeZoneConverter.convert(rs.getTimestamp(s)) else rs.getTimestamp(s)
  }

  /*
   * [error] /scalikejdbc/scalikejdbc-library/src/test/scala/scalikejdbc/TypeBinderSpec.scala:40: ambiguous
   * implicit values:
   * [error]  both value date in object TypeBinder of type => scalikejdbc.TypeBinder[java.sql.Date]
   * [error]  and value time in object TypeBinder of type => scalikejdbc.TypeBinder[java.sql.Time]
   * [error]  match expected type scalikejdbc.TypeBinder[java.util.Date]
   * [error]     implicitly[TypeBinder[java.util.Date]].apply(rs, "time") should not be (null)
   * [error]               ^
   * [error] one error found
   */
  //implicit val javaUtilDate: TypeBinder[java.util.Date] = option[java.sql.Timestamp].map(_.map(_.toJavaUtilDate).orNull[java.util.Date])
  implicit val javaUtilCalendar: TypeBinder[java.util.Calendar] = {
    option[java.sql.Timestamp].map(_.map { t =>
      val c = java.util.Calendar.getInstance
      c.setTime(t.toJavaUtilDate)
      c
    }.orNull[java.util.Calendar])
  }
  implicit val jodaDateTime: TypeBinder[JodaDateTime] = option[java.sql.Timestamp].map(_.map(_.toJodaDateTime).orNull[JodaDateTime])
  implicit val jodaLocalDate: TypeBinder[JodaLocalDate] = option[java.sql.Date].map(_.map(_.toJodaLocalDate).orNull[JodaLocalDate])
  implicit val jodaLocalTime: TypeBinder[JodaLocalTime] = option[java.sql.Time].map(_.map(_.toJodaLocalTime).orNull[JodaLocalTime])
  implicit val jpdaLocalDateTime: TypeBinder[JodaLocalDateTime] = option[java.sql.Timestamp].map(_.map(_.toJodaLocalDateTime).orNull)

  implicit val url: TypeBinder[java.net.URL] = TypeBinder(_ getURL _)(_ getURL _)

  private[scalikejdbc] val asciiStream: TypeBinder[java.io.InputStream] = TypeBinder(_ getAsciiStream _)(_ getAsciiStream _)
  private[scalikejdbc] val nCharacterStream: TypeBinder[java.io.Reader] = TypeBinder(_ getNCharacterStream _)(_ getNCharacterStream _)
  private[scalikejdbc] val nString: TypeBinder[String] = TypeBinder(_ getNString _)(_ getNString _)

  private def throwExceptionIfNull[A <: AnyVal](f: Any => A)(a: Any): A =
    if (a == null) throw new UnexpectedNullValueException else f(a)

}

trait LowPriorityTypeBinderImplicits {

  implicit def option[A](implicit ev: TypeBinder[A]): TypeBinder[Option[A]] = new TypeBinder[Option[A]] {
    def apply(rs: ResultSet, columnIndex: Int): Option[A] = wrap(ev(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): Option[A] = wrap(ev(rs, columnLabel))
    private def wrap(a: => A): Option[A] =
      try Option(a) catch { case _: NullPointerException | _: UnexpectedNullValueException => None }
  }

}
