package scalikejdbc

import java.time._
import java.sql.ResultSet
import java.util.Calendar
import collection.JavaConverters._

/**
 * java.sql.ResultSet wrapper.
 */
case class WrappedResultSet(
  underlying: ResultSet,
  cursor: ResultSetCursor,
  index: Int
) {

  def ensureCursor(): Unit = {
    if (cursor.position != index) {
      throw new IllegalStateException(
        ErrorMessage.INVALID_CURSOR_POSITION + " (actual:" + cursor.position + ",expected:" + index + ")"
      )
    }
  }

  /**
   * Throws ResultSetExtractorException if some exception is thrown.
   */
  private[this] def wrapIfError[A](op: => A): A = {
    try {
      op
    } catch {
      case e: Exception =>
        throw new ResultSetExtractorException(
          "Failed to retrieve value because " + e.getMessage + ". If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.",
          Some(e)
        )
    }
  }

  def array(columnIndex: Int): java.sql.Array = get[java.sql.Array](columnIndex)

  def array(columnLabel: String): java.sql.Array =
    get[java.sql.Array](columnLabel)

  def arrayOpt(columnIndex: Int): Option[java.sql.Array] =
    getOpt[java.sql.Array](columnIndex)

  def arrayOpt(columnLabel: String): Option[java.sql.Array] =
    getOpt[java.sql.Array](columnLabel)

  def asciiStream(columnIndex: Int): java.io.InputStream =
    get[java.io.InputStream](columnIndex)(TypeBinder.asciiStream)

  def asciiStream(columnLabel: String): java.io.InputStream =
    get[java.io.InputStream](columnLabel)(TypeBinder.asciiStream)

  def asciiStreamOpt(columnIndex: Int): Option[java.io.InputStream] = {
    implicit val binder = TypeBinder.asciiStream
    getOpt[java.io.InputStream](columnIndex)
  }

  def asciiStreamOpt(columnLabel: String): Option[java.io.InputStream] = {
    implicit val binder = TypeBinder.asciiStream
    getOpt[java.io.InputStream](columnLabel)
  }

  def bigDecimal(columnIndex: Int): java.math.BigDecimal =
    get[java.math.BigDecimal](columnIndex)

  def bigDecimal(columnLabel: String): java.math.BigDecimal =
    get[java.math.BigDecimal](columnLabel)

  def bigDecimalOpt(columnIndex: Int): Option[java.math.BigDecimal] =
    getOpt[java.math.BigDecimal](columnIndex)

  def bigDecimalOpt(columnLabel: String): Option[java.math.BigDecimal] =
    getOpt[java.math.BigDecimal](columnLabel)

  def bigInt(columnIndex: Int): java.math.BigInteger =
    get[java.math.BigInteger](columnIndex)

  def bigInt(columnLabel: String): java.math.BigInteger =
    get[java.math.BigInteger](columnLabel)

  def bigIntOpt(columnIndex: Int): Option[java.math.BigInteger] =
    getOpt[java.math.BigInteger](columnIndex)

  def bigIntOpt(columnLabel: String): Option[java.math.BigInteger] =
    getOpt[java.math.BigInteger](columnLabel)

  def binaryStream(columnIndex: Int): java.io.InputStream =
    get[java.io.InputStream](columnIndex)

  def binaryStream(columnLabel: String): java.io.InputStream =
    get[java.io.InputStream](columnLabel)

  def binaryStreamOpt(columnIndex: Int): Option[java.io.InputStream] =
    getOpt[java.io.InputStream](columnIndex)

  def binaryStreamOpt(columnLabel: String): Option[java.io.InputStream] =
    getOpt[java.io.InputStream](columnLabel)

  def blob(columnIndex: Int): java.sql.Blob = get[java.sql.Blob](columnIndex)

  def blob(columnLabel: String): java.sql.Blob = get[java.sql.Blob](columnLabel)

  def blobOpt(columnIndex: Int): Option[java.sql.Blob] =
    getOpt[java.sql.Blob](columnIndex)

  def blobOpt(columnLabel: String): Option[java.sql.Blob] =
    getOpt[java.sql.Blob](columnLabel)

  def nullableBoolean(columnIndex: Int): java.lang.Boolean =
    get[java.lang.Boolean](columnIndex)

  def nullableBoolean(columnLabel: String): java.lang.Boolean =
    get[java.lang.Boolean](columnLabel)

  def boolean(columnIndex: Int): Boolean = get[Boolean](columnIndex)

  def boolean(columnLabel: String): Boolean = get[Boolean](columnLabel)

  def booleanOpt(columnIndex: Int): Option[Boolean] =
    getOpt[Boolean](columnIndex)

  def booleanOpt(columnLabel: String): Option[Boolean] =
    getOpt[Boolean](columnLabel)

  def nullableByte(columnIndex: Int): java.lang.Byte =
    get[java.lang.Byte](columnIndex)

  def nullableByte(columnLabel: String): java.lang.Byte =
    get[java.lang.Byte](columnLabel)

  def byte(columnIndex: Int): Byte = get[Byte](columnIndex)

  def byte(columnLabel: String): Byte = get[Byte](columnLabel)

  def byteOpt(columnIndex: Int): Option[Byte] = getOpt[Byte](columnIndex)

  def byteOpt(columnLabel: String): Option[Byte] = getOpt[Byte](columnLabel)

  def bytes(columnIndex: Int): Array[Byte] = get[Array[Byte]](columnIndex)

  def bytes(columnLabel: String): Array[Byte] = get[Array[Byte]](columnLabel)

  def bytesOpt(columnIndex: Int): Option[Array[Byte]] =
    getOpt[Array[Byte]](columnIndex)

  def bytesOpt(columnLabel: String): Option[Array[Byte]] =
    getOpt[Array[Byte]](columnLabel)

  def characterStream(columnIndex: Int): java.io.Reader =
    get[java.io.Reader](columnIndex)

  def characterStream(columnLabel: String): java.io.Reader =
    get[java.io.Reader](columnLabel)

  def characterStreamOpt(columnIndex: Int): Option[java.io.Reader] =
    getOpt[java.io.Reader](columnIndex)

  def characterStreamOpt(columnLabel: String): Option[java.io.Reader] =
    getOpt[java.io.Reader](columnLabel)

  def clob(columnIndex: Int): java.sql.Clob = get[java.sql.Clob](columnIndex)

  def clob(columnLabel: String): java.sql.Clob = get[java.sql.Clob](columnLabel)

  def clobOpt(columnIndex: Int): Option[java.sql.Clob] =
    getOpt[java.sql.Clob](columnIndex)

  def clobOpt(columnLabel: String): Option[java.sql.Clob] =
    getOpt[java.sql.Clob](columnLabel)

  def concurrency: Int = {
    ensureCursor()
    underlying.getConcurrency
  }

  def cursorName: String = {
    ensureCursor()
    underlying.getCursorName
  }

  def date(columnIndex: Int): java.sql.Date = get[java.sql.Date](columnIndex)

  def date(columnLabel: String): java.sql.Date = get[java.sql.Date](columnLabel)

  def date(columnIndex: Int, cal: Calendar): java.sql.Date = {
    implicit val binder: TypeBinder[java.sql.Date] =
      TypeBinder((rs, i) => rs.getDate(i, cal))((rs, l) => rs.getDate(l, cal))
    get[java.sql.Date](columnIndex)
  }

  def date(columnLabel: String, cal: Calendar): java.sql.Date = {
    implicit val binder: TypeBinder[java.sql.Date] =
      TypeBinder((rs, i) => rs.getDate(i, cal))((rs, l) => rs.getDate(l, cal))
    get[java.sql.Date](columnLabel)
  }

  def dateOpt(columnIndex: Int): Option[java.sql.Date] =
    getOpt[java.sql.Date](columnIndex)

  def dateOpt(columnLabel: String): Option[java.sql.Date] =
    getOpt[java.sql.Date](columnLabel)

  def dateOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Date] = {
    implicit val binder: TypeBinder[java.sql.Date] =
      TypeBinder((rs, i) => rs.getDate(i, cal))((rs, l) => rs.getDate(l, cal))
    getOpt[java.sql.Date](columnIndex)
  }

  def dateOpt(columnLabel: String, cal: Calendar): Option[java.sql.Date] = {
    implicit val binder: TypeBinder[java.sql.Date] =
      TypeBinder((rs, i) => rs.getDate(i, cal))((rs, l) => rs.getDate(l, cal))
    getOpt[java.sql.Date](columnLabel)
  }

  def nullableDouble(columnIndex: Int): java.lang.Double =
    get[java.lang.Double](columnIndex)

  def nullableDouble(columnLabel: String): java.lang.Double =
    get[java.lang.Double](columnLabel)

  def double(columnIndex: Int): Double = get[Double](columnIndex)

  def double(columnLabel: String): Double = get[Double](columnLabel)

  def doubleOpt(columnIndex: Int): Option[Double] = getOpt[Double](columnIndex)

  def doubleOpt(columnLabel: String): Option[Double] =
    getOpt[Double](columnLabel)

  def fetchDirection: Int = {
    ensureCursor()
    underlying.getFetchDirection
  }

  def fetchSize: Int = {
    ensureCursor()
    underlying.getFetchSize
  }

  def nullableFloat(columnIndex: Int): java.lang.Float =
    get[java.lang.Float](columnIndex)

  def nullableFloat(columnLabel: String): java.lang.Float =
    get[java.lang.Float](columnLabel)

  def float(columnIndex: Int): Float = get[Float](columnIndex)

  def float(columnLabel: String): Float = get[Float](columnLabel)

  def floatOpt(columnIndex: Int): Option[Float] = getOpt[Float](columnIndex)

  def floatOpt(columnLabel: String): Option[Float] = getOpt[Float](columnLabel)

  def holdability: Int = {
    ensureCursor()
    underlying.getHoldability
  }

  def nullableInt(columnIndex: Int): java.lang.Integer =
    get[java.lang.Integer](columnIndex)

  def nullableInt(columnLabel: String): java.lang.Integer =
    get[java.lang.Integer](columnLabel)

  def int(columnIndex: Int): Int = get[Int](columnIndex)

  def int(columnLabel: String): Int = get[Int](columnLabel)

  def intOpt(columnIndex: Int): Option[Int] = getOpt[Int](columnIndex)

  def intOpt(columnLabel: String): Option[Int] = getOpt[Int](columnLabel)

  def nullableLong(columnIndex: Int): java.lang.Long =
    get[java.lang.Long](columnIndex)

  def nullableLong(columnLabel: String): java.lang.Long =
    get[java.lang.Long](columnLabel)

  def long(columnIndex: Int): Long = get[Long](columnIndex)

  def long(columnLabel: String): Long = get[Long](columnLabel)

  def longOpt(columnIndex: Int): Option[Long] = getOpt[Long](columnIndex)

  def longOpt(columnLabel: String): Option[Long] = getOpt[Long](columnLabel)

  def metaData: java.sql.ResultSetMetaData = {
    ensureCursor()
    underlying.getMetaData
  }

  def nCharacterStream(columnIndex: Int): java.io.Reader =
    get[java.io.Reader](columnIndex)(TypeBinder.nCharacterStream)

  def nCharacterStream(columnLabel: String): java.io.Reader =
    get[java.io.Reader](columnLabel)(TypeBinder.nCharacterStream)

  def nCharacterStreamOpt(columnIndex: Int): Option[java.io.Reader] = {
    implicit val binder = TypeBinder.nCharacterStream
    getOpt[java.io.Reader](columnIndex)
  }

  def nCharacterStreamOpt(columnLabel: String): Option[java.io.Reader] = {
    implicit val binder = TypeBinder.nCharacterStream
    getOpt[java.io.Reader](columnLabel)
  }

  def nClob(columnIndex: Int): java.sql.NClob = get[java.sql.NClob](columnIndex)

  def nClob(columnLabel: String): java.sql.NClob =
    get[java.sql.NClob](columnLabel)

  def nClobOpt(columnIndex: Int): Option[java.sql.NClob] =
    getOpt[java.sql.NClob](columnIndex)

  def nClobOpt(columnLabel: String): Option[java.sql.NClob] =
    getOpt[java.sql.NClob](columnLabel)

  def nString(columnIndex: Int): String =
    get[String](columnIndex)(TypeBinder.nString)

  def nString(columnLabel: String): String =
    get[String](columnLabel)(TypeBinder.nString)

  def nStringOpt(columnIndex: Int): Option[String] = {
    implicit val binder = TypeBinder.nString
    getOpt[String](columnIndex)
  }

  def nStringOpt(columnLabel: String): Option[String] = {
    implicit val binder = TypeBinder.nString
    getOpt[String](columnLabel)
  }

  def any(columnIndex: Int): Any = get[Any](columnIndex)(TypeBinder.any)

  def any(columnLabel: String): Any = get[Any](columnLabel)(TypeBinder.any)

  def any(columnIndex: Int, map: Map[String, Class[_]]): Any = {
    implicit val binder: TypeBinder[Any] =
      TypeBinder((rs, i) => rs.getObject(i, map.asJava))((rs, l) =>
        rs.getObject(l, map.asJava)
      )
    get[Any](columnIndex)
  }

  def any(columnLabel: String, map: Map[String, Class[_]]): Any = {
    implicit val binder: TypeBinder[Any] =
      TypeBinder((rs, i) => rs.getObject(i, map.asJava))((rs, l) =>
        rs.getObject(l, map.asJava)
      )
    get[Any](columnLabel)
  }

  def anyOpt(columnIndex: Int): Option[Any] = {
    implicit val binder: TypeBinder[Any] = TypeBinder.any
    getOpt[Any](columnIndex)(binder)
  }

  def anyOpt(columnLabel: String): Option[Any] = {
    implicit val binder: TypeBinder[Any] = TypeBinder.any
    getOpt[Any](columnLabel)(binder)
  }

  def anyOpt(columnIndex: Int, map: Map[String, Class[_]]): Option[Any] = {
    implicit val binder: TypeBinder[Any] =
      TypeBinder((rs, i) => rs.getObject(i, map.asJava))((rs, l) =>
        rs.getObject(l, map.asJava)
      )
    getOpt[Any](columnIndex)(binder)
  }

  def anyOpt(columnLabel: String, map: Map[String, Class[_]]): Option[Any] = {
    implicit val binder: TypeBinder[Any] =
      TypeBinder((rs, i) => rs.getObject(i, map.asJava))((rs, l) =>
        rs.getObject(l, map.asJava)
      )
    getOpt[Any](columnLabel)(binder)
  }

  def ref(columnIndex: Int): java.sql.Ref = get[java.sql.Ref](columnIndex)

  def ref(columnLabel: String): java.sql.Ref = get[java.sql.Ref](columnLabel)

  def refOpt(columnIndex: Int): Option[java.sql.Ref] =
    getOpt[java.sql.Ref](columnIndex)

  def refOpt(columnLabel: String): Option[java.sql.Ref] =
    getOpt[java.sql.Ref](columnLabel)

  def row: Int = {
    ensureCursor()
    underlying.getRow
  }

  def rowId(columnIndex: Int): java.sql.RowId = get[java.sql.RowId](columnIndex)

  def rowId(columnLabel: String): java.sql.RowId =
    get[java.sql.RowId](columnLabel)

  def nullableShort(columnIndex: Int): java.lang.Short =
    get[java.lang.Short](columnIndex)

  def nullableShort(columnLabel: String): java.lang.Short =
    get[java.lang.Short](columnLabel)

  def short(columnIndex: Int): Short = get[Short](columnIndex)

  def short(columnLabel: String): Short = get[Short](columnLabel)

  def shortOpt(columnIndex: Int): Option[Short] = getOpt[Short](columnIndex)

  def shortOpt(columnLabel: String): Option[Short] = getOpt[Short](columnLabel)

  def sqlXml(columnIndex: Int): java.sql.SQLXML =
    get[java.sql.SQLXML](columnIndex)

  def sqlXml(columnLabel: String): java.sql.SQLXML =
    get[java.sql.SQLXML](columnLabel)

  def sqlXmlOpt(columnIndex: Int): Option[java.sql.SQLXML] =
    getOpt[java.sql.SQLXML](columnIndex)

  def sqlXmlOpt(columnLabel: String): Option[java.sql.SQLXML] =
    getOpt[java.sql.SQLXML](columnLabel)

  def statement: java.sql.Statement = {
    ensureCursor()
    underlying.getStatement
  }

  def string(columnIndex: Int): String = get[String](columnIndex)

  def string(columnLabel: String): String = get[String](columnLabel)

  def stringOpt(columnIndex: Int): Option[String] = getOpt[String](columnIndex)

  def stringOpt(columnLabel: String): Option[String] =
    getOpt[String](columnLabel)

  def time(columnIndex: Int): java.sql.Time = get[java.sql.Time](columnIndex)

  def time(columnLabel: String): java.sql.Time = get[java.sql.Time](columnLabel)

  def time(columnIndex: Int, cal: Calendar): java.sql.Time = {
    implicit val binder: TypeBinder[java.sql.Time] =
      TypeBinder((rs, i) => rs.getTime(i, cal))((rs, l) => rs.getTime(l, cal))
    get[java.sql.Time](columnIndex)
  }

  def time(columnLabel: String, cal: Calendar): java.sql.Time = {
    implicit val binder: TypeBinder[java.sql.Time] =
      TypeBinder((rs, i) => rs.getTime(i, cal))((rs, l) => rs.getTime(l, cal))
    get[java.sql.Time](columnLabel)
  }

  def timeOpt(columnIndex: Int): Option[java.sql.Time] =
    getOpt[java.sql.Time](columnIndex)

  def timeOpt(columnLabel: String): Option[java.sql.Time] =
    getOpt[java.sql.Time](columnLabel)

  def timeOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Time] = {
    implicit val binder: TypeBinder[java.sql.Time] =
      TypeBinder((rs, i) => rs.getTime(i, cal))((rs, l) => rs.getTime(l, cal))
    getOpt[java.sql.Time](columnIndex)
  }

  def timeOpt(columnLabel: String, cal: Calendar): Option[java.sql.Time] = {
    implicit val binder: TypeBinder[java.sql.Time] =
      TypeBinder((rs, i) => rs.getTime(i, cal))((rs, l) => rs.getTime(l, cal))
    getOpt[java.sql.Time](columnLabel)
  }

  def timestamp(columnIndex: Int): java.sql.Timestamp =
    get[java.sql.Timestamp](columnIndex)

  def timestamp(columnLabel: String): java.sql.Timestamp =
    get[java.sql.Timestamp](columnLabel)

  def timestamp(columnIndex: Int, cal: Calendar): java.sql.Timestamp = {
    implicit val binder: TypeBinder[java.sql.Timestamp] =
      TypeBinder((rs, i) => rs.getTimestamp(i, cal))((rs, l) =>
        rs.getTimestamp(l, cal)
      )
    get[java.sql.Timestamp](columnIndex)
  }

  def timestamp(columnLabel: String, cal: Calendar): java.sql.Timestamp = {
    implicit val binder: TypeBinder[java.sql.Timestamp] =
      TypeBinder((rs, i) => rs.getTimestamp(i, cal))((rs, l) =>
        rs.getTimestamp(l, cal)
      )
    get[java.sql.Timestamp](columnLabel)
  }

  def timestampOpt(columnIndex: Int): Option[java.sql.Timestamp] =
    getOpt[java.sql.Timestamp](columnIndex)

  def timestampOpt(columnLabel: String): Option[java.sql.Timestamp] =
    getOpt[java.sql.Timestamp](columnLabel)

  def timestampOpt(
    columnIndex: Int,
    cal: Calendar
  ): Option[java.sql.Timestamp] = {
    implicit val binder: TypeBinder[java.sql.Timestamp] =
      TypeBinder((rs, i) => rs.getTimestamp(i, cal))((rs, l) =>
        rs.getTimestamp(l, cal)
      )
    getOpt[java.sql.Timestamp](columnIndex)
  }

  def timestampOpt(
    columnLabel: String,
    cal: Calendar
  ): Option[java.sql.Timestamp] = {
    implicit val binder: TypeBinder[java.sql.Timestamp] =
      TypeBinder((rs, i) => rs.getTimestamp(i, cal))((rs, l) =>
        rs.getTimestamp(l, cal)
      )
    getOpt[java.sql.Timestamp](columnLabel)
  }

  def url(columnIndex: Int): java.net.URL = get[java.net.URL](columnIndex)

  def url(columnLabel: String): java.net.URL = get[java.net.URL](columnLabel)

  def urlOpt(columnIndex: Int): Option[java.net.URL] =
    getOpt[java.net.URL](columnIndex)

  def urlOpt(columnLabel: String): Option[java.net.URL] =
    getOpt[java.net.URL](columnLabel)

  def dateTime(columnIndex: Int): ZonedDateTime = zonedDateTime(columnIndex)
  def dateTime(columnLabel: String): ZonedDateTime = zonedDateTime(columnLabel)

  def zonedDateTime(columnIndex: Int): ZonedDateTime =
    get[ZonedDateTime](columnIndex)
  def zonedDateTime(columnLabel: String): ZonedDateTime =
    get[ZonedDateTime](columnLabel)

  def offsetDateTime(columnIndex: Int): OffsetDateTime =
    get[OffsetDateTime](columnIndex)
  def offsetDateTime(columnLabel: String): OffsetDateTime =
    get[OffsetDateTime](columnLabel)

  def localDate(columnIndex: Int): LocalDate = get[LocalDate](columnIndex)
  def localDate(columnLabel: String): LocalDate = get[LocalDate](columnLabel)

  def localTime(columnIndex: Int): LocalTime = get[LocalTime](columnIndex)
  def localTime(columnLabel: String): LocalTime = get[LocalTime](columnLabel)

  def localDateTime(columnIndex: Int): LocalDateTime =
    get[LocalDateTime](columnIndex)
  def localDateTime(columnLabel: String): LocalDateTime =
    get[LocalDateTime](columnLabel)

  def dateTimeOpt(columnIndex: Int): Option[ZonedDateTime] = zonedDateTimeOpt(
    columnIndex
  )
  def dateTimeOpt(columnLabel: String): Option[ZonedDateTime] =
    zonedDateTimeOpt(columnLabel)

  def zonedDateTimeOpt(columnIndex: Int): Option[ZonedDateTime] =
    getOpt[ZonedDateTime](columnIndex)
  def zonedDateTimeOpt(columnLabel: String): Option[ZonedDateTime] =
    getOpt[ZonedDateTime](columnLabel)

  def offsetDateTimeOpt(columnIndex: Int): Option[OffsetDateTime] =
    getOpt[OffsetDateTime](columnIndex)
  def offsetDateTimeOpt(columnLabel: String): Option[OffsetDateTime] =
    getOpt[OffsetDateTime](columnLabel)

  def localDateOpt(columnIndex: Int): Option[LocalDate] =
    getOpt[LocalDate](columnIndex)
  def localDateOpt(columnLabel: String): Option[LocalDate] =
    getOpt[LocalDate](columnLabel)

  def localTimeOpt(columnIndex: Int): Option[LocalTime] =
    getOpt[LocalTime](columnIndex)
  def localTimeOpt(columnLabel: String): Option[LocalTime] =
    getOpt[LocalTime](columnLabel)

  def localDateTimeOpt(columnIndex: Int): Option[LocalDateTime] =
    getOpt[LocalDateTime](columnIndex)
  def localDateTimeOpt(columnLabel: String): Option[LocalDateTime] =
    getOpt[LocalDateTime](columnLabel)

  def warnings: java.sql.SQLWarning = {
    ensureCursor()
    underlying.getWarnings
  }

  def toMap(): Map[String, Any] = (1 to metaData.getColumnCount)
    .foldLeft(Map.newBuilder[String, Any]) { (result, i) =>
      val label = metaData.getColumnLabel(i)
      Option(any(label))
        .map { value => result += (label -> value) }
        .getOrElse(result)
    }
    .result()

  def toSymbolMap(): Map[Symbol, Any] = toMap().map { case (k, v) =>
    Symbol(k) -> v
  }

  def get[A: TypeBinder](columnIndex: Int): A = {
    ensureCursor()
    wrapIfError(implicitly[TypeBinder[A]].apply(underlying, columnIndex))
  }

  def get[A: TypeBinder](columnLabel: String): A = {
    ensureCursor()
    wrapIfError(implicitly[TypeBinder[A]].apply(underlying, columnLabel))
  }

  def getOpt[A: TypeBinder](columnIndex: Int): Option[A] =
    get[Option[A]](columnIndex)

  def getOpt[A: TypeBinder](columnLabel: String): Option[A] =
    get[Option[A]](columnLabel)

}
