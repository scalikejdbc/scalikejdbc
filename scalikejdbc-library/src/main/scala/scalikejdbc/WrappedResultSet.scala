/*
 * Copyright 2012 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import java.sql.ResultSet
import java.util.Calendar
import org.joda.time._
import collection.JavaConverters._

/**
 * [[java.sql.ResultSet]] wrapper
 */
case class WrappedResultSet(underlying: ResultSet, cursor: ResultSetCursor, index: Int) {

  def ensureCursor(): Unit = {
    if (cursor.position != index) {
      throw new IllegalStateException(
        ErrorMessage.INVALID_CURSOR_POSITION + " (actual:" + cursor.position + ",expected:" + index + ")")
    }
  }

  /**
   * Throws ResultSetExtractorException if some exception is thrown.
   */
  private[this] def wrapIfError[A](op: => A): A = {
    try {
      op
    } catch {
      case e: Exception => throw new ResultSetExtractorException(
        "Failed to retrieve value because " + e.getMessage + ". If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.", Some(e))
    }
  }

  def array(columnIndex: Int): java.sql.Array = {
    ensureCursor()
    wrapIfError(underlying.getArray(columnIndex))
  }

  def array(columnLabel: String): java.sql.Array = {
    ensureCursor()
    wrapIfError(underlying.getArray(columnLabel))
  }

  def arrayOpt(columnIndex: Int): Option[java.sql.Array] = opt[java.sql.Array](array(columnIndex))

  def arrayOpt(columnLabel: String): Option[java.sql.Array] = opt[java.sql.Array](array(columnLabel))

  def asciiStream(columnIndex: Int): java.io.InputStream = {
    ensureCursor()
    wrapIfError(underlying.getAsciiStream(columnIndex))
  }

  def asciiStream(columnLabel: String): java.io.InputStream = {
    ensureCursor()
    wrapIfError(underlying.getAsciiStream(columnLabel))
  }

  def asciiStreamOpt(columnIndex: Int): Option[java.io.InputStream] = opt[java.io.InputStream](asciiStream(columnIndex))

  def asciiStreamOpt(columnLabel: String): Option[java.io.InputStream] = opt[java.io.InputStream](asciiStream(columnLabel))

  def bigDecimal(columnIndex: Int): java.math.BigDecimal = {
    ensureCursor()
    wrapIfError(underlying.getBigDecimal(columnIndex))
  }

  def bigDecimal(columnLabel: String): java.math.BigDecimal = {
    ensureCursor()
    wrapIfError(underlying.getBigDecimal(columnLabel))
  }

  def bigDecimalOpt(columnIndex: Int): Option[java.math.BigDecimal] = opt[java.math.BigDecimal](bigDecimal(columnIndex))

  def bigDecimalOpt(columnLabel: String): Option[java.math.BigDecimal] = opt[java.math.BigDecimal](bigDecimal(columnLabel))

  def binaryStream(columnIndex: Int): java.io.InputStream = {
    ensureCursor()
    wrapIfError(underlying.getBinaryStream(columnIndex))
  }

  def binaryStream(columnLabel: String): java.io.InputStream = {
    ensureCursor()
    wrapIfError(underlying.getBinaryStream(columnLabel))
  }

  def binaryStreamOpt(columnIndex: Int): Option[java.io.InputStream] = opt[java.io.InputStream](binaryStream(columnIndex))

  def binaryStreamOpt(columnLabel: String): Option[java.io.InputStream] = opt[java.io.InputStream](binaryStream(columnLabel))

  def blob(columnIndex: Int): java.sql.Blob = {
    ensureCursor()
    wrapIfError(underlying.getBlob(columnIndex))
  }

  def blob(columnLabel: String): java.sql.Blob = {
    ensureCursor()
    wrapIfError(underlying.getBlob(columnLabel))
  }

  def blobOpt(columnIndex: Int): Option[java.sql.Blob] = opt[java.sql.Blob](blob(columnIndex))

  def blobOpt(columnLabel: String): Option[java.sql.Blob] = opt[java.sql.Blob](blob(columnLabel))

  def nullableBoolean(columnIndex: Int): java.lang.Boolean = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Boolean.valueOf(v.toString))
      .orNull[java.lang.Boolean]
  }

  def nullableBoolean(columnLabel: String): java.lang.Boolean = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Boolean.valueOf(v.toString))
      .orNull[java.lang.Boolean]
  }

  def boolean(columnIndex: Int): Boolean = nullableBoolean(columnIndex).asInstanceOf[Boolean]

  def boolean(columnLabel: String): Boolean = nullableBoolean(columnLabel).asInstanceOf[Boolean]

  def booleanOpt(columnIndex: Int): Option[Boolean] = opt[Boolean](nullableBoolean(columnIndex))

  def booleanOpt(columnLabel: String): Option[Boolean] = opt[Boolean](nullableBoolean(columnLabel))

  def nullableByte(columnIndex: Int): java.lang.Byte = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Byte.valueOf(v.toString))
      .orNull[java.lang.Byte]
  }

  def nullableByte(columnLabel: String): java.lang.Byte = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Byte.valueOf(v.toString))
      .orNull[java.lang.Byte]
  }

  def byte(columnIndex: Int): Byte = nullableByte(columnIndex).asInstanceOf[Byte]

  def byte(columnLabel: String): Byte = nullableByte(columnLabel).asInstanceOf[Byte]

  def byteOpt(columnIndex: Int): Option[Byte] = opt[Byte](nullableByte(columnIndex))

  def byteOpt(columnLabel: String): Option[Byte] = opt[Byte](nullableByte(columnLabel))

  def bytes(columnIndex: Int): Array[Byte] = {
    ensureCursor()
    wrapIfError(underlying.getBytes(columnIndex))
  }

  def bytes(columnLabel: String): Array[Byte] = {
    ensureCursor()
    wrapIfError(underlying.getBytes(columnLabel))
  }

  def bytesOpt(columnIndex: Int): Option[Array[Byte]] = opt[Array[Byte]](bytes(columnIndex))

  def bytesOpt(columnLabel: String): Option[Array[Byte]] = opt[Array[Byte]](bytes(columnLabel))

  def characterStream(columnIndex: Int): java.io.Reader = {
    ensureCursor()
    wrapIfError(underlying.getCharacterStream(columnIndex))
  }

  def characterStream(columnLabel: String): java.io.Reader = {
    ensureCursor()
    wrapIfError(underlying.getCharacterStream(columnLabel))
  }

  def characterStreamOpt(columnIndex: Int): Option[java.io.Reader] = opt[java.io.Reader](characterStream(columnIndex))

  def characterStreamOpt(columnLabel: String): Option[java.io.Reader] = opt[java.io.Reader](characterStream(columnLabel))

  def clob(columnIndex: Int): java.sql.Clob = {
    ensureCursor()
    wrapIfError(underlying.getClob(columnIndex))
  }

  def clob(columnLabel: String): java.sql.Clob = {
    ensureCursor()
    wrapIfError(underlying.getClob(columnLabel))
  }

  def clobOpt(columnIndex: Int): Option[java.sql.Clob] = opt[java.sql.Clob](clob(columnIndex))

  def clobOpt(columnLabel: String): Option[java.sql.Clob] = opt[java.sql.Clob](clob(columnLabel))

  def concurrency: Int = {
    ensureCursor()
    underlying.getConcurrency
  }

  def cursorName: String = {
    ensureCursor()
    underlying.getCursorName
  }

  def date(columnIndex: Int): java.sql.Date = {
    ensureCursor()
    wrapIfError(underlying.getDate(columnIndex))
  }

  def date(columnLabel: String): java.sql.Date = {
    ensureCursor()
    wrapIfError(underlying.getDate(columnLabel))
  }

  def date(columnIndex: Int, cal: Calendar): java.sql.Date = {
    ensureCursor()
    wrapIfError(underlying.getDate(columnIndex, cal))
  }

  def date(columnLabel: String, cal: Calendar): java.sql.Date = {
    ensureCursor()
    wrapIfError(underlying.getDate(columnLabel, cal))
  }

  def dateOpt(columnIndex: Int): Option[java.sql.Date] = opt[java.sql.Date](date(columnIndex))

  def dateOpt(columnLabel: String): Option[java.sql.Date] = opt[java.sql.Date](date(columnLabel))

  def dateOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Date] = opt[java.sql.Date](date(columnIndex, cal))

  def dateOpt(columnLabel: String, cal: Calendar): Option[java.sql.Date] = opt[java.sql.Date](date(columnLabel, cal))

  def nullableDouble(columnIndex: Int): java.lang.Double = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Double.valueOf(v.toString))
      .orNull[java.lang.Double]
  }

  def nullableDouble(columnLabel: String): java.lang.Double = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Double.valueOf(v.toString))
      .orNull[java.lang.Double]
  }

  def double(columnIndex: Int): Double = nullableDouble(columnIndex).asInstanceOf[Double]

  def double(columnLabel: String): Double = nullableDouble(columnLabel).asInstanceOf[Double]

  def doubleOpt(columnIndex: Int): Option[Double] = opt[Double](nullableDouble(columnIndex))

  def doubleOpt(columnLabel: String): Option[Double] = opt[Double](nullableDouble(columnLabel))

  def fetchDirection: Int = {
    ensureCursor()
    underlying.getFetchDirection
  }

  def fetchSize: Int = {
    ensureCursor()
    underlying.getFetchSize
  }

  def nullableFloat(columnIndex: Int): java.lang.Float = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Float.valueOf(v.toString))
      .orNull[java.lang.Float]
  }

  def nullableFloat(columnLabel: String): java.lang.Float = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Float.valueOf(v.toString))
      .orNull[java.lang.Float]
  }

  def float(columnIndex: Int): Float = nullableFloat(columnIndex).asInstanceOf[Float]

  def float(columnLabel: String): Float = nullableFloat(columnLabel).asInstanceOf[Float]

  def floatOpt(columnIndex: Int): Option[Float] = opt[Float](nullableFloat(columnIndex))

  def floatOpt(columnLabel: String): Option[Float] = opt[Float](nullableFloat(columnLabel))

  def holdability: Int = {
    ensureCursor()
    underlying.getHoldability
  }

  def nullableInt(columnIndex: Int): java.lang.Integer = {
    ensureCursor()
    Option(any(columnIndex)).map {
      case v: Float => v.toInt.asInstanceOf[java.lang.Integer]
      case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
      case v => java.lang.Integer.valueOf(v.toString)
    }.orNull[java.lang.Integer]
  }

  def nullableInt(columnLabel: String): java.lang.Integer = {
    ensureCursor()
    Option(any(columnLabel)).map {
      case v: Float => v.toInt.asInstanceOf[java.lang.Integer]
      case v: Double => v.toInt.asInstanceOf[java.lang.Integer]
      case v => java.lang.Integer.valueOf(v.toString)
    }.orNull[java.lang.Integer]
  }

  def int(columnIndex: Int): Int = nullableInt(columnIndex).asInstanceOf[Int]

  def int(columnLabel: String): Int = nullableInt(columnLabel).asInstanceOf[Int]

  def intOpt(columnIndex: Int): Option[Int] = opt[Int](nullableInt(columnIndex))

  def intOpt(columnLabel: String): Option[Int] = opt[Int](nullableInt(columnLabel))

  def nullableLong(columnIndex: Int): java.lang.Long = {
    ensureCursor()
    Option(any(columnIndex)).map {
      case v: Float => v.toLong.asInstanceOf[java.lang.Long]
      case v: Double => v.toLong.asInstanceOf[java.lang.Long]
      case v => java.lang.Long.valueOf(v.toString)
    }.orNull[java.lang.Long]
  }

  def nullableLong(columnLabel: String): java.lang.Long = {
    ensureCursor()
    Option(any(columnLabel)).map {
      case v: Float => v.toLong.asInstanceOf[java.lang.Long]
      case v: Double => v.toLong.asInstanceOf[java.lang.Long]
      case v => java.lang.Long.valueOf(v.toString)
    }.orNull[java.lang.Long]
  }

  def long(columnIndex: Int): Long = nullableLong(columnIndex).asInstanceOf[Long]

  def long(columnLabel: String): Long = nullableLong(columnLabel).asInstanceOf[Long]

  def longOpt(columnIndex: Int): Option[Long] = opt[Long](nullableLong(columnIndex))

  def longOpt(columnLabel: String): Option[Long] = opt[Long](nullableLong(columnLabel))

  def metaData: java.sql.ResultSetMetaData = {
    ensureCursor()
    underlying.getMetaData
  }

  def nCharacterStream(columnIndex: Int): java.io.Reader = {
    ensureCursor()
    wrapIfError(underlying.getNCharacterStream(columnIndex))
  }

  def nCharacterStream(columnLabel: String): java.io.Reader = {
    ensureCursor()
    wrapIfError(underlying.getNCharacterStream(columnLabel))
  }

  def nCharacterStreamOpt(columnIndex: Int): Option[java.io.Reader] = opt[java.io.Reader](nCharacterStream(columnIndex))

  def nCharacterStreamOpt(columnLabel: String): Option[java.io.Reader] = opt[java.io.Reader](nCharacterStream(columnLabel))

  def nClob(columnIndex: Int): java.sql.NClob = {
    ensureCursor()
    wrapIfError(underlying.getNClob(columnIndex))
  }

  def nClob(columnLabel: String): java.sql.NClob = {
    ensureCursor()
    wrapIfError(underlying.getNClob(columnLabel))
  }

  def nClobOpt(columnIndex: Int): Option[java.sql.NClob] = opt[java.sql.NClob](nClob(columnIndex))

  def nClobOpt(columnLabel: String): Option[java.sql.NClob] = opt[java.sql.NClob](nClob(columnLabel))

  def nString(columnIndex: Int): String = {
    ensureCursor()
    wrapIfError(underlying.getNString(columnIndex))
  }

  def nString(columnLabel: String): String = {
    ensureCursor()
    wrapIfError(underlying.getNString(columnLabel))
  }

  def nStringOpt(columnIndex: Int): Option[String] = opt[String](nString(columnIndex))

  def nStringOpt(columnLabel: String): Option[String] = opt[String](nString(columnLabel))

  def any(columnIndex: Int): Any = {
    ensureCursor()
    wrapIfError(underlying.getObject(columnIndex))
  }

  def any(columnLabel: String): Any = {
    ensureCursor()
    wrapIfError(underlying.getObject(columnLabel))
  }

  def any(columnIndex: Int, map: Map[String, Class[_]]): Any = {
    ensureCursor()
    wrapIfError(underlying.getObject(columnIndex.asInstanceOf[java.lang.Integer], map.asJava))
  }

  def any(columnLabel: String, map: Map[String, Class[_]]): Any = {
    ensureCursor()
    wrapIfError(underlying.getObject(columnLabel, map.asJava))
  }

  def anyOpt(columnIndex: Int): Option[Any] = opt[Any](any(columnIndex))

  def anyOpt(columnLabel: String): Option[Any] = opt[Any](any(columnLabel))

  def anyOpt(columnIndex: Int, map: Map[String, Class[_]]): Option[Any] = opt[Any](any(columnIndex, map))

  def anyOpt(columnLabel: String, map: Map[String, Class[_]]): Option[Any] = opt[Any](any(columnLabel, map))

  def ref(columnIndex: Int): java.sql.Ref = {
    ensureCursor()
    wrapIfError(underlying.getRef(columnIndex))
  }

  def ref(columnLabel: String): java.sql.Ref = {
    ensureCursor()
    wrapIfError(underlying.getRef(columnLabel))
  }

  def refOpt(columnIndex: Int): Option[java.sql.Ref] = opt[java.sql.Ref](ref(columnIndex))

  def refOpt(columnLabel: String): Option[java.sql.Ref] = opt[java.sql.Ref](ref(columnLabel))

  def row: Int = {
    ensureCursor()
    underlying.getRow
  }

  def rowId(columnIndex: Int): java.sql.RowId = {
    ensureCursor()
    underlying.getRowId(columnIndex)
  }

  def rowId(columnLabel: String): java.sql.RowId = {
    ensureCursor()
    underlying.getRowId(columnLabel)
  }

  def nullableShort(columnIndex: Int): java.lang.Short = {
    ensureCursor()
    Option(any(columnIndex)).map {
      case v: Float => v.toShort.asInstanceOf[java.lang.Short]
      case v: Double => v.toShort.asInstanceOf[java.lang.Short]
      case v => java.lang.Short.valueOf(v.toString)
    }.orNull[java.lang.Short]
  }

  def nullableShort(columnLabel: String): java.lang.Short = {
    ensureCursor()
    Option(any(columnLabel)).map {
      case v: Float => v.toShort.asInstanceOf[java.lang.Short]
      case v: Double => v.toShort.asInstanceOf[java.lang.Short]
      case v => java.lang.Short.valueOf(v.toString)
    }.orNull[java.lang.Short]
  }

  def short(columnIndex: Int): Short = nullableShort(columnIndex).asInstanceOf[Short]

  def short(columnLabel: String): Short = nullableShort(columnLabel).asInstanceOf[Short]

  def shortOpt(columnIndex: Int): Option[Short] = opt[Short](nullableShort(columnIndex))

  def shortOpt(columnLabel: String): Option[Short] = opt[Short](nullableShort(columnLabel))

  def sqlXml(columnIndex: Int): java.sql.SQLXML = {
    ensureCursor()
    wrapIfError(underlying.getSQLXML(columnIndex))
  }

  def sqlXml(columnLabel: String): java.sql.SQLXML = {
    ensureCursor()
    wrapIfError(underlying.getSQLXML(columnLabel))
  }

  def sqlXmlOpt(columnIndex: Int): Option[java.sql.SQLXML] = opt[java.sql.SQLXML](sqlXml(columnIndex))

  def sqlXmlOpt(columnLabel: String): Option[java.sql.SQLXML] = opt[java.sql.SQLXML](sqlXml(columnLabel))

  def statement: java.sql.Statement = {
    ensureCursor()
    underlying.getStatement
  }

  def string(columnIndex: Int): String = {
    ensureCursor()
    wrapIfError(underlying.getString(columnIndex))
  }

  def string(columnLabel: String): String = {
    ensureCursor()
    wrapIfError(underlying.getString(columnLabel))
  }

  def stringOpt(columnIndex: Int): Option[String] = opt[String](string(columnIndex))

  def stringOpt(columnLabel: String): Option[String] = opt[String](string(columnLabel))

  def time(columnIndex: Int): java.sql.Time = {
    ensureCursor()
    wrapIfError(underlying.getTime(columnIndex))
  }

  def time(columnLabel: String): java.sql.Time = {
    ensureCursor()
    wrapIfError(underlying.getTime(columnLabel))
  }

  def time(columnIndex: Int, cal: Calendar): java.sql.Time = {
    ensureCursor()
    wrapIfError(underlying.getTime(columnIndex, cal))
  }

  def time(columnLabel: String, cal: Calendar): java.sql.Time = {
    ensureCursor()
    wrapIfError(underlying.getTime(columnLabel, cal))
  }

  def timeOpt(columnIndex: Int): Option[java.sql.Time] = opt[java.sql.Time](time(columnIndex))

  def timeOpt(columnLabel: String): Option[java.sql.Time] = opt[java.sql.Time](time(columnLabel))

  def timeOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Time] = opt[java.sql.Time](time(columnIndex, cal))

  def timeOpt(columnLabel: String, cal: Calendar): Option[java.sql.Time] = opt[java.sql.Time](time(columnLabel, cal))

  def timestamp(columnIndex: Int): java.sql.Timestamp = {
    ensureCursor()
    wrapIfError(underlying.getTimestamp(columnIndex))
  }

  def timestamp(columnLabel: String): java.sql.Timestamp = {
    ensureCursor()
    wrapIfError(underlying.getTimestamp(columnLabel))
  }

  def timestamp(columnIndex: Int, cal: Calendar): java.sql.Timestamp = {
    ensureCursor()
    wrapIfError(underlying.getTimestamp(columnIndex, cal))
  }

  def timestamp(columnLabel: String, cal: Calendar): java.sql.Timestamp = {
    ensureCursor()
    wrapIfError(underlying.getTimestamp(columnLabel, cal))
  }

  def dateTime(columnIndex: Int): DateTime = timestamp(columnIndex).toDateTime

  def dateTime(columnLabel: String): DateTime = timestamp(columnLabel).toDateTime

  def localDate(columnIndex: Int): LocalDate = date(columnIndex).toLocalDate

  def localDate(columnLabel: String): LocalDate = date(columnLabel).toLocalDate

  def localTime(columnIndex: Int): LocalTime = time(columnIndex).toLocalTime

  def localTime(columnLabel: String): LocalTime = time(columnLabel).toLocalTime

  def timestampOpt(columnIndex: Int): Option[java.sql.Timestamp] = opt[java.sql.Timestamp](timestamp(columnIndex))

  def timestampOpt(columnLabel: String): Option[java.sql.Timestamp] = opt[java.sql.Timestamp](timestamp(columnLabel))

  def timestampOpt(columnIndex: Int, cal: Calendar): Option[java.sql.Timestamp] = opt[java.sql.Timestamp](timestamp(columnIndex, cal))

  def timestampOpt(columnLabel: String, cal: Calendar): Option[java.sql.Timestamp] = opt[java.sql.Timestamp](timestamp(columnLabel, cal))

  def dateTimeOpt(columnIndex: Int): Option[DateTime] = timestampOpt(columnIndex).map(_.toDateTime)

  def dateTimeOpt(columnLabel: String): Option[DateTime] = timestampOpt(columnLabel).map(_.toDateTime)

  def localDateOpt(columnIndex: Int): Option[LocalDate] = dateOpt(columnIndex).map(_.toLocalDate)

  def localDateOpt(columnLabel: String): Option[LocalDate] = dateOpt(columnLabel).map(_.toLocalDate)

  def localTimeOpt(columnIndex: Int): Option[LocalTime] = timeOpt(columnIndex).map(_.toLocalTime)

  def localTimeOpt(columnLabel: String): Option[LocalTime] = timeOpt(columnLabel).map(_.toLocalTime)

  def url(columnIndex: Int): java.net.URL = {
    ensureCursor()
    wrapIfError(underlying.getURL(columnIndex))
  }

  def url(columnLabel: String): java.net.URL = {
    ensureCursor()
    wrapIfError(underlying.getURL(columnLabel))
  }

  def urlOpt(columnIndex: Int): Option[java.net.URL] = opt[java.net.URL](url(columnIndex))

  def urlOpt(columnLabel: String): Option[java.net.URL] = opt[java.net.URL](url(columnLabel))

  def warnings: java.sql.SQLWarning = {
    ensureCursor()
    underlying.getWarnings
  }

  def toMap(): Map[String, Any] = (1 to metaData.getColumnCount).foldLeft(Map[String, Any]()) { (result, i) =>
    val label = metaData.getColumnLabel(i)
    Option(any(label)).map { value => result + (label -> value) }.getOrElse(result)
  }

  def toSymbolMap(): Map[Symbol, Any] = toMap().map { case (k, v) => Symbol(k) -> v }

}

