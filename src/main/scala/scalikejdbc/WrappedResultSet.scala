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
    if (cursor.index != index) {
      throw new IllegalStateException("Invalid cursor position (actual:" + cursor.index + ",expected:" + index + ")")
    }
  }

  def array(columnIndex: Int) = {
    ensureCursor()
    underlying.getArray(columnIndex)
  }
  def array(columnLabel: String) = {
    ensureCursor()
    underlying.getArray(columnLabel)
  }

  def asciiStream(columnIndex: Int) = {
    ensureCursor()
    underlying.getAsciiStream(columnIndex)
  }

  def asciiStream(columnLabel: String) = {
    ensureCursor()
    underlying.getAsciiStream(columnLabel)
  }
  def bigDecimal(columnIndex: Int) = {
    ensureCursor()
    underlying.getBigDecimal(columnIndex)
  }

  def bigDecimal(columnLabel: String) = {
    ensureCursor()
    underlying.getBigDecimal(columnLabel)
  }

  def binaryStream(columnIndex: Int) = {
    ensureCursor()
    underlying.getBinaryStream(columnIndex)
  }

  def binaryStream(columnLabel: String) = {
    ensureCursor()
    underlying.getBinaryStream(columnLabel)
  }

  def blob(columnIndex: Int) = {
    ensureCursor()
    underlying.getBlob(columnIndex)
  }

  def blob(columnLabel: String) = {
    ensureCursor()
    underlying.getBlob(columnLabel)
  }

  def boolean(columnIndex: Int) = {
    ensureCursor()
    underlying.getBoolean(columnIndex)
  }

  def boolean(columnLabel: String) = {
    ensureCursor()
    underlying.getBoolean(columnLabel)
  }

  def byte(columnIndex: Int) = {
    ensureCursor()
    underlying.getByte(columnIndex)
  }

  def byte(columnLabel: String) = {
    ensureCursor()
    underlying.getByte(columnLabel)
  }

  def bytes(columnIndex: Int) = {
    ensureCursor()
    underlying.getBytes(columnIndex)
  }

  def bytes(columnLabel: String) = {
    ensureCursor()
    underlying.getBytes(columnLabel)
  }

  def characterStream(columnIndex: Int) = {
    ensureCursor()
    underlying.getCharacterStream(columnIndex)
  }

  def characterStream(columnLabel: String) = {
    ensureCursor()
    underlying.getCharacterStream(columnLabel)
  }

  def clob(columnIndex: Int) = {
    ensureCursor()
    underlying.getClob(columnIndex)
  }

  def clob(columnLabel: String) = {
    ensureCursor()
    underlying.getClob(columnLabel)
  }

  def concurrency = {
    ensureCursor()
    underlying.getConcurrency
  }

  def cursorName = {
    ensureCursor()
    underlying.getCursorName
  }

  def date(columnIndex: Int) = {
    ensureCursor()
    underlying.getDate(columnIndex)
  }

  def date(columnLabel: String) = {
    ensureCursor()
    underlying.getDate(columnLabel)
  }

  def date(columnIndex: Int, cal: Calendar) = {
    ensureCursor()
    underlying.getDate(columnIndex, cal)
  }

  def date(columnLabel: String, cal: Calendar) = {
    ensureCursor()
    underlying.getDate(columnLabel, cal)
  }

  def double(columnIndex: Int) = {
    ensureCursor()
    underlying.getDouble(columnIndex)
  }

  def double(columnLabel: String) = {
    ensureCursor()
    underlying.getDouble(columnLabel)
  }

  def fetchDirection = {
    ensureCursor()
    underlying.getFetchDirection
  }

  def fetchSize = {
    ensureCursor()
    underlying.getFetchSize
  }

  def float(columnIndex: Int) = {
    ensureCursor()
    underlying.getFloat(columnIndex)
  }

  def float(columnLabel: String) = {
    ensureCursor()
    underlying.getFloat(columnLabel)
  }

  def holdability = {
    ensureCursor()
    underlying.getHoldability
  }

  def int(columnIndex: Int) = {
    ensureCursor()
    underlying.getInt(columnIndex)
  }

  def int(columnLabel: String) = {
    ensureCursor()
    underlying.getInt(columnLabel)
  }

  def long(columnIndex: Int) = {
    ensureCursor()
    underlying.getLong(columnIndex)
  }

  def long(columnLabel: String) = {
    ensureCursor()
    underlying.getLong(columnLabel)
  }

  def metaData = {
    ensureCursor()
    underlying.getMetaData
  }

  def nCharacterStream(columnIndex: Int) = {
    ensureCursor()
    underlying.getNCharacterStream(columnIndex)
  }

  def nCharacterStream(columnLabel: String) = {
    ensureCursor()
    underlying.getNCharacterStream(columnLabel)
  }

  def nClob(columnIndex: Int) = {
    ensureCursor()
    underlying.getNClob(columnIndex)
  }

  def nClob(columnLabel: String) = {
    ensureCursor()
    underlying.getNClob(columnLabel)
  }

  def nString(columnIndex: Int) = {
    ensureCursor()
    underlying.getNString(columnIndex)
  }

  def nString(columnLabel: String) = {
    ensureCursor()
    underlying.getNString(columnLabel)
  }

  def any(columnIndex: Int) = {
    ensureCursor()
    underlying.getObject(columnIndex)
  }

  def any(columnLabel: String) = {
    ensureCursor()
    underlying.getObject(columnLabel)
  }

  def any(columnIndex: Int, map: Map[String, Class[_]]) = {
    ensureCursor()
    underlying.getObject(columnIndex.asInstanceOf[java.lang.Integer], map.asJava)
  }

  def any(columnLabel: String, map: Map[String, Class[_]]) = {
    ensureCursor()
    underlying.getObject(columnLabel, map.asJava)
  }

  def ref(columnIndex: Int) = {
    ensureCursor()
    underlying.getRef(columnIndex)
  }

  def ref(columnLabel: String) = {
    ensureCursor()
    underlying.getRef(columnLabel)
  }

  def row = {
    ensureCursor()
    underlying.getRow
  }

  def rowId(columnIndex: Int) = {
    ensureCursor()
    underlying.getRowId(columnIndex)
  }

  def rowId(columnLabel: String) = {
    ensureCursor()
    underlying.getRowId(columnLabel)
  }

  def short(columnIndex: Int) = {
    ensureCursor()
    underlying.getShort(columnIndex)
  }

  def short(columnLabel: String) = {
    ensureCursor()
    underlying.getShort(columnLabel)
  }

  def sqlXml(columnIndex: Int) = {
    ensureCursor()
    underlying.getSQLXML(columnIndex)
  }

  def sqlXml(columnLabel: String) = {
    ensureCursor()
    underlying.getSQLXML(columnLabel)
  }

  def statement = {
    ensureCursor()
    underlying.getStatement
  }

  def string(columnIndex: Int) = {
    ensureCursor()
    underlying.getString(columnIndex)
  }

  def string(columnLabel: String) = {
    ensureCursor()
    underlying.getString(columnLabel)
  }

  def time(columnIndex: Int) = {
    ensureCursor()
    underlying.getTime(columnIndex)
  }

  def time(columnLabel: String) = {
    ensureCursor()
    underlying.getTime(columnLabel)
  }

  def time(columnIndex: Int, cal: Calendar) = {
    ensureCursor()
    underlying.getTime(columnIndex, cal)
  }

  def time(columnLabel: String, cal: Calendar) = {
    ensureCursor()
    underlying.getTime(columnLabel, cal)
  }

  def timestamp(columnIndex: Int) = {
    ensureCursor()
    underlying.getTimestamp(columnIndex)
  }

  def timestamp(columnLabel: String) = {
    ensureCursor()
    underlying.getTimestamp(columnLabel)
  }

  def timestamp(columnIndex: Int, cal: Calendar) = {
    ensureCursor()
    underlying.getTimestamp(columnIndex, cal)
  }

  def timestamp(columnLabel: String, cal: Calendar) = {
    ensureCursor()
    underlying.getTimestamp(columnLabel, cal)
  }

  def url(columnIndex: Int) = {
    ensureCursor()
    underlying.getURL(columnIndex)
  }

  def url(columnLabel: String) = {
    ensureCursor()
    underlying.getURL(columnLabel)
  }

  def warnings = {
    ensureCursor()
    underlying.getWarnings
  }

}

