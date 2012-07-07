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
import collection.JavaConverters._

/**
 * {@link java.sql.ResultSet} wrapper
 */
case class WrappedResultSet(underlying: ResultSet, cursor: ResultSetCursor, index: Int) {

  def ensureCursor(): Unit = {
    if (cursor.position != index) {
      throw new IllegalStateException(
        ErrorMessage.INVALID_CURSOR_POSITION + " (actual:" + cursor.position + ",expected:" + index + ")")
    }
  }

  def array(columnIndex: Int): java.sql.Array = {
    ensureCursor()
    underlying.getArray(columnIndex)
  }

  def array(columnLabel: String): java.sql.Array = {
    ensureCursor()
    underlying.getArray(columnLabel)
  }

  def asciiStream(columnIndex: Int): java.io.InputStream = {
    ensureCursor()
    underlying.getAsciiStream(columnIndex)
  }

  def asciiStream(columnLabel: String): java.io.InputStream = {
    ensureCursor()
    underlying.getAsciiStream(columnLabel)
  }

  def bigDecimal(columnIndex: Int): java.math.BigDecimal = {
    ensureCursor()
    underlying.getBigDecimal(columnIndex)
  }

  def bigDecimal(columnLabel: String): java.math.BigDecimal = {
    ensureCursor()
    underlying.getBigDecimal(columnLabel)
  }

  def binaryStream(columnIndex: Int): java.io.InputStream = {
    ensureCursor()
    underlying.getBinaryStream(columnIndex)
  }

  def binaryStream(columnLabel: String): java.io.InputStream = {
    ensureCursor()
    underlying.getBinaryStream(columnLabel)
  }

  def blob(columnIndex: Int): java.sql.Blob = {
    ensureCursor()
    underlying.getBlob(columnIndex)
  }

  def blob(columnLabel: String): java.sql.Blob = {
    ensureCursor()
    underlying.getBlob(columnLabel)
  }

  def boolean(columnIndex: Int): java.lang.Boolean = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Boolean.valueOf(v.toString))
      .orNull[java.lang.Boolean]
  }

  def boolean(columnLabel: String): java.lang.Boolean = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Boolean.valueOf(v.toString))
      .orNull[java.lang.Boolean]
  }

  def booleanOpt(columnIndex: Int): Option[Boolean] = opt[Boolean](boolean(columnIndex))

  def booleanOpt(columnLabel: String): Option[Boolean] = opt[Boolean](boolean(columnLabel))

  def byte(columnIndex: Int): java.lang.Byte = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Byte.valueOf(v.toString))
      .orNull[java.lang.Byte]
  }

  def byte(columnLabel: String): java.lang.Byte = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Byte.valueOf(v.toString))
      .orNull[java.lang.Byte]
  }

  def byteOpt(columnIndex: Int): Option[Byte] = opt[Byte](byte(columnIndex))

  def byteOpt(columnLabel: String): Option[Byte] = opt[Byte](byte(columnLabel))

  def bytes(columnIndex: Int): Array[Byte] = {
    ensureCursor()
    underlying.getBytes(columnIndex)
  }

  def bytes(columnLabel: String): Array[Byte] = {
    ensureCursor()
    underlying.getBytes(columnLabel)
  }

  def characterStream(columnIndex: Int): java.io.Reader = {
    ensureCursor()
    underlying.getCharacterStream(columnIndex)
  }

  def characterStream(columnLabel: String): java.io.Reader = {
    ensureCursor()
    underlying.getCharacterStream(columnLabel)
  }

  def clob(columnIndex: Int): java.sql.Clob = {
    ensureCursor()
    underlying.getClob(columnIndex)
  }

  def clob(columnLabel: String): java.sql.Clob = {
    ensureCursor()
    underlying.getClob(columnLabel)
  }

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
    underlying.getDate(columnIndex)
  }

  def date(columnLabel: String): java.sql.Date = {
    ensureCursor()
    underlying.getDate(columnLabel)
  }

  def date(columnIndex: Int, cal: Calendar): java.sql.Date = {
    ensureCursor()
    underlying.getDate(columnIndex, cal)
  }

  def date(columnLabel: String, cal: Calendar): java.sql.Date = {
    ensureCursor()
    underlying.getDate(columnLabel, cal)
  }

  def double(columnIndex: Int): java.lang.Double = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Double.valueOf(v.toString))
      .orNull[java.lang.Double]
  }

  def double(columnLabel: String): java.lang.Double = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Double.valueOf(v.toString))
      .orNull[java.lang.Double]
  }

  def doubleOpt(columnIndex: Int): Option[Double] = opt[Double](double(columnIndex))

  def doubleOpt(columnLabel: String): Option[Double] = opt[Double](double(columnLabel))

  def fetchDirection: Int = {
    ensureCursor()
    underlying.getFetchDirection
  }

  def fetchSize: Int = {
    ensureCursor()
    underlying.getFetchSize
  }

  def float(columnIndex: Int): java.lang.Float = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Float.valueOf(v.toString))
      .orNull[java.lang.Float]
  }

  def float(columnLabel: String): java.lang.Float = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Float.valueOf(v.toString))
      .orNull[java.lang.Float]
  }

  def floatOpt(columnIndex: Int): Option[Float] = opt[Float](float(columnIndex))

  def floatOpt(columnLabel: String): Option[Float] = opt[Float](float(columnLabel))

  def holdability: Int = {
    ensureCursor()
    underlying.getHoldability
  }

  def int(columnIndex: Int): java.lang.Integer = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Integer.valueOf(v.toString))
      .orNull[java.lang.Integer]
  }

  def int(columnLabel: String): java.lang.Integer = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Integer.valueOf(v.toString))
      .orNull[java.lang.Integer]
  }

  def intOpt(columnIndex: Int): Option[Int] = opt[Int](int(columnIndex))

  def intOpt(columnLabel: String): Option[Int] = opt[Int](int(columnLabel))

  def long(columnIndex: Int): java.lang.Long = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Long.valueOf(v.toString))
      .orNull[java.lang.Long]
  }

  def long(columnLabel: String): java.lang.Long = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Long.valueOf(v.toString))
      .orNull[java.lang.Long]
  }

  def longOpt(columnIndex: Int): Option[Long] = opt[Long](long(columnIndex))

  def longOpt(columnLabel: String): Option[Long] = opt[Long](long(columnLabel))

  def metaData: java.sql.ResultSetMetaData = {
    ensureCursor()
    underlying.getMetaData
  }

  def nCharacterStream(columnIndex: Int): java.io.Reader = {
    ensureCursor()
    underlying.getNCharacterStream(columnIndex)
  }

  def nCharacterStream(columnLabel: String): java.io.Reader = {
    ensureCursor()
    underlying.getNCharacterStream(columnLabel)
  }

  def nClob(columnIndex: Int): java.sql.NClob = {
    ensureCursor()
    underlying.getNClob(columnIndex)
  }

  def nClob(columnLabel: String): java.sql.NClob = {
    ensureCursor()
    underlying.getNClob(columnLabel)
  }

  def nString(columnIndex: Int): String = {
    ensureCursor()
    underlying.getNString(columnIndex)
  }

  def nString(columnLabel: String): String = {
    ensureCursor()
    underlying.getNString(columnLabel)
  }

  def any(columnIndex: Int): Any = {
    ensureCursor()
    underlying.getObject(columnIndex)
  }

  def any(columnLabel: String): Any = {
    ensureCursor()
    underlying.getObject(columnLabel)
  }

  def any(columnIndex: Int, map: Map[String, Class[_]]): Any = {
    ensureCursor()
    underlying.getObject(columnIndex.asInstanceOf[java.lang.Integer], map.asJava)
  }

  def any(columnLabel: String, map: Map[String, Class[_]]): Any = {
    ensureCursor()
    underlying.getObject(columnLabel, map.asJava)
  }

  def ref(columnIndex: Int): java.sql.Ref = {
    ensureCursor()
    underlying.getRef(columnIndex)
  }

  def ref(columnLabel: String): java.sql.Ref = {
    ensureCursor()
    underlying.getRef(columnLabel)
  }

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

  def short(columnIndex: Int): java.lang.Short = {
    ensureCursor()
    Option(any(columnIndex))
      .map(v => java.lang.Short.valueOf(v.toString))
      .orNull[java.lang.Short]
  }

  def short(columnLabel: String): java.lang.Short = {
    ensureCursor()
    Option(any(columnLabel))
      .map(v => java.lang.Short.valueOf(v.toString))
      .orNull[java.lang.Short]
  }

  def shortOpt(columnIndex: Int): Option[Short] = opt[Short](short(columnIndex))

  def shortOpt(columnLabel: String): Option[Short] = opt[Short](short(columnLabel))

  def sqlXml(columnIndex: Int): java.sql.SQLXML = {
    ensureCursor()
    underlying.getSQLXML(columnIndex)
  }

  def sqlXml(columnLabel: String): java.sql.SQLXML = {
    ensureCursor()
    underlying.getSQLXML(columnLabel)
  }

  def statement: java.sql.Statement = {
    ensureCursor()
    underlying.getStatement
  }

  def string(columnIndex: Int): String = {
    ensureCursor()
    underlying.getString(columnIndex)
  }

  def string(columnLabel: String): String = {
    ensureCursor()
    underlying.getString(columnLabel)
  }

  def time(columnIndex: Int): java.sql.Time = {
    ensureCursor()
    underlying.getTime(columnIndex)
  }

  def time(columnLabel: String): java.sql.Time = {
    ensureCursor()
    underlying.getTime(columnLabel)
  }

  def time(columnIndex: Int, cal: Calendar): java.sql.Time = {
    ensureCursor()
    underlying.getTime(columnIndex, cal)
  }

  def time(columnLabel: String, cal: Calendar): java.sql.Time = {
    ensureCursor()
    underlying.getTime(columnLabel, cal)
  }

  def timestamp(columnIndex: Int): java.sql.Timestamp = {
    ensureCursor()
    underlying.getTimestamp(columnIndex)
  }

  def timestamp(columnLabel: String): java.sql.Timestamp = {
    ensureCursor()
    underlying.getTimestamp(columnLabel)
  }

  def timestamp(columnIndex: Int, cal: Calendar): java.sql.Timestamp = {
    ensureCursor()
    underlying.getTimestamp(columnIndex, cal)
  }

  def timestamp(columnLabel: String, cal: Calendar): java.sql.Timestamp = {
    ensureCursor()
    underlying.getTimestamp(columnLabel, cal)
  }

  def url(columnIndex: Int): java.net.URL = {
    ensureCursor()
    underlying.getURL(columnIndex)
  }

  def url(columnLabel: String): java.net.URL = {
    ensureCursor()
    underlying.getURL(columnLabel)
  }

  def warnings: java.sql.SQLWarning = {
    ensureCursor()
    underlying.getWarnings
  }

}

