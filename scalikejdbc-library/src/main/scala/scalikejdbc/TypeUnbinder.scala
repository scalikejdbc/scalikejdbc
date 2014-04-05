package scalikejdbc

import org.joda.time.{ LocalTime, LocalDate, DateTime, LocalDateTime }

/**
 * Type unbinder for PreparedStatement#set
 */
trait TypeUnbinder[-A] { self =>

  def apply(value: A): Any
  def contramap[B](f: B => A): TypeUnbinder[B] = new TypeUnbinder[B] {
    def apply(value: B): Any = self(f(value))
  }

}

object TypeUnbinder extends LowPriorityTypeUninderImplicits {

  def apply[A](f: A => Any): TypeUnbinder[A] = new TypeUnbinder[A] {
    def apply(value: A): Any = f(value)
  }

  private val any: TypeUnbinder[Any] = TypeUnbinder(identity)

  implicit val array: TypeUnbinder[java.sql.Array] = any
  implicit val bigDecimal: TypeUnbinder[java.math.BigDecimal] = any
  implicit val scalaBigDecimal: TypeUnbinder[BigDecimal] = any
  implicit val binaryStream: TypeUnbinder[java.io.InputStream] = any
  implicit val blob: TypeUnbinder[java.sql.Blob] = any
  implicit val nullableBoolean: TypeUnbinder[java.lang.Boolean] = any
  implicit val boolean: TypeUnbinder[Boolean] = any
  implicit val optionBoolean: TypeUnbinder[Option[Boolean]] = any
  implicit val nullableByte: TypeUnbinder[java.lang.Byte] = any
  implicit val byte: TypeUnbinder[Byte] = any
  implicit val optionByte: TypeUnbinder[Option[Byte]] = any
  implicit val bytes: TypeUnbinder[Array[Byte]] = any
  implicit val characterStream: TypeUnbinder[java.io.Reader] = any
  implicit val clob: TypeUnbinder[java.sql.Clob] = any
  implicit val date: TypeUnbinder[java.sql.Date] = any
  implicit val nullableDouble: TypeUnbinder[java.lang.Double] = any
  implicit val double: TypeUnbinder[Double] = any
  implicit val optionDouble: TypeUnbinder[Option[Double]] = any
  implicit val nullableFloat: TypeUnbinder[java.lang.Float] = any
  implicit val float: TypeUnbinder[Float] = any
  implicit val optionFloat: TypeUnbinder[Option[Float]] = any
  implicit val nullableInt: TypeUnbinder[java.lang.Integer] = any
  implicit val int: TypeUnbinder[Int] = any
  implicit val optionInt: TypeUnbinder[Option[Int]] = any
  implicit val nullableLong: TypeUnbinder[java.lang.Long] = any
  implicit val long: TypeUnbinder[Long] = any
  implicit val optionLong: TypeUnbinder[Option[Long]] = any
  implicit val nClob: TypeUnbinder[java.sql.NClob] = any
  implicit val ref: TypeUnbinder[java.sql.Ref] = any
  implicit val rowId: TypeUnbinder[java.sql.RowId] = any
  implicit val nullableShort: TypeUnbinder[java.lang.Short] = any
  implicit val short: TypeUnbinder[Short] = any
  implicit val optionShort: TypeUnbinder[Option[Short]] = any
  implicit val sqlXml: TypeUnbinder[java.sql.SQLXML] = any

  implicit val string: TypeUnbinder[String] = any

  implicit val time: TypeUnbinder[java.sql.Time] = any
  implicit val timestamp: TypeUnbinder[java.sql.Timestamp] = any
  implicit val javaUtilCalendar: TypeUnbinder[java.util.Calendar] = any
  implicit val dateTime: TypeUnbinder[DateTime] = any
  implicit val localDate: TypeUnbinder[LocalDate] = any
  implicit val localTime: TypeUnbinder[LocalTime] = any
  implicit val localDateTime: TypeUnbinder[LocalDateTime] = any

  implicit val url: TypeUnbinder[java.net.URL] = any

}
trait LowPriorityTypeUninderImplicits {

  implicit def option[A: TypeUnbinder]: TypeUnbinder[Option[A]] = new TypeUnbinder[Option[A]] {
    def apply(value: Option[A]): Any = value.map(implicitly[TypeUnbinder[A]].apply)
  }

}