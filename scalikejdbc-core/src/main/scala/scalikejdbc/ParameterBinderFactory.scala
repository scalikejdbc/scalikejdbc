package scalikejdbc

import java.io.InputStream
import java.sql.PreparedStatement

import scalikejdbc.interpolation.SQLSyntax

import scala.annotation.implicitNotFound

@implicitNotFound("""
--------------------------------------------------------
 Implicit ParameterBinderFactory[${A}] for the parameter type ${A} is missing.
 You need to define ParameterBinderFactory for the type or use AsIsParameterBinder.

  (example1)
    implicit val intParameterBinderFactory: ParameterBinderFactory[Int] = ParameterBinderFactory {
      value => (stmt, idx) => stmt.setInt(idx, value)
    }

  (example2)
    case class Price(value: Int)
    object Price {
      implicit val converter: Binders[Price] = Binders.int.xmap(Price.apply, _.value)
    }

  (example3)
    val value: Any = 123
    val key: SQLSyntax = sqls"column_name"
    key -> AsIsParameterBinder(value)
--------------------------------------------------------""")
trait ParameterBinderFactory[A] { self =>

  def apply(value: A): ParameterBinderWithValue

  def contramap[B](g: B => A): ParameterBinderFactory[B] =
    (value: B) => {
      if (value == null) ParameterBinder.NullParameterBinder
      else ContramappedParameterBinder(value, self(g(value)))
    }

}

object ParameterBinderFactory
  extends LowPriorityImplicitsParameterBinderFactory1 {

  def apply[A](
    f: A => (PreparedStatement, Int) => Unit
  ): ParameterBinderFactory[A] = (value: A) => {
    if (value == null) ParameterBinder.NullParameterBinder
    else ParameterBinder(value, f(value))
  }

  implicit val longParameterBinderFactory: ParameterBinderFactory[Long] =
    Binders.long
  implicit val javaLongParameterBinderFactory
    : ParameterBinderFactory[java.lang.Long] = Binders.javaLong
  implicit val intParameterBinderFactory: ParameterBinderFactory[Int] =
    Binders.int
  implicit val javaIntegerParameterBinderFactory
    : ParameterBinderFactory[java.lang.Integer] = Binders.javaInteger
  implicit val shortParameterBinderFactory: ParameterBinderFactory[Short] =
    Binders.short
  implicit val javaShortParameterBinderFactory
    : ParameterBinderFactory[java.lang.Short] = Binders.javaShort
  implicit val byteParameterBinderFactory: ParameterBinderFactory[Byte] =
    Binders.byte
  implicit val javaByteParameterBinderFactory
    : ParameterBinderFactory[java.lang.Byte] = Binders.javaByte
  implicit val doubleParameterBinderFactory: ParameterBinderFactory[Double] =
    Binders.double
  implicit val javaDoubleParameterBinderFactory
    : ParameterBinderFactory[java.lang.Double] = Binders.javaDouble
  implicit val floatParameterBinderFactory: ParameterBinderFactory[Float] =
    Binders.float
  implicit val javaFloatParameterBinderFactory
    : ParameterBinderFactory[java.lang.Float] = Binders.javaFloat
  implicit val booleanParameterBinderFactory: ParameterBinderFactory[Boolean] =
    Binders.boolean
  implicit val javaBooleanParameterBinderFactory
    : ParameterBinderFactory[java.lang.Boolean] = Binders.javaBoolean
  implicit val stringParameterBinderFactory: ParameterBinderFactory[String] =
    Binders.string
  implicit val bigIntParameterBinderFactory: ParameterBinderFactory[BigInt] =
    Binders.bigInt
  implicit val javaBigIntegerParameterBinderFactory
    : ParameterBinderFactory[java.math.BigInteger] = Binders.javaBigInteger
  implicit val bigDecimalParameterBinderFactory
    : ParameterBinderFactory[BigDecimal] = Binders.bigDecimal
  implicit val javaBigDecimalParameterBinderFactory
    : ParameterBinderFactory[java.math.BigDecimal] = Binders.javaBigDecimal
  implicit val urlParameterBinderFactory: ParameterBinderFactory[java.net.URL] =
    Binders.url
  implicit val sqlArrayParameterBinderFactory
    : ParameterBinderFactory[java.sql.Array] = Binders.sqlArray
  implicit val sqlXmlParameterBinderFactory
    : ParameterBinderFactory[java.sql.SQLXML] = Binders.sqlXml
  implicit val sqlDateParameterBinderFactory
    : ParameterBinderFactory[java.sql.Date] = Binders.sqlDate
  implicit val sqlTimeParameterBinderFactory
    : ParameterBinderFactory[java.sql.Time] = Binders.sqlTime
  implicit val sqlTimestampParameterBinderFactory
    : ParameterBinderFactory[java.sql.Timestamp] = Binders.sqlTimestamp
  implicit val utilDateParameterBinderFactory
    : ParameterBinderFactory[java.util.Date] = Binders.utilDate

  implicit val javaTimeInstantParameterBinderFactory
    : ParameterBinderFactory[java.time.Instant] = Binders.javaTimeInstant
  implicit val javaTimeZonedDateTimeParameterBinderFactory
    : ParameterBinderFactory[java.time.ZonedDateTime] =
    Binders.javaTimeZonedDateTime
  implicit val javaTimeOffsetDateTimeParameterBinderFactory
    : ParameterBinderFactory[java.time.OffsetDateTime] =
    Binders.javaTimeOffsetDateTime
  implicit val javaTimeLocalDateTimeParameterBinderFactory
    : ParameterBinderFactory[java.time.LocalDateTime] =
    Binders.javaTimeLocalDateTime
  implicit val javaTimeLocalDateParameterBinderFactory
    : ParameterBinderFactory[java.time.LocalDate] = Binders.javaTimeLocalDate
  implicit val javaTimeLocalTimeParameterBinderFactory
    : ParameterBinderFactory[java.time.LocalTime] = Binders.javaTimeLocalTime

  implicit val inputStreamParameterBinderFactory
    : ParameterBinderFactory[InputStream] = Binders.binaryStream
  implicit val blobParameterBinderFactory
    : ParameterBinderFactory[java.sql.Blob] = Binders.blob
  implicit val clobParameterBinderFactory
    : ParameterBinderFactory[java.sql.Clob] = Binders.clob
  implicit val nClobParameterBinderFactory
    : ParameterBinderFactory[java.sql.NClob] = Binders.nClob
  implicit val refParameterBinderFactory: ParameterBinderFactory[java.sql.Ref] =
    Binders.ref
  implicit val rowIdParameterBinderFactory
    : ParameterBinderFactory[java.sql.RowId] = Binders.rowId
  implicit val bytesParameterBinderFactory
    : ParameterBinderFactory[Array[Byte]] = Binders.bytes
  implicit val readerParameterBinderFactory
    : ParameterBinderFactory[java.io.Reader] = Binders.characterStream
  implicit val calendarParameterBinderFactory
    : ParameterBinderFactory[java.util.Calendar] = Binders.javaUtilCalendar
  implicit val nullParameterBinderFactory: ParameterBinderFactory[Null] =
    (value: Null) => ParameterBinder.NullParameterBinder
  implicit val noneParameterBinderFactory: ParameterBinderFactory[None.type] =
    (value: None.type) => ParameterBinder.NullParameterBinder
  implicit val sqlSyntaxParameterBinderFactory
    : ParameterBinderFactory[SQLSyntax] =
    (value: SQLSyntax) => SQLSyntaxParameterBinder(value)

  implicit val optionalSqlSyntaxParameterBinderFactory
    : ParameterBinderFactory[Option[SQLSyntax]] =
    (value: Option[SQLSyntax]) => {
      val result = value match {
        case null         => SQLSyntaxParameterBinder(null)
        case None         => SQLSyntaxParameterBinder(SQLSyntax.empty)
        case Some(syntax) => SQLSyntaxParameterBinder(syntax)
      }
      ContramappedParameterBinder(value, result)
    }

}

sealed abstract class LowPriorityImplicitsParameterBinderFactory1 {

  implicit def optionalParameterBinderFactory[A](implicit
    ev: ParameterBinderFactory[A]
  ): ParameterBinderFactory[Option[A]] = (value: Option[A]) => {
    if (value == null) ParameterBinder.NullParameterBinder
    else if (ev == asisParameterBinderFactory) AsIsParameterBinder(value)
    else
      value.fold[ParameterBinderWithValue](
        ParameterBinder.NullParameterBinder
      ) { v =>
        ContramappedParameterBinder(v, ev(v))
      }
  }

  /**
   * Unsafe ParameterBinderFactory which accepts any type value as-is.
   *
   * This implicit is not enabled by default. If you need this, have implicit val definition in your own code.
   */
  val asisParameterBinderFactory: ParameterBinderFactory[Any] =
    (value: Any) =>
      AsIsParameterBinder(
        value
      )

}
