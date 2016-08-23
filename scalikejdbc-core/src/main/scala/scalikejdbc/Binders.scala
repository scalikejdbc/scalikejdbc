package scalikejdbc

import java.sql.{ JDBCType, PreparedStatement, ResultSet, SQLType }
import scalikejdbc.UnixTimeInMillisConverterImplicits._

/**
 * Provides both of TypeBinder and ParameterBinderFactory for the specified type A.
 */
trait Binders[A] extends TypeBinder[A] with ParameterBinderFactory[A] {
  self =>

  override def xmap[B](f: A => B, g: B => A): Binders[B] = new Binders[B] {
    override def read(rs: ResultSet, columnIndex: Int): B = f(self.read(rs, columnIndex))
    override def read(rs: ResultSet, columnLabel: String): B = f(self.read(rs, columnLabel))

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

  /*
  def of[T](sqlType: SQLType, fromSqlType: Any => T, toSqlType: T => Any): Binders[T] =
    ofExt(sqlType, fromSqlType, toSqlType)(
      (rs, index, converter) => converter(rs.getObject(index)),
      (rs, label, converter) => converter(rs.getObject(label)),
      value => (ps, index, converter) =>
        if (sqlType == JDBCType.OTHER)
          ps.setObject(index, converter(value))
        else
          ps.setObject(index, converter(value), sqlType))
  */

  def of[T](sqlTypeParam: SQLType, fromSqlTypeParam: Any => T, toSqlTypeParam: T => Any, handleDefaultForNullParam: Boolean = false)(
    getByIndex: (ResultSet, Int, Any => T) => T,
    getByLabel: (ResultSet, String, Any => T) => T,
    setStatement: T => (PreparedStatement, Int, T => Any) => Unit
  ): Binders[T] = new Binders[T] {

    override val handleDefaultForNull = handleDefaultForNullParam

    // from TypeBinder
    override def read(rs: ResultSet, columnIndex: Int): T = getByIndex(rs, columnIndex, fromSqlType)
    override def read(rs: ResultSet, columnLabel: String): T = {
      val res = getByLabel(rs, columnLabel, fromSqlType)
      res
    }
    // from ParameterBinderFactory
    override def apply(value: T): ParameterBinderWithValue[T] = {
      if (value == null)
        ParameterBinder.NullParameterBinder
      else
        ParameterBinder(value, (statement, i) => {
          val statement1: (PreparedStatement, Int, (T) => Any) => Unit = setStatement(value)
          statement1(statement, i, toSqlTypeParam)
        })
    }

    override val sqlType: SQLType = sqlTypeParam
    override def toSqlType(value: T): Any = toSqlTypeParam(value)
    override def fromSqlType(value: Any): T = fromSqlTypeParam(value)
  }

  // ----------------------------------------------------
  // private

  private[this] def nullThrough[A, B](f: A => B)(a: A): B = if (a == null) null.asInstanceOf[B] else f(a)

  // --------------------------------------------------------------------------------------------
  // Built-in Binders
  // --------------------------------------------------------------------------------------------
  /*
  def optionBinder[T: Binders](implicit binder: Binders[T]): Binders[Option[T]] = {

    @inline
    def wrap(a: => T): Option[T] = try Option(a) catch {
      case _: NullPointerException | _: UnexpectedNullValueException => None
    }

    ofExt[Option[T]](
      binder.sqlType,
      value => if (value == null) None else Some(value.asInstanceOf[T]),
      value => value.fold(null.asInstanceOf[Any])(v => binder.toSqlType(v)))(
      (rs, index, _) => wrap(binder.apply(rs, index)),
      (rs, label, _) => wrap(binder.apply(rs, label)),
      valueOpt => (ps, index, _) => valueOpt.fold(ps.setObject(index, null))(value => binder.apply(value).apply(ps, index)))
  }
  */

  @inline
  private def asIsWithAccessors[T](sqlType: SQLType, handleDefaultForNull: Boolean = false): ((ResultSet, Int, (Any) => T) => T, (ResultSet, String, (Any) => T) => T, (T) => (PreparedStatement, Int, (T) => Any) => Unit) => Binders[T] = {
    Binders.of[T](sqlType, _.asInstanceOf[T], identity, handleDefaultForNull)
  }

  val any = asIsWithAccessors[Any](JDBCType.OTHER)((rs, i, _) => rs.getObject(i), (rs, i, _) => rs.getObject(i), v => (ps, i, _) => ps.setObject(i, v, JDBCType.OTHER))
  val byte = asIsWithAccessors[Byte](JDBCType.TINYINT, handleDefaultForNull = true)((rs, i, _) => {
//    println("calling by index")
//    val obj = rs.getObject(i)
//    val wasNull1 = rs.wasNull()
//    println(s"obj i: $obj, $wasNull1")
    val res = rs.getByte(i)
//    val wasNull2 = rs.wasNull()
//    println(s"res i: $res, $wasNull2")
    res
  }, (rs, i, _) => {
//    println("calling  by label")
//    val obj = rs.getObject(i)
//    val wasNull1 = rs.wasNull()
//    println(s"obj L: $obj, $wasNull1")
    val res = rs.getByte(i)
//    val wasNull2 = rs.wasNull()
//    println(s"res L: $res, $wasNull2")
    res
  }, v => (ps, i, _) => ps.setByte(i, v))
  val short = asIsWithAccessors[Short](JDBCType.SMALLINT, handleDefaultForNull = true)((rs, i, _) => rs.getShort(i), (rs, i, _) => rs.getShort(i), v => (ps, i, _) => ps.setShort(i, v))
  val int = asIsWithAccessors[Int](JDBCType.INTEGER, handleDefaultForNull = true)((rs, i, _) => rs.getInt(i), (rs, i, _) => rs.getInt(i), v => (ps, i, _) => ps.setInt(i, v))
  val long = asIsWithAccessors[Long](JDBCType.BIGINT, handleDefaultForNull = true)((rs, i, _) => rs.getLong(i), (rs, i, _) => rs.getLong(i), v => (ps, i, _) => ps.setLong(i, v))
  val float = asIsWithAccessors[Float](JDBCType.REAL, handleDefaultForNull = true)((rs, i, _) => rs.getFloat(i), (rs, i, _) => rs.getFloat(i), v => (ps, i, _) => ps.setFloat(i, v))
  val double = asIsWithAccessors[Double](JDBCType.DOUBLE, handleDefaultForNull = true)((rs, i, _) => rs.getDouble(i), (rs, i, _) => rs.getDouble(i), v => (ps, i, _) => ps.setDouble(i, v))
  val char = asIsWithAccessors[Char](JDBCType.CHAR)((rs, i, _) => rs.getString(i).head, (rs, i, _) => rs.getString(i).head, v => (ps, i, _) => ps.setString(i, String.valueOf(v)))
  val boolean = asIsWithAccessors[Boolean](JDBCType.BOOLEAN, handleDefaultForNull = true)((rs, i, _) => rs.getBoolean(i), (rs, i, _) => rs.getBoolean(i), v => (ps, i, _) => ps.setBoolean(i, v))
  val string = asIsWithAccessors[String](JDBCType.VARCHAR)((rs, i, _) => rs.getString(i), (rs, i, _) => rs.getString(i), v => (ps, i, _) => ps.setString(i, v))
  val bigDecimal = asIsWithAccessors[BigDecimal](JDBCType.NUMERIC)(
    (rs, index, _) => {
      val res = rs.getBigDecimal(index)
      if (res != null)
        math.BigDecimal.javaBigDecimal2bigDecimal(res)
      else
        null
    },
    (rs, label, _) => {
      val res = rs.getBigDecimal(label)
      if (res != null)
        math.BigDecimal.javaBigDecimal2bigDecimal(res)
      else
        null
    },
    v => (ps, i, _) => ps.setBigDecimal(i, v.bigDecimal)
  )
  val bigInt = bigDecimal.xmap[BigInt](_.toBigInt(), BigDecimal.apply)
  val bytes = asIsWithAccessors[Array[Byte]](JDBCType.BINARY)((rs, i, _) => rs.getBytes(i), (rs, i, _) => rs.getBytes(i), v => (ps, i, _) => ps.setBytes(i, v))

  // FIXME: Candidates for removal because it's unlikely people will use java.sql types since they've already chosen Scalikejdbc to avoid JDBC :)
  val sqlArray = asIsWithAccessors[java.sql.Array](JDBCType.ARRAY)((rs, i, _) => rs.getArray(i), (rs, i, _) => rs.getArray(i), v => (ps, i, _) => ps.setArray(i, v))
  val sqlDate = asIsWithAccessors[java.sql.Date](JDBCType.DATE)((rs, i, _) => rs.getDate(i), (rs, i, _) => rs.getDate(i), v => (ps, i, _) => ps.setDate(i, v))
  val sqlXml = asIsWithAccessors[java.sql.SQLXML](JDBCType.SQLXML)((rs, i, _) => rs.getSQLXML(i), (rs, i, _) => rs.getSQLXML(i), v => (ps, i, _) => ps.setSQLXML(i, v))
  val sqlTime = asIsWithAccessors[java.sql.Time](JDBCType.TIME)((rs, i, _) => rs.getTime(i), (rs, i, _) => rs.getTime(i), v => (ps, i, _) => ps.setTime(i, v))
  val sqlTimestamp = asIsWithAccessors[java.sql.Timestamp](JDBCType.TIMESTAMP)((rs, i, _) => rs.getTimestamp(i), (rs, i, _) => rs.getTimestamp(i), v => (ps, i, _) => ps.setTimestamp(i, v))
  val blob = asIsWithAccessors[java.sql.Blob](JDBCType.BLOB)((rs, i, _) => rs.getBlob(i), (rs, i, _) => rs.getBlob(i), v => (ps, i, _) => ps.setBlob(i, v))
  val clob = asIsWithAccessors[java.sql.Clob](JDBCType.CLOB)((rs, i, _) => rs.getClob(i), (rs, i, _) => rs.getClob(i), v => (ps, i, _) => ps.setClob(i, v))
  val nClob = asIsWithAccessors[java.sql.NClob](JDBCType.NCLOB)((rs, i, _) => rs.getNClob(i), (rs, i, _) => rs.getNClob(i), v => (ps, i, _) => ps.setNClob(i, v))
  val ref = asIsWithAccessors[java.sql.Ref](JDBCType.REF)((rs, i, _) => rs.getRef(i), (rs, i, _) => rs.getRef(i), v => (ps, i, _) => ps.setRef(i, v))
  val rowId = asIsWithAccessors[java.sql.RowId](JDBCType.ROWID)((rs, i, _) => rs.getRowId(i), (rs, i, _) => rs.getRowId(i), v => (ps, i, _) => ps.setRowId(i, v))

  /*
  // FIXME: Not sure if we need java.io classes here. Maybe we should Scala counterparts instead.
  // FIXME: Plus `java.io.Reader` can be written/read using many ways: getCharacterStream/getBinaryStream/getAsciiStream
  // FIXME: Same goes for `java.io.Reader`
  val characterStream: Binders[java.io.Reader] = asIsWithAccessors(JDBCType.LONGVARCHAR) (
    (rs, index, _) => rs.getCharacterStream(index),
    (rs, label, _) => rs.getCharacterStream(label),
    value => (ps, index, _) => ps.setCharacterStream(index, value) )
  val asciiStream: Binders[java.io.InputStream] = asIsWithAccessors(JDBCType.LONGVARCHAR) (
    (rs, index, _) => rs.getAsciiStream(index),
    (rs, label, _) => rs.getAsciiStream(label),
    value => (ps, index, _) => ps.setAsciiStream(index, value) )
  val binaryStream: Binders[java.io.InputStream] = asIsWithAccessors(JDBCType.LONGVARBINARY) (
    (rs, index, _) => rs.getBinaryStream(index),
    (rs, label, _) => rs.getBinaryStream(label),
    value => (ps, index, _) => ps.setBinaryStream(index, value) )
  val nString: Binders[String] = asIsWithAccessors(JDBCType.NVARCHAR) (
    (rs, index, _) => rs.getNString(index),
    (rs, label, _) => rs.getNString(label),
    value => (ps, index, _) => ps.setNString(index, value) )
  // FIXME: No need to implement Binder for java.net.URL as implementation differs from vendor to vendor - Postgresql throws an exception
  // FIXME: in setURL, mysql converts it to setString. It should be up to developer how he wants to serialize it.
  val url: Binders[java.net.URL] = asIsWithAccessors(JDBCType.DATALINK) (
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

  // FIXME: Simplify this horror
  def optionReaderBinder[T](implicit binder: TypeBinder[T]): TypeBinder[Option[T]] = new TypeBinder[Option[T]] {
    @inline
    def wrap(rs: ResultSet, block: => T): Option[T] =
      try {
        // Some SQL types have default values when row has NULL. For example `false` for Boolean, 0 for Byte and etc.
        // To be able to distinguish between real value and default value in *opt() getters we do extra check.
        val result = block
//        println(s">>> result - $result")
        if (binder.handleDefaultForNull && rs.wasNull())
          None
        else
          Option(result)
      } catch {
        case _: NullPointerException | _: UnexpectedNullValueException =>
          None
      }

    override def read(rs: ResultSet, columnIndex: Int): Option[T] =
      wrap(rs, binder.read(rs, columnIndex))
    override def read(rs: ResultSet, columnLabel: String): Option[T] =
      wrap(rs, binder.read(rs, columnLabel))
    override def fromSqlType(value: Any): Option[T] = if (value == null) None else Some(binder.fromSqlType(value))
    override val sqlType: SQLType = binder.sqlType
  }

  // FIXME: Simplify this horror
  def optionWriterBinder[T](implicit binder: ParameterBinderFactory[T]): ParameterBinderFactory[Option[T]] = new ParameterBinderFactory[Option[T]] {
    override def toSqlType(value: Option[T]): Any = value.fold(null.asInstanceOf[Any])(v => binder.toSqlType(v))
    override def apply(valueOpt: Option[T]): ParameterBinderWithValue[Option[T]] = new ParameterBinderWithValue[Option[T]] {
      override def value: Option[T] = valueOpt
      override def apply(stmt: PreparedStatement, idx: Int): Unit =
        // FIXME: Generally I think we should prohibit people from using `null` and not to handle it
        if (valueOpt == null)
          stmt.setObject(idx, null)
        else
          valueOpt.fold {
            stmt.setObject(idx, null)
          } { v =>
            val apply1: ParameterBinderWithValue[T] = binder.apply(v)
            apply1.apply(stmt, idx)
          }
    }

    override val sqlType: SQLType = binder.sqlType
  }
}
