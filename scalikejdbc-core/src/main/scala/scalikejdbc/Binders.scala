package scalikejdbc

import java.sql.{JDBCType, PreparedStatement, ResultSet, SQLType}
import scalikejdbc.UnixTimeInMillisConverterImplicits._

/**
 * Provides both of TypeBinder and ParameterBinderFactory for the specified type A.
 */
trait Binders[A] extends TypeBinder[A] with ParameterBinderFactory[A] { self =>

  override def xmap[B](f: A => B, g: B => A): Binders[B] = new Binders[B] {
    def apply(rs: ResultSet, columnIndex: Int): B = f(self(rs, columnIndex))
    def apply(rs: ResultSet, columnLabel: String): B = f(self(rs, columnLabel))

    def apply(value: B): ParameterBinderWithValue[B] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else self(g(value)).map(f)
    }

    override def fromSqlType(value: Any): B = f(self.fromSqlType(value))
    override val sqlType: SQLType = self.sqlType
    override def toSqlType(value: B): Any = self.toSqlType(g(value))
  }

}

/**
 * Provides factories of Binders and built-in Binders.
 */
object Binders {

  // --------------------------------------------------------------------------------------------
  // Factory methods
  // --------------------------------------------------------------------------------------------

  def of[T](sqlType: SQLType, fromSqlType: Any => T, toSqlType: T => Any): Binders[T] =
    ofExt(sqlType, fromSqlType, toSqlType)(
      (rs, index, converter) => converter(rs.getObject(index)),
      (rs, label, converter) => converter(rs.getObject(label)),
      value => (ps, index, converter) =>
        if (sqlType == JDBCType.OTHER)
          ps.setObject(index, converter(value))
        else
          ps.setObject(index, converter(value), sqlType) )

  def ofExt[T](sqlTypeParam: SQLType, fromSqlTypeParam : Any => T, toSqlTypeParam : T => Any)
              (getByIndex: (ResultSet, Int, Any => T) => T,
               getByLabel: (ResultSet, String, Any => T) => T,
               setStatement: T => (PreparedStatement, Int, T => Any) => Unit): Binders[T] = new Binders[T] {

    // from TypeBinder
    override def apply(rs: ResultSet, columnIndex: Int): T = getByIndex(rs, columnIndex, fromSqlType)
    override def apply(rs: ResultSet, columnLabel: String): T = getByLabel(rs, columnLabel, fromSqlType)
    // from ParameterBinderFactory
    override def apply(value: T): ParameterBinderWithValue[T] = {
      if (value == null) ParameterBinder.NullParameterBinder
      else ParameterBinder(value, (statement, i) => setStatement(value)(statement, i, toSqlTypeParam) )
    }

    override val sqlType: SQLType = sqlTypeParam
    override def toSqlType(value: T): Any = toSqlTypeParam(value)
    override def fromSqlType(value: Any): T = fromSqlTypeParam(value)
  }

  // ----------------------------------------------------
  // private

  private[this] def throwExceptionIfNull[A <: AnyVal, B](f: B => A)(a: B): A = {
    if (a == null) throw new UnexpectedNullValueException else f(a)
  }

  private[this] def nullThrough[A, B](f: A => B)(a: A): B = if (a == null) null.asInstanceOf[B] else f(a)

  // --------------------------------------------------------------------------------------------
  // Built-in Binders
  // --------------------------------------------------------------------------------------------
  def optionBinder[T : Binders](implicit binder: Binders[T]) : Binders[Option[T]] = {

    @inline
    def wrap(a: => T): Option[T] = try Option(a) catch { case _: NullPointerException | _: UnexpectedNullValueException => None }

    ofExt[Option[T]](
      binder.sqlType,
      value => if (value == null) None else Some(value.asInstanceOf[T]),
      value => value.fold(null.asInstanceOf[Any])(v => binder.toSqlType(v)))(
      (rs, index, _) => wrap(binder.apply(rs, index)),
      (rs, label, _) => wrap(binder.apply(rs, label)),
      valueOpt => (ps, index, _) => valueOpt.fold(ps.setObject(index, null))(value => binder.apply(value).apply(ps, index)) )
  }

  // FIXME: Simplify this horror
  def optionReaderBinder[T](implicit binder : TypeBinder[T]) : TypeBinder[Option[T]] =
    new TypeBinder[Option[T]] {
      @inline
      def wrap(a: => T): Option[T] = try Option(a) catch { case _: NullPointerException | _: UnexpectedNullValueException => None }

      override def apply(rs: ResultSet, columnIndex: Int): Option[T] = wrap(binder.apply(rs, columnIndex))
      override def apply(rs: ResultSet, columnLabel: String): Option[T] = wrap(binder.apply(rs, columnLabel))
      override def fromSqlType(value: Any): Option[T] = if (value == null) None else Some(binder.fromSqlType(value))
      override val sqlType: SQLType = binder.sqlType
    }

  // FIXME: Simplify this horror
  def optionWriterBinder[T](implicit binder : ParameterBinderFactory[T]) : ParameterBinderFactory[Option[T]] =
    new ParameterBinderFactory[Option[T]] {
      override def toSqlType(value: Option[T]): Any = value.fold(null.asInstanceOf[Any])(v => binder.toSqlType(v))
      override def apply(valueOpt: Option[T]): ParameterBinderWithValue[Option[T]] = new ParameterBinderWithValue[Option[T]] {
        override def value: Option[T] = valueOpt
        override def apply(stmt: PreparedStatement, idx: Int): Unit = valueOpt.fold(stmt.setObject(idx, null))(v => binder.apply(v).apply(stmt, idx))
      }
      override val sqlType: SQLType = binder.sqlType
    }

  @inline
  private def asIsBinderWithType[T](sqlType: SQLType) = Binders.of[T](sqlType, _.asInstanceOf[T], identity)

  @inline
  private def asIsBinderWithTypeAndAccessors[T](sqlType: SQLType) : ((ResultSet, Int, (Any) => T) => T, (ResultSet, String, (Any) => T) => T, (T) => (PreparedStatement, Int, (T) => Any) => Unit) => Binders[T] = {
    Binders.ofExt[T](sqlType, _.asInstanceOf[T], identity)
  }

  val byte = asIsBinderWithType[Byte](JDBCType.TINYINT) // depending on DB vendor 1 or 2 bytes
  val short = asIsBinderWithType[Short](JDBCType.SMALLINT) // 2 bytes
  val int = asIsBinderWithType[Int](JDBCType.INTEGER) // 4 bytes
  val long = asIsBinderWithType[Long](JDBCType.BIGINT) // 8 bytes
  val float = asIsBinderWithType[Float](JDBCType.REAL) // 4 bytes
  val double = asIsBinderWithType[Double](JDBCType.DOUBLE) // 8 bytes
  val char = asIsBinderWithType[Char](JDBCType.CHAR) // 1 byte
  val boolean = asIsBinderWithType[Boolean](JDBCType.BOOLEAN) // 1 byte
  val string = asIsBinderWithType[String](JDBCType.VARCHAR) // Postgresql 9.x prefers to use TEXT
  val bigDecimal = asIsBinderWithType[BigDecimal](JDBCType.NUMERIC)
  val bigInt = bigDecimal.xmap[BigInt](_.toBigInt(), BigDecimal.apply)
  val bytes = asIsBinderWithTypeAndAccessors[Array[Byte]](JDBCType.BINARY) (
    (rs, index, _) => rs.getBytes(index),
    (rs, label, _) => rs.getBytes(label),
    value => (ps, index, _) => ps.setBytes(index, value) )

  // FIXME: Candidates for removal because it's unlikely people will use java.sql types since they've already chosen Scalikejdbc to avoid JDBC :)
  val sqlArray = asIsBinderWithType[java.sql.Array](JDBCType.ARRAY)
  val sqlDate = asIsBinderWithType[java.sql.Date](JDBCType.DATE)
  val sqlXml = asIsBinderWithType[java.sql.SQLXML](JDBCType.SQLXML)
  val sqlTime = asIsBinderWithType[java.sql.Time](JDBCType.TIME)
  val sqlTimestamp = asIsBinderWithType[java.sql.Timestamp](JDBCType.TIMESTAMP)
  val blob = asIsBinderWithType[java.sql.Blob](JDBCType.BLOB)
  val clob = asIsBinderWithType[java.sql.Clob](JDBCType.CLOB)
  val nClob = asIsBinderWithType[java.sql.NClob](JDBCType.NCLOB)
  val ref = asIsBinderWithType[java.sql.Ref](JDBCType.REF)
  val rowId = asIsBinderWithType[java.sql.RowId](JDBCType.ROWID)

  /*
  // FIXME: Not sure if we need java.io classes here. Maybe we should Scala counterparts instead.
  // FIXME: Plus `java.io.Reader` can be written/read using many ways: getCharacterStream/getBinaryStream/getAsciiStream
  // FIXME: Same goes for `java.io.Reader`
  val characterStream: Binders[java.io.Reader] = asIsBinderWithTypeAndAccessors(JDBCType.LONGVARCHAR) (
    (rs, index, _) => rs.getCharacterStream(index),
    (rs, label, _) => rs.getCharacterStream(label),
    value => (ps, index, _) => ps.setCharacterStream(index, value) )
  val asciiStream: Binders[java.io.InputStream] = asIsBinderWithTypeAndAccessors(JDBCType.LONGVARCHAR) (
    (rs, index, _) => rs.getAsciiStream(index),
    (rs, label, _) => rs.getAsciiStream(label),
    value => (ps, index, _) => ps.setAsciiStream(index, value) )
  val binaryStream: Binders[java.io.InputStream] = asIsBinderWithTypeAndAccessors(JDBCType.LONGVARBINARY) (
    (rs, index, _) => rs.getBinaryStream(index),
    (rs, label, _) => rs.getBinaryStream(label),
    value => (ps, index, _) => ps.setBinaryStream(index, value) )
  val nString: Binders[String] = asIsBinderWithTypeAndAccessors(JDBCType.NVARCHAR) (
    (rs, index, _) => rs.getNString(index),
    (rs, label, _) => rs.getNString(label),
    value => (ps, index, _) => ps.setNString(index, value) )
  // FIXME: No need to implement Binder for java.net.URL as implementation differs from vendor to vendor - Postgresql throws an exception
  // FIXME: in setURL, mysql converts it to setString. It should be up to developer how he wants to serialize it.
  val url: Binders[java.net.URL] = asIsBinderWithTypeAndAccessors(JDBCType.DATALINK) (
    (rs, index, _) => rs.getURL(index),
    (rs, label, _) => rs.getURL(label),
    value => (ps, index, _) => ps.setURL(index, value) )
  */

  val utilDate: Binders[java.util.Date] = sqlTimestamp.xmap(identity, _.toSqlTimestamp)
  val jodaDateTime: Binders[org.joda.time.DateTime] = utilDate.xmap(nullThrough(_.toJodaDateTime), _.toDate)
  val jodaLocalDateTime: Binders[org.joda.time.LocalDateTime] = utilDate.xmap(nullThrough(_.toJodaLocalDateTime), _.toDate)
  val jodaLocalDate: Binders[org.joda.time.LocalDate] = sqlDate.xmap(nullThrough(_.toJodaLocalDate), _.toDate.toSqlDate)
  val jodaLocalTime: Binders[org.joda.time.LocalTime] = sqlTime.xmap(nullThrough(_.toJodaLocalTime), _.toSqlTime)
  val javaUtilCalendar: Binders[java.util.Calendar] = utilDate.xmap(nullThrough { v =>
    val c = java.util.Calendar.getInstance
    c.setTime(v)
    c
  }, _.getTime)
}
