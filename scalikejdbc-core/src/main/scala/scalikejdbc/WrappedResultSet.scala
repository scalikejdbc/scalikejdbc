package scalikejdbc

import java.sql.{ JDBCType, ResultSet }
import java.util.Calendar
import org.joda.time._
import collection.JavaConverters._

/**
 * java.sql.ResultSet wrapper.
 */
case class WrappedResultSet(underlying: ResultSet, cursor: ResultSetCursor, index: Int) {

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
      case e: Exception => throw ResultSetExtractorException(
        "Failed to retrieve value because " + e.getMessage + ". If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.", Some(e)
      )
    }
  }

  // FIXME: Candidates for removal
  def array(columnIndex: Int): java.sql.Array = get[java.sql.Array](columnIndex)
  def array(columnLabel: String): java.sql.Array = get[java.sql.Array](columnLabel)
  def arrayOpt(columnIndex: Int): Option[java.sql.Array] = getOpt[java.sql.Array](columnIndex)
  def arrayOpt(columnLabel: String): Option[java.sql.Array] = getOpt[java.sql.Array](columnLabel)
  def blob(columnIndex: Int): java.sql.Blob = get[java.sql.Blob](columnIndex)
  def blob(columnLabel: String): java.sql.Blob = get[java.sql.Blob](columnLabel)
  def blobOpt(columnIndex: Int): Option[java.sql.Blob] = getOpt[java.sql.Blob](columnIndex)
  def blobOpt(columnLabel: String): Option[java.sql.Blob] = getOpt[java.sql.Blob](columnLabel)
  def clob(columnIndex: Int): java.sql.Clob = get[java.sql.Clob](columnIndex)
  def clob(columnLabel: String): java.sql.Clob = get[java.sql.Clob](columnLabel)
  def clobOpt(columnIndex: Int): Option[java.sql.Clob] = getOpt[java.sql.Clob](columnIndex)
  def clobOpt(columnLabel: String): Option[java.sql.Clob] = getOpt[java.sql.Clob](columnLabel)

  private def getSqlDateWithCalBinder(cal: Calendar) = TypeBinder(JDBCType.DATE, _.asInstanceOf[java.sql.Date])((rs, i, _) => rs.getDate(i, cal), (rs, l, _) => rs.getDate(l, cal))
  def date(columnIndex: Int): java.sql.Date = get[java.sql.Date](columnIndex)
  def date(columnLabel: String): java.sql.Date = get[java.sql.Date](columnLabel)
  def date(columnIndex: Int, cal: Calendar): java.sql.Date = get[java.sql.Date](columnIndex)(getSqlDateWithCalBinder(cal))
  def date(columnLabel: String, cal: Calendar): java.sql.Date = get[java.sql.Date](columnLabel)(getSqlDateWithCalBinder(cal))
  def dateOpt(columnIndex: Int): Option[java.sql.Date] = getOpt[java.sql.Date](columnIndex)
  def dateOpt(columnLabel: String): Option[java.sql.Date] = getOpt[java.sql.Date](columnLabel)
  def dateOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Date] = getOpt[java.sql.Date](columnIndex)(TypeBinder.option(getSqlDateWithCalBinder(cal)))
  def dateOpt(columnLabel: String, cal: Calendar): Option[java.sql.Date] = getOpt[java.sql.Date](columnLabel)(TypeBinder.option(getSqlDateWithCalBinder(cal)))

  def nClob(columnIndex: Int): java.sql.NClob = get[java.sql.NClob](columnIndex)
  def nClob(columnLabel: String): java.sql.NClob = get[java.sql.NClob](columnLabel)
  def nClobOpt(columnIndex: Int): Option[java.sql.NClob] = getOpt[java.sql.NClob](columnIndex)
  def nClobOpt(columnLabel: String): Option[java.sql.NClob] = getOpt[java.sql.NClob](columnLabel)

  def bigDecimal(columnIndex: Int): BigDecimal = get[BigDecimal](columnIndex)

  def bigDecimal(columnLabel: String): BigDecimal = get[BigDecimal](columnLabel)

  def bigDecimalOpt(columnIndex: Int): Option[BigDecimal] = getOpt[BigDecimal](columnIndex)

  def bigDecimalOpt(columnLabel: String): Option[BigDecimal] = getOpt[BigDecimal](columnLabel)

  def bigInt(columnIndex: Int): BigInt = get[BigInt](columnIndex)

  def bigInt(columnLabel: String): BigInt = get[BigInt](columnLabel)

  def bigIntOpt(columnIndex: Int): Option[BigInt] = getOpt[BigInt](columnIndex)

  def bigIntOpt(columnLabel: String): Option[BigInt] = getOpt[BigInt](columnLabel)

  def boolean(columnIndex: Int): Boolean = get[Boolean](columnIndex)

  def boolean(columnLabel: String): Boolean = get[Boolean](columnLabel)

  def booleanOpt(columnIndex: Int): Option[Boolean] = getOpt[Boolean](columnIndex)

  def booleanOpt(columnLabel: String): Option[Boolean] = getOpt[Boolean](columnLabel)

  def byte(columnIndex: Int): Byte = get[Byte](columnIndex)

  def byte(columnLabel: String): Byte = get[Byte](columnLabel)

  def byteOpt(columnIndex: Int): Option[Byte] = getOpt[Byte](columnIndex)

  def byteOpt(columnLabel: String): Option[Byte] = getOpt[Byte](columnLabel)

  def bytes(columnIndex: Int): Array[Byte] = get[Array[Byte]](columnIndex)

  def bytes(columnLabel: String): Array[Byte] = get[Array[Byte]](columnLabel)

  def bytesOpt(columnIndex: Int): Option[Array[Byte]] = getOpt[Array[Byte]](columnIndex)

  def bytesOpt(columnLabel: String): Option[Array[Byte]] = getOpt[Array[Byte]](columnLabel)

  def concurrency: Int = {
    ensureCursor()
    underlying.getConcurrency
  }

  def cursorName: String = {
    ensureCursor()
    underlying.getCursorName
  }

  def double(columnIndex: Int): Double = get[Double](columnIndex)

  def double(columnLabel: String): Double = get[Double](columnLabel)

  def doubleOpt(columnIndex: Int): Option[Double] = getOpt[Double](columnIndex)

  def doubleOpt(columnLabel: String): Option[Double] = getOpt[Double](columnLabel)

  def fetchDirection: Int = {
    ensureCursor()
    underlying.getFetchDirection
  }

  def fetchSize: Int = {
    ensureCursor()
    underlying.getFetchSize
  }

  def float(columnIndex: Int): Float = get[Float](columnIndex)

  def float(columnLabel: String): Float = get[Float](columnLabel)

  def floatOpt(columnIndex: Int): Option[Float] = getOpt[Float](columnIndex)

  def floatOpt(columnLabel: String): Option[Float] = getOpt[Float](columnLabel)

  def holdability: Int = {
    ensureCursor()
    underlying.getHoldability
  }

  def int(columnIndex: Int): Int = get[Int](columnIndex)

  def int(columnLabel: String): Int = get[Int](columnLabel)

  def intOpt(columnIndex: Int): Option[Int] = getOpt[Int](columnIndex)

  def intOpt(columnLabel: String): Option[Int] = getOpt[Int](columnLabel)

  def long(columnIndex: Int): Long = get[Long](columnIndex)

  def long(columnLabel: String): Long = get[Long](columnLabel)

  def longOpt(columnIndex: Int): Option[Long] = getOpt[Long](columnIndex)

  def longOpt(columnLabel: String): Option[Long] = getOpt[Long](columnLabel)

  def metaData: java.sql.ResultSetMetaData = {
    ensureCursor()
    underlying.getMetaData
  }

  def any(columnIndex: Int): Any = get[Any](columnIndex)(Binders.any)

  def any(columnLabel: String): Any = get[Any](columnLabel)(Binders.any)

  private def anyWithClassMapBinder(map: Map[String, Class[_]]) = TypeBinder(JDBCType.OTHER, identity)((rs, i, _) => rs.getObject(i, map.asJava), (rs, l, _) => rs.getObject(l, map.asJava))

  def any(columnIndex: Int, map: Map[String, Class[_]]): Any =
    get[Any](columnIndex)(anyWithClassMapBinder(map))

  def any(columnLabel: String, map: Map[String, Class[_]]): Any =
    get[Any](columnLabel)(anyWithClassMapBinder(map))

  def anyOpt(columnIndex: Int): Option[Any] = {
    get[Option[Any]](columnIndex)(TypeBinder.option(Binders.any))
  }

  def anyOpt(columnLabel: String): Option[Any] = {
    get[Option[Any]](columnLabel)(TypeBinder.option(Binders.any))
  }

  def anyOpt(columnIndex: Int, map: Map[String, Class[_]]): Option[Any] = {
    get[Option[Any]](columnIndex)(Binders.optionReaderBinder(anyWithClassMapBinder(map)))
  }

  def anyOpt(columnLabel: String, map: Map[String, Class[_]]): Option[Any] = {
    get[Option[Any]](columnLabel)(Binders.optionReaderBinder(anyWithClassMapBinder(map)))
  }

  def ref(columnIndex: Int): java.sql.Ref = get[java.sql.Ref](columnIndex)

  def ref(columnLabel: String): java.sql.Ref = get[java.sql.Ref](columnLabel)

  def refOpt(columnIndex: Int): Option[java.sql.Ref] = getOpt[java.sql.Ref](columnIndex)

  def refOpt(columnLabel: String): Option[java.sql.Ref] = getOpt[java.sql.Ref](columnLabel)

  def row: Int = {
    ensureCursor()
    underlying.getRow
  }

  def rowId(columnIndex: Int): java.sql.RowId = get[java.sql.RowId](columnIndex)

  def rowId(columnLabel: String): java.sql.RowId = get[java.sql.RowId](columnLabel)

  def short(columnIndex: Int): Short = get[Short](columnIndex)

  def short(columnLabel: String): Short = get[Short](columnLabel)

  def shortOpt(columnIndex: Int): Option[Short] = getOpt[Short](columnIndex)

  def shortOpt(columnLabel: String): Option[Short] = getOpt[Short](columnLabel)

  def sqlXml(columnIndex: Int): java.sql.SQLXML = get[java.sql.SQLXML](columnIndex)

  def sqlXml(columnLabel: String): java.sql.SQLXML = get[java.sql.SQLXML](columnLabel)

  def sqlXmlOpt(columnIndex: Int): Option[java.sql.SQLXML] = getOpt[java.sql.SQLXML](columnIndex)

  def sqlXmlOpt(columnLabel: String): Option[java.sql.SQLXML] = getOpt[java.sql.SQLXML](columnLabel)

  def statement: java.sql.Statement = {
    ensureCursor()
    underlying.getStatement
  }

  def string(columnIndex: Int): String = get[String](columnIndex)

  def string(columnLabel: String): String = get[String](columnLabel)

  def stringOpt(columnIndex: Int): Option[String] = getOpt[String](columnIndex)

  def stringOpt(columnLabel: String): Option[String] = getOpt[String](columnLabel)

  def time(columnIndex: Int): java.sql.Time = get[java.sql.Time](columnIndex)

  def time(columnLabel: String): java.sql.Time = get[java.sql.Time](columnLabel)

  private def getSqlTimeWithCalBinder(cal: Calendar) = TypeBinder(JDBCType.TIME, _.asInstanceOf[java.sql.Time])((rs, i, _) => rs.getTime(i, cal), (rs, l, _) => rs.getTime(l, cal))

  def time(columnIndex: Int, cal: Calendar): java.sql.Time = {
    get[java.sql.Time](columnIndex)(getSqlTimeWithCalBinder(cal))
  }

  def time(columnLabel: String, cal: Calendar): java.sql.Time = {
    get[java.sql.Time](columnLabel)(getSqlTimeWithCalBinder(cal))
  }

  def timeOpt(columnIndex: Int): Option[java.sql.Time] = getOpt[java.sql.Time](columnIndex)

  def timeOpt(columnLabel: String): Option[java.sql.Time] = getOpt[java.sql.Time](columnLabel)

  def timeOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Time] = {
    getOpt[java.sql.Time](columnIndex)(TypeBinder.option(getSqlTimeWithCalBinder(cal)))
  }

  def timeOpt(columnLabel: String, cal: Calendar): Option[java.sql.Time] = {
    getOpt[java.sql.Time](columnLabel)(TypeBinder.option(getSqlTimeWithCalBinder(cal)))
  }

  def timestamp(columnIndex: Int): java.sql.Timestamp = get[java.sql.Timestamp](columnIndex)

  def timestamp(columnLabel: String): java.sql.Timestamp = get[java.sql.Timestamp](columnLabel)

  private def getSqlTimestampWithCalBinder(cal: Calendar) = TypeBinder(JDBCType.TIMESTAMP, _.asInstanceOf[java.sql.Timestamp])((rs, i, _) => rs.getTimestamp(i, cal), (rs, l, _) => rs.getTimestamp(l, cal))

  def timestamp(columnIndex: Int, cal: Calendar): java.sql.Timestamp = {
    get[java.sql.Timestamp](columnIndex)(getSqlTimestampWithCalBinder(cal))
  }

  def timestamp(columnLabel: String, cal: Calendar): java.sql.Timestamp = {
    get[java.sql.Timestamp](columnLabel)(getSqlTimestampWithCalBinder(cal))
  }

  def jodaDateTime(columnIndex: Int): DateTime = get[DateTime](columnIndex)
  def jodaDateTime(columnLabel: String): DateTime = get[DateTime](columnLabel)

  def jodaLocalDate(columnIndex: Int): LocalDate = get[LocalDate](columnIndex)
  def jodaLocalDate(columnLabel: String): LocalDate = get[LocalDate](columnLabel)

  def jodaLocalTime(columnIndex: Int): LocalTime = get[LocalTime](columnIndex)
  def jodaLocalTime(columnLabel: String): LocalTime = get[LocalTime](columnLabel)

  def jodaLocalDateTime(columnIndex: Int): LocalDateTime = get[LocalDateTime](columnIndex)
  def jodaLocalDateTime(columnLabel: String): LocalDateTime = get[LocalDateTime](columnLabel)

  def timestampOpt(columnIndex: Int): Option[java.sql.Timestamp] = getOpt[java.sql.Timestamp](columnIndex)

  def timestampOpt(columnLabel: String): Option[java.sql.Timestamp] = getOpt[java.sql.Timestamp](columnLabel)

  def timestampOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Timestamp] = {
    getOpt[java.sql.Timestamp](columnIndex)(TypeBinder.option(getSqlTimestampWithCalBinder(cal)))
  }

  def timestampOpt(columnLabel: String, cal: Calendar): Option[java.sql.Timestamp] = {
    getOpt[java.sql.Timestamp](columnLabel)(TypeBinder.option(getSqlTimestampWithCalBinder(cal)))
  }

  def jodaDateTimeOpt(columnIndex: Int): Option[DateTime] = getOpt[DateTime](columnIndex)
  def jodaDateTimeOpt(columnLabel: String): Option[DateTime] = getOpt[DateTime](columnLabel)

  def jodaLocalDateOpt(columnIndex: Int): Option[LocalDate] = getOpt[LocalDate](columnIndex)
  def jodaLocalDateOpt(columnLabel: String): Option[LocalDate] = getOpt[LocalDate](columnLabel)

  def jodaLocalTimeOpt(columnIndex: Int): Option[LocalTime] = getOpt[LocalTime](columnIndex)
  def jodaLocalTimeOpt(columnLabel: String): Option[LocalTime] = getOpt[LocalTime](columnLabel)

  def jodaLocalDateTimeOpt(columnIndex: Int): Option[LocalDateTime] = getOpt[LocalDateTime](columnIndex)
  def jodaLocalDateTimeOpt(columnLabel: String): Option[LocalDateTime] = getOpt[LocalDateTime](columnLabel)

  def warnings: java.sql.SQLWarning = {
    ensureCursor()
    underlying.getWarnings
  }

  def toMap(): Map[String, Any] = (1 to metaData.getColumnCount).foldLeft(Map[String, Any]()) { (result, i) =>
    val label = metaData.getColumnLabel(i)
    Option(any(label)).map { value => result + (label -> value) }.getOrElse(result)
  }

  def toSymbolMap(): Map[Symbol, Any] = toMap().map { case (k, v) => Symbol(k) -> v }

  def get[A: TypeBinder](columnIndex: Int): A = {
    ensureCursor()
    val result = wrapIfError(implicitly[TypeBinder[A]].read(underlying, columnIndex))
    if (result == null)
      throw ResultSetExtractorException(s"NULL was returned for column `$columnIndex`. If it's intentional use *Opt() version of getter.")
    result
  }

  def getOpt[A](columnIndex: Int)(implicit binder: TypeBinder[Option[A]]): Option[A] = {
    ensureCursor()
    wrapIfError(binder.read(underlying, columnIndex))
  }

  def get[A](columnLabel: String)(implicit binder: TypeBinder[A]): A = {
    ensureCursor()
    val result = wrapIfError(binder.read(underlying, columnLabel))
    if (result == null)
      throw ResultSetExtractorException(s"NULL was returned for column `$columnLabel`. If it's intentional use *Opt() version of getter.")
    result
  }

  def getOpt[A](columnLabel: String)(implicit binder: TypeBinder[Option[A]]): Option[A] = {
    ensureCursor()
    wrapIfError(binder.read(underlying, columnLabel))
  }

}

