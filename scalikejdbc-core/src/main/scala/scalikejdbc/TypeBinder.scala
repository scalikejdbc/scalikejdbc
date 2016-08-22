package scalikejdbc

import java.sql.{ ResultSet, SQLType }
import org.joda.time.{ DateTime => JodaDateTime, LocalDate => JodaLocalDate, LocalDateTime => JodaLocalDateTime, LocalTime => JodaLocalTime }

/**
 * Type binder for java.sql.ResultSet.
 */
trait TypeBinder[+A] { self =>
  val handleDefaultForNull: Boolean = false
  def sqlType: SQLType
  def fromSqlType(value: Any): A

  def read(rs: ResultSet, columnIndex: Int): A
  def read(rs: ResultSet, columnLabel: String): A

  def map[B](f: A => B): TypeBinder[B] = new TypeBinder[B] {
    override def read(rs: ResultSet, columnIndex: Int): B = f(TypeBinder.this.read(rs, columnIndex))
    override def read(rs: ResultSet, columnLabel: String): B = f(TypeBinder.this.read(rs, columnLabel))

    override val sqlType: SQLType = self.sqlType
    override def fromSqlType(value: Any) = f(self.fromSqlType(value))
  }
}

/**
 * Type binder for java.sql.ResultSet.
 */
object TypeBinder {

  def apply[T](sqlTypeParam: SQLType, fromSqlTypeParam: Any => T, handleDefaultForNullParam: Boolean = false)(
    getByIndex: (ResultSet, Int, Any => T) => T,
    getByLabel: (ResultSet, String, Any => T) => T
  ): TypeBinder[T] = new TypeBinder[T] {
    override def read(rs: ResultSet, columnIndex: Int): T = getByIndex(rs, columnIndex, fromSqlTypeParam)
    override def read(rs: ResultSet, columnLabel: String): T = getByLabel(rs, columnLabel, fromSqlTypeParam)

    override val sqlType: SQLType = sqlTypeParam
    override def fromSqlType(value: Any): T = fromSqlTypeParam(value)
  }

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

  implicit def option[A: TypeBinder]: TypeBinder[Option[A]] = Binders.optionReaderBinder[A]
}
