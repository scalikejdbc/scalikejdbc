package scalikejdbc

import java.sql.{JDBCType, ResultSet, SQLType}

import org.joda.time.{DateTime => JodaDateTime}
import org.joda.time.{LocalDate => JodaLocalDate}
import org.joda.time.{LocalTime => JodaLocalTime}
import org.joda.time.{LocalDateTime => JodaLocalDateTime}

/**
 * Type binder for java.sql.ResultSet.
 */
trait TypeBinder[+A] { self =>
  def sqlType : SQLType
  def fromSqlType(value : Any) : A

  def apply(rs: ResultSet, columnIndex: Int): A
  def apply(rs: ResultSet, columnLabel: String): A

  def map[B](f: A => B): TypeBinder[B] = new TypeBinder[B] {
    def apply(rs: ResultSet, columnIndex: Int): B = f(TypeBinder.this.apply(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): B = f(TypeBinder.this.apply(rs, columnLabel))

    override val sqlType: SQLType = self.sqlType
    override def fromSqlType(value: Any) = f(self.fromSqlType(value))
  }
}

/**
 * Type binder for java.sql.ResultSet.
 */
object TypeBinder extends UnixTimeInMillisConverterImplicits {
  // TODO: Remove UnixTimeInMillisConverterImplicits in 2.5.
  // TypeBinder object actually doesn't need UnixTimeInMillisConverterImplicits.
  // Since removing it breaks bin-compatibility, we cannot do that in 2.4 series.

  def apply[A](index: (ResultSet, Int) => A)(label: (ResultSet, String) => A): TypeBinder[A] = new TypeBinder[A] {
    def apply(rs: ResultSet, columnIndex: Int): A = index(rs, columnIndex)
    def apply(rs: ResultSet, columnLabel: String): A = label(rs, columnLabel)

    // FIXME: Do something here
    override val sqlType: SQLType = JDBCType.OTHER
    override def fromSqlType(value: Any): A = ???
  }

  private[scalikejdbc] val any: TypeBinder[Any] = TypeBinder(_ getObject _)(_ getObject _)

  // FIXME: Candidates for removal
  implicit val array: TypeBinder[java.sql.Array] = Binders.sqlArray
  implicit val blob: TypeBinder[java.sql.Blob] = Binders.blob
  implicit val clob: TypeBinder[java.sql.Clob] = Binders.clob
  implicit val date: TypeBinder[java.sql.Date] = Binders.sqlDate
  implicit val nClob: TypeBinder[java.sql.NClob] = Binders.nClob
  implicit val ref: TypeBinder[java.sql.Ref] = Binders.ref
  implicit val rowId: TypeBinder[java.sql.RowId] = Binders.rowId
  implicit val sqlXml: TypeBinder[java.sql.SQLXML] = Binders.sqlXml
  implicit val time: TypeBinder[java.sql.Time] = Binders.sqlTime
  implicit val timestamp: TypeBinder[java.sql.Timestamp] = Binders.sqlTimestamp

  implicit val boolean: TypeBinder[Boolean] = Binders.boolean
  implicit val byte: TypeBinder[Byte] = Binders.byte
  implicit val bytes: TypeBinder[Array[Byte]] = Binders.bytes
  implicit val double: TypeBinder[Double] = Binders.double
  implicit val float: TypeBinder[Float] = Binders.float
  implicit val int: TypeBinder[Int] = Binders.int
  implicit val long: TypeBinder[Long] = Binders.long
  implicit val short: TypeBinder[Short] = Binders.short
  implicit val string: TypeBinder[String] = Binders.string
  implicit val scalaBigDecimal: TypeBinder[BigDecimal] = Binders.bigDecimal
  implicit val scalaBigInt: TypeBinder[BigInt] = Binders.bigInt

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

  implicit def option[A : TypeBinder]: TypeBinder[Option[A]] = Binders.optionReaderBinder[A]
}
