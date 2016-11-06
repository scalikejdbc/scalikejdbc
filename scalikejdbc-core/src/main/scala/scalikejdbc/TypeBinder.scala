package scalikejdbc

import java.sql.ResultSet
import java.time._
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
  // TODO: Remove UnixTimeInMillisConverterImplicits in 2.5.
  // TypeBinder object actually doesn't need UnixTimeInMillisConverterImplicits.
  // Since removing it breaks bin-compatibility, we cannot do that in 2.4 series.

  def apply[A](index: (ResultSet, Int) => A)(label: (ResultSet, String) => A): TypeBinder[A] = new TypeBinder[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs, columnLabel)
  }

  private[scalikejdbc] val any: TypeBinder[Any] = TypeBinder(_ getObject _)(_ getObject _)
  implicit val array: TypeBinder[java.sql.Array] = Binders.sqlArray

  implicit val bigDecimal: TypeBinder[java.math.BigDecimal] = Binders.javaBigDecimal
  implicit val scalaBigDecimal: TypeBinder[BigDecimal] = Binders.bigDecimal
  implicit val bigInteger: TypeBinder[java.math.BigInteger] = Binders.javaBigInteger
  implicit val scalaBigInt: TypeBinder[BigInt] = Binders.bigInt

  implicit val binaryStream: TypeBinder[java.io.InputStream] = Binders.binaryStream
  implicit val blob: TypeBinder[java.sql.Blob] = Binders.blob
  implicit val nullableBoolean: TypeBinder[java.lang.Boolean] = Binders.javaBoolean
  implicit val boolean: TypeBinder[Boolean] = Binders.boolean
  implicit val optionBoolean: TypeBinder[Option[Boolean]] = Binders.optionBoolean
  implicit val nullableByte: TypeBinder[java.lang.Byte] = Binders.javaByte
  implicit val byte: TypeBinder[Byte] = Binders.byte
  implicit val optionByte: TypeBinder[Option[Byte]] = Binders.optionByte
  implicit val bytes: TypeBinder[Array[Byte]] = Binders.bytes
  implicit val characterStream: TypeBinder[java.io.Reader] = Binders.characterStream
  implicit val clob: TypeBinder[java.sql.Clob] = Binders.clob
  implicit val date: TypeBinder[java.sql.Date] = Binders.sqlDate
  implicit val nullableDouble: TypeBinder[java.lang.Double] = Binders.javaDouble
  implicit val double: TypeBinder[Double] = Binders.double
  implicit val optionDouble: TypeBinder[Option[Double]] = Binders.optionDouble
  implicit val nullableFloat: TypeBinder[java.lang.Float] = Binders.javaFloat
  implicit val float: TypeBinder[Float] = Binders.float
  implicit val optionFloat: TypeBinder[Option[Float]] = Binders.optionFloat
  implicit val nullableInt: TypeBinder[java.lang.Integer] = Binders.javaInteger
  implicit val int: TypeBinder[Int] = Binders.int
  implicit val optionInt: TypeBinder[Option[Int]] = Binders.optionInt
  implicit val nullableLong: TypeBinder[java.lang.Long] = Binders.javaLong
  implicit val long: TypeBinder[Long] = Binders.long
  implicit val optionLong: TypeBinder[Option[Long]] = Binders.optionLong
  implicit val nClob: TypeBinder[java.sql.NClob] = Binders.nClob
  implicit val ref: TypeBinder[java.sql.Ref] = Binders.ref
  implicit val rowId: TypeBinder[java.sql.RowId] = Binders.rowId
  implicit val nullableShort: TypeBinder[java.lang.Short] = Binders.javaShort
  implicit val short: TypeBinder[Short] = Binders.short
  implicit val optionShort: TypeBinder[Option[Short]] = Binders.optionShort
  implicit val sqlXml: TypeBinder[java.sql.SQLXML] = Binders.sqlXml

  implicit val string: TypeBinder[String] = Binders.string

  implicit val time: TypeBinder[java.sql.Time] = Binders.sqlTime
  implicit val timestamp: TypeBinder[java.sql.Timestamp] = Binders.sqlTimestamp

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
  implicit val javaUtilCalendar: TypeBinder[java.util.Calendar] = Binders.javaUtilCalendar
  implicit val jodaDateTime: TypeBinder[JodaDateTime] = Binders.jodaDateTime
  implicit val jodaLocalDate: TypeBinder[JodaLocalDate] = Binders.jodaLocalDate
  implicit val jodaLocalTime: TypeBinder[JodaLocalTime] = Binders.jodaLocalTime
  implicit val jpdaLocalDateTime: TypeBinder[JodaLocalDateTime] = Binders.jodaLocalDateTime

  implicit val url: TypeBinder[java.net.URL] = Binders.url

  implicit val zonedDateTime: TypeBinder[ZonedDateTime] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[ZonedDateTime])
  implicit val offsetDateTime: TypeBinder[OffsetDateTime] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())).orNull[OffsetDateTime])
  implicit val instant: TypeBinder[Instant] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime)).orNull[Instant])
  implicit val localDate: TypeBinder[LocalDate] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate).orNull[LocalDate])
  implicit val localTime: TypeBinder[LocalTime] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime).orNull[LocalTime])
  implicit val localDateTime: TypeBinder[LocalDateTime] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime).orNull[LocalDateTime])

  implicit val zonedDateTimeOpt: TypeBinder[Option[ZonedDateTime]] = option[java.sql.Timestamp].map(_.map(v => ZonedDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val offsetDateTimeOpt: TypeBinder[Option[OffsetDateTime]] = option[java.sql.Timestamp].map(_.map(v => OffsetDateTime.ofInstant(Instant.ofEpochMilli(v.getTime), ZoneId.systemDefault())))
  implicit val instantOpt: TypeBinder[Option[Instant]] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime)))
  implicit val localDateOpt: TypeBinder[Option[LocalDate]] = option[java.sql.Date].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDate))
  implicit val localTimeOpt: TypeBinder[Option[LocalTime]] = option[java.sql.Time].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalTime))
  implicit val localDateTimeOpt: TypeBinder[Option[LocalDateTime]] = option[java.sql.Timestamp].map(_.map(v => Instant.ofEpochMilli(v.getTime).atZone(ZoneId.systemDefault()).toLocalDateTime))

  private[scalikejdbc] val asciiStream: TypeBinder[java.io.InputStream] = Binders.asciiStream
  private[scalikejdbc] val nCharacterStream: TypeBinder[java.io.Reader] = Binders.nCharacterStream
  private[scalikejdbc] val nString: TypeBinder[String] = Binders.nString

}

trait LowPriorityTypeBinderImplicits {

  implicit def option[A](implicit ev: TypeBinder[A]): TypeBinder[Option[A]] = new TypeBinder[Option[A]] {
    def apply(rs: ResultSet, columnIndex: Int): Option[A] = wrap(ev(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): Option[A] = wrap(ev(rs, columnLabel))
    private def wrap(a: => A): Option[A] =
      try Option(a) catch { case _: NullPointerException | _: UnexpectedNullValueException => None }
  }

}
