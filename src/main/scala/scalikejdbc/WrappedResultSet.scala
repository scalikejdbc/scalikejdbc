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
case class WrappedResultSet(underlying: ResultSet) {

  def array(columnIndex: Int) = underlying.getArray(columnIndex)

  def array(columnLabel: String) = underlying.getArray(columnLabel)

  def asciiStream(columnIndex: Int) = underlying.getAsciiStream(columnIndex)

  def asciiStream(columnLabel: String) = underlying.getAsciiStream(columnLabel)

  def bigDecimal(columnIndex: Int) = underlying.getBigDecimal(columnIndex)

  def bigDecimal(columnLabel: String) = underlying.getBigDecimal(columnLabel)

  def binaryStream(columnIndex: Int) = underlying.getBinaryStream(columnIndex)

  def binaryStream(columnLabel: String) = underlying.getBinaryStream(columnLabel)

  def blob(columnIndex: Int) = underlying.getBlob(columnIndex)

  def blob(columnLabel: String) = underlying.getBlob(columnLabel)

  def boolean(columnIndex: Int) = underlying.getBoolean(columnIndex)

  def boolean(columnLabel: String) = underlying.getBoolean(columnLabel)

  def byte(columnIndex: Int) = underlying.getByte(columnIndex)

  def byte(columnLabel: String) = underlying.getByte(columnLabel)

  def bytes(columnIndex: Int) = underlying.getBytes(columnIndex)

  def bytes(columnLabel: String) = underlying.getBytes(columnLabel)

  def characterStream(columnIndex: Int) = underlying.getCharacterStream(columnIndex)

  def characterStream(columnLabel: String) = underlying.getCharacterStream(columnLabel)

  def clob(columnIndex: Int) = underlying.getClob(columnIndex)

  def clob(columnLabel: String) = underlying.getClob(columnLabel)

  def concurrency = underlying.getConcurrency

  def cursorName = underlying.getCursorName

  def date(columnIndex: Int) = underlying.getDate(columnIndex)

  def date(columnLabel: String) = underlying.getDate(columnLabel)

  def date(columnIndex: Int, cal: Calendar) = underlying.getDate(columnIndex, cal)

  def date(columnLabel: String, cal: Calendar) = underlying.getDate(columnLabel, cal)

  def double(columnIndex: Int) = underlying.getDouble(columnIndex)

  def double(columnLabel: String) = underlying.getDouble(columnLabel)

  def fetchDirection = underlying.getFetchDirection

  def fetchSize = underlying.getFetchSize

  def float(columnIndex: Int) = underlying.getFloat(columnIndex)

  def float(columnLabel: String) = underlying.getFloat(columnLabel)

  def holdability = underlying.getHoldability

  def int(columnIndex: Int) = underlying.getInt(columnIndex)

  def int(columnLabel: String) = underlying.getInt(columnLabel)

  def long(columnIndex: Int) = underlying.getLong(columnIndex)

  def long(columnLabel: String) = underlying.getLong(columnLabel)

  def metaData = underlying.getMetaData

  def nCharacterStream(columnIndex: Int) = underlying.getNCharacterStream(columnIndex)

  def nCharacterStream(columnLabel: String) = underlying.getNCharacterStream(columnLabel)

  def nClob(columnIndex: Int) = underlying.getNClob(columnIndex)

  def nClob(columnLabel: String) = underlying.getNClob(columnLabel)

  def nString(columnIndex: Int) = underlying.getNString(columnIndex)

  def nString(columnLabel: String) = underlying.getNString(columnLabel)

  def any(columnIndex: Int) = underlying.getObject(columnIndex)

  def any(columnLabel: String) = underlying.getObject(columnLabel)

  def any(columnIndex: Int, map: Map[String, Class[_]]) = underlying.getObject(columnIndex.asInstanceOf[java.lang.Integer], map.asJava)

  def any(columnLabel: String, map: Map[String, Class[_]]) = underlying.getObject(columnLabel, map.asJava)

  def ref(columnIndex: Int) = underlying.getRef(columnIndex)

  def ref(columnLabel: String) = underlying.getRef(columnLabel)

  def row = underlying.getRow

  def rowId(columnIndex: Int) = underlying.getRowId(columnIndex)

  def rowId(columnLabel: String) = underlying.getRowId(columnLabel)

  def short(columnIndex: Int) = underlying.getShort(columnIndex)

  def short(columnLabel: String) = underlying.getShort(columnLabel)

  def sqlXml(columnIndex: Int) = underlying.getSQLXML(columnIndex)

  def sqlXml(columnLabel: String) = underlying.getSQLXML(columnLabel)

  def statement = underlying.getStatement

  def string(columnIndex: Int) = underlying.getString(columnIndex)

  def string(columnLabel: String) = underlying.getString(columnLabel)

  def time(columnIndex: Int) = underlying.getTime(columnIndex)

  def time(columnLabel: String) = underlying.getTime(columnLabel)

  def time(columnIndex: Int, cal: Calendar) = underlying.getTime(columnIndex, cal)

  def time(columnLabel: String, cal: Calendar) = underlying.getTime(columnLabel, cal)

  def timestamp(columnIndex: Int) = underlying.getTimestamp(columnIndex)

  def timestamp(columnLabel: String) = underlying.getTimestamp(columnLabel)

  def timestamp(columnIndex: Int, cal: Calendar) = underlying.getTimestamp(columnIndex, cal)

  def timestamp(columnLabel: String, cal: Calendar) = underlying.getTimestamp(columnLabel, cal)

  def url(columnIndex: Int) = underlying.getURL(columnIndex)

  def url(columnLabel: String) = underlying.getURL(columnLabel)

  def warnings = underlying.getWarnings

}

