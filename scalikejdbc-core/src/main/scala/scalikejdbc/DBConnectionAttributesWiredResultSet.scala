package scalikejdbc

import java.io.{ Reader, InputStream }
import java.math.BigDecimal
import java.net.URL
import java.sql._
import java.util
import java.util.Calendar

/**
 * ResultSet proxy which holds current DBConnectionAttributes.
 */
private[scalikejdbc] class DBConnectionAttributesWiredResultSet(
  underlying: ResultSet,
  connAttributes: DBConnectionAttributes
) extends ResultSet {

  /**
   * Converts Timestamp value to an appropriate timezone.
   */
  private[this] def convertTimeZoneIfNeeded(timestamp: Timestamp): Timestamp =
    if (connAttributes.timeZoneSettings.conversionEnabled) {
      val serverTimeZone = connAttributes.timeZoneSettings.serverTimeZone
      val clientTimeZone = java.util.TimeZone.getDefault
      TimeZoneConverter
        .from(serverTimeZone)
        .to(clientTimeZone)
        .convert(timestamp)
    } else {
      timestamp
    }

  private[this] def convertTimeZoneIfNeeded[T](any: T): T = any match {
    case any: Timestamp => convertTimeZoneIfNeeded(any).asInstanceOf[T]
    case _              => any
  }

  // --------------------------------------------
  // Converts timezone if needed
  // --------------------------------------------

  def getTimestamp(columnIndex: Int): Timestamp = convertTimeZoneIfNeeded(
    underlying.getTimestamp(columnIndex)
  )
  def getTimestamp(columnLabel: String): Timestamp = convertTimeZoneIfNeeded(
    underlying.getTimestamp(columnLabel)
  )
  def getTimestamp(columnIndex: Int, cal: Calendar): Timestamp =
    convertTimeZoneIfNeeded(underlying.getTimestamp(columnIndex, cal))
  def getTimestamp(columnLabel: String, cal: Calendar): Timestamp =
    convertTimeZoneIfNeeded(underlying.getTimestamp(columnLabel, cal))

  def getObject(columnIndex: Int): AnyRef = convertTimeZoneIfNeeded(
    underlying.getObject(columnIndex)
  )
  def getObject(columnLabel: String): AnyRef = convertTimeZoneIfNeeded(
    underlying.getObject(columnLabel)
  )
  def getObject(columnIndex: Int, map: util.Map[String, Class[?]]): AnyRef =
    convertTimeZoneIfNeeded(underlying.getObject(columnIndex, map))
  def getObject(columnLabel: String, map: util.Map[String, Class[?]]): AnyRef =
    convertTimeZoneIfNeeded(underlying.getObject(columnLabel, map))
  def getObject[T](columnIndex: Int, `type`: Class[T]): T =
    convertTimeZoneIfNeeded(underlying.getObject(columnIndex, `type`))
  def getObject[T](columnLabel: String, `type`: Class[T]): T =
    convertTimeZoneIfNeeded(underlying.getObject(columnLabel, `type`))

  // --------------------------------------------
  // Just delegates to underlying methods
  // --------------------------------------------

  def getType: Int = underlying.getType
  def isBeforeFirst: Boolean = underlying.isBeforeFirst
  def next(): Boolean = underlying.next()
  def updateString(columnIndex: Int, x: String): Unit =
    underlying.updateString(columnIndex, x)
  def updateString(columnLabel: String, x: String): Unit =
    underlying.updateString(columnLabel, x)
  def updateNString(columnIndex: Int, nString: String): Unit =
    underlying.updateNString(columnIndex, nString)
  def updateNString(columnLabel: String, nString: String): Unit =
    underlying.updateNString(columnLabel, nString)
  def clearWarnings(): Unit = underlying.clearWarnings()
  def updateTimestamp(columnIndex: Int, x: Timestamp): Unit =
    underlying.updateTimestamp(columnIndex, x)
  def updateTimestamp(columnLabel: String, x: Timestamp): Unit =
    underlying.updateTimestamp(columnLabel, x)
  def updateByte(columnIndex: Int, x: Byte): Unit =
    underlying.updateByte(columnIndex, x)
  def updateByte(columnLabel: String, x: Byte): Unit =
    underlying.updateByte(columnLabel, x)
  def updateBigDecimal(columnIndex: Int, x: BigDecimal): Unit =
    underlying.updateBigDecimal(columnIndex, x)
  def updateBigDecimal(columnLabel: String, x: BigDecimal): Unit =
    underlying.updateBigDecimal(columnLabel, x)
  def updateDouble(columnIndex: Int, x: Double): Unit =
    underlying.updateDouble(columnIndex, x)
  def updateDouble(columnLabel: String, x: Double): Unit =
    underlying.updateDouble(columnLabel, x)
  def updateDate(columnIndex: Int, x: Date): Unit =
    underlying.updateDate(columnIndex, x)
  def updateDate(columnLabel: String, x: Date): Unit =
    underlying.updateDate(columnLabel, x)
  def isAfterLast: Boolean = underlying.isAfterLast
  def updateBoolean(columnIndex: Int, x: Boolean): Unit =
    underlying.updateBoolean(columnIndex, x)
  def updateBoolean(columnLabel: String, x: Boolean): Unit =
    underlying.updateBoolean(columnLabel, x)
  def getBinaryStream(columnIndex: Int): InputStream =
    underlying.getBinaryStream(columnIndex)
  def getBinaryStream(columnLabel: String): InputStream =
    underlying.getBinaryStream(columnLabel)
  def beforeFirst(): Unit = underlying.beforeFirst()
  def updateNCharacterStream(columnIndex: Int, x: Reader, length: Long): Unit =
    underlying.updateNCharacterStream(columnIndex, x, length)
  def updateNCharacterStream(
    columnLabel: String,
    reader: Reader,
    length: Long
  ): Unit = underlying.updateNCharacterStream(columnLabel, reader, length)
  def updateNCharacterStream(columnIndex: Int, x: Reader): Unit =
    underlying.updateNCharacterStream(columnIndex, x)
  def updateNCharacterStream(columnLabel: String, reader: Reader): Unit =
    underlying.updateNCharacterStream(columnLabel, reader)
  def updateNClob(columnIndex: Int, nClob: NClob): Unit =
    underlying.updateNClob(columnIndex, nClob)
  def updateNClob(columnLabel: String, nClob: NClob): Unit =
    underlying.updateNClob(columnLabel, nClob)
  def updateNClob(columnIndex: Int, reader: Reader, length: Long): Unit =
    underlying.updateNClob(columnIndex, reader, length)
  def updateNClob(columnLabel: String, reader: Reader, length: Long): Unit =
    underlying.updateNClob(columnLabel, reader, length)
  def updateNClob(columnIndex: Int, reader: Reader): Unit =
    underlying.updateNClob(columnIndex, reader)
  def updateNClob(columnLabel: String, reader: Reader): Unit =
    underlying.updateNClob(columnLabel, reader)
  def last(): Boolean = underlying.last()
  def isLast: Boolean = underlying.isLast
  def getNClob(columnIndex: Int): NClob = underlying.getNClob(columnIndex)
  def getNClob(columnLabel: String): NClob = underlying.getNClob(columnLabel)
  def getCharacterStream(columnIndex: Int): Reader =
    underlying.getCharacterStream(columnIndex)
  def getCharacterStream(columnLabel: String): Reader =
    underlying.getCharacterStream(columnLabel)
  def updateArray(columnIndex: Int, x: Array): Unit =
    underlying.updateArray(columnIndex, x)
  def updateArray(columnLabel: String, x: Array): Unit =
    underlying.updateArray(columnLabel, x)
  def getDouble(columnIndex: Int): Double = underlying.getDouble(columnIndex)
  def getDouble(columnLabel: String): Double = underlying.getDouble(columnLabel)
  def updateBlob(columnIndex: Int, x: Blob): Unit =
    underlying.updateBlob(columnIndex, x)
  def updateBlob(columnLabel: String, x: Blob): Unit =
    underlying.updateBlob(columnLabel, x)
  def updateBlob(
    columnIndex: Int,
    inputStream: InputStream,
    length: Long
  ): Unit = underlying.updateBlob(columnIndex, inputStream, length)
  def updateBlob(
    columnLabel: String,
    inputStream: InputStream,
    length: Long
  ): Unit = underlying.updateBlob(columnLabel, inputStream, length)
  def updateBlob(columnIndex: Int, inputStream: InputStream): Unit =
    underlying.updateBlob(columnIndex, inputStream)
  def updateBlob(columnLabel: String, inputStream: InputStream): Unit =
    underlying.updateBlob(columnLabel, inputStream)
  def getArray(columnIndex: Int): Array = underlying.getArray(columnIndex)
  def getArray(columnLabel: String): Array = underlying.getArray(columnLabel)
  def isFirst: Boolean = underlying.isFirst
  def getURL(columnIndex: Int): URL = underlying.getURL(columnIndex)
  def getURL(columnLabel: String): URL = underlying.getURL(columnLabel)
  def updateRow(): Unit = underlying.updateRow()
  def insertRow(): Unit = underlying.insertRow()
  def getMetaData: ResultSetMetaData = underlying.getMetaData
  def updateBinaryStream(columnIndex: Int, x: InputStream, length: Int): Unit =
    underlying.updateBinaryStream(columnIndex, x, length)
  def updateBinaryStream(
    columnLabel: String,
    x: InputStream,
    length: Int
  ): Unit = underlying.updateBinaryStream(columnLabel, x, length)
  def updateBinaryStream(columnIndex: Int, x: InputStream, length: Long): Unit =
    underlying.updateBinaryStream(columnIndex, x, length)
  def updateBinaryStream(
    columnLabel: String,
    x: InputStream,
    length: Long
  ): Unit = underlying.updateBinaryStream(columnLabel, x, length)
  def updateBinaryStream(columnIndex: Int, x: InputStream): Unit =
    underlying.updateBinaryStream(columnIndex, x)
  def updateBinaryStream(columnLabel: String, x: InputStream): Unit =
    underlying.updateBinaryStream(columnLabel, x)
  def absolute(row: Int): Boolean = underlying.absolute(row)
  def updateRowId(columnIndex: Int, x: RowId): Unit =
    underlying.updateRowId(columnIndex, x)
  def updateRowId(columnLabel: String, x: RowId): Unit =
    underlying.updateRowId(columnLabel, x)
  def getRowId(columnIndex: Int): RowId = underlying.getRowId(columnIndex)
  def getRowId(columnLabel: String): RowId = underlying.getRowId(columnLabel)
  def moveToInsertRow(): Unit = underlying.moveToInsertRow()
  def rowInserted(): Boolean = underlying.rowInserted()
  def getFloat(columnIndex: Int): Float = underlying.getFloat(columnIndex)
  def getFloat(columnLabel: String): Float = underlying.getFloat(columnLabel)
  def getBigDecimal(columnIndex: Int, scale: Int): BigDecimal =
    underlying.getBigDecimal(columnIndex, scale)
  def getBigDecimal(columnLabel: String, scale: Int): BigDecimal =
    underlying.getBigDecimal(columnLabel, scale)
  def getBigDecimal(columnIndex: Int): BigDecimal =
    underlying.getBigDecimal(columnIndex)
  def getBigDecimal(columnLabel: String): BigDecimal =
    underlying.getBigDecimal(columnLabel)
  def getClob(columnIndex: Int): Clob = underlying.getClob(columnIndex)
  def getClob(columnLabel: String): Clob = underlying.getClob(columnLabel)
  def getRow: Int = underlying.getRow
  def getLong(columnIndex: Int): Long = underlying.getLong(columnIndex)
  def getLong(columnLabel: String): Long = underlying.getLong(columnLabel)
  def getHoldability: Int = underlying.getHoldability
  def updateFloat(columnIndex: Int, x: Float): Unit =
    underlying.updateFloat(columnIndex, x)
  def updateFloat(columnLabel: String, x: Float): Unit =
    underlying.updateFloat(columnLabel, x)
  def afterLast(): Unit = underlying.afterLast()
  def refreshRow(): Unit = underlying.refreshRow()
  def getNString(columnIndex: Int): String = underlying.getNString(columnIndex)
  def getNString(columnLabel: String): String =
    underlying.getNString(columnLabel)
  def deleteRow(): Unit = underlying.deleteRow()
  def getConcurrency: Int = underlying.getConcurrency
  def updateObject(columnIndex: Int, x: scala.Any, scaleOrLength: Int): Unit =
    underlying.updateObject(columnIndex, x, scaleOrLength)
  def updateObject(columnIndex: Int, x: scala.Any): Unit =
    underlying.updateObject(columnIndex, x)
  def updateObject(
    columnLabel: String,
    x: scala.Any,
    scaleOrLength: Int
  ): Unit = underlying.updateObject(columnLabel, x, scaleOrLength)
  def updateObject(columnLabel: String, x: scala.Any): Unit =
    underlying.updateObject(columnLabel, x)
  override def updateObject(
    columnIndex: Int,
    x: scala.Any,
    targetSqlType: SQLType,
    scaleOrLength: Int
  ): Unit =
    underlying.updateObject(columnIndex, x, targetSqlType, scaleOrLength)
  override def updateObject(
    columnIndex: Int,
    x: scala.Any,
    targetSqlType: SQLType
  ): Unit = underlying.updateObject(columnIndex, x, targetSqlType)
  override def updateObject(
    columnLabel: String,
    x: scala.Any,
    targetSqlType: SQLType,
    scaleOrLength: Int
  ): Unit =
    underlying.updateObject(columnLabel, x, targetSqlType, scaleOrLength)
  override def updateObject(
    columnLabel: String,
    x: scala.Any,
    targetSqlType: SQLType
  ): Unit = underlying.updateObject(columnLabel, x, targetSqlType)
  def getFetchSize: Int = underlying.getFetchSize
  def getTime(columnIndex: Int): Time = underlying.getTime(columnIndex)
  def getTime(columnLabel: String): Time = underlying.getTime(columnLabel)
  def getTime(columnIndex: Int, cal: Calendar): Time =
    underlying.getTime(columnIndex, cal)
  def getTime(columnLabel: String, cal: Calendar): Time =
    underlying.getTime(columnLabel, cal)
  def updateCharacterStream(columnIndex: Int, x: Reader, length: Int): Unit =
    underlying.updateCharacterStream(columnIndex, x, length)
  def updateCharacterStream(
    columnLabel: String,
    reader: Reader,
    length: Int
  ): Unit = underlying.updateCharacterStream(columnLabel, reader, length)
  def updateCharacterStream(columnIndex: Int, x: Reader, length: Long): Unit =
    underlying.updateCharacterStream(columnIndex, x, length)
  def updateCharacterStream(
    columnLabel: String,
    reader: Reader,
    length: Long
  ): Unit = underlying.updateCharacterStream(columnLabel, reader, length)
  def updateCharacterStream(columnIndex: Int, x: Reader): Unit =
    underlying.updateCharacterStream(columnIndex, x)
  def updateCharacterStream(columnLabel: String, reader: Reader): Unit =
    underlying.updateCharacterStream(columnLabel, reader)
  def getByte(columnIndex: Int): Byte = underlying.getByte(columnIndex)
  def getByte(columnLabel: String): Byte = underlying.getByte(columnLabel)
  def getBoolean(columnIndex: Int): Boolean = underlying.getBoolean(columnIndex)
  def getBoolean(columnLabel: String): Boolean =
    underlying.getBoolean(columnLabel)
  def setFetchDirection(direction: Int): Unit =
    underlying.setFetchDirection(direction)
  def getFetchDirection: Int = underlying.getFetchDirection
  def updateRef(columnIndex: Int, x: Ref): Unit =
    underlying.updateRef(columnIndex, x)
  def updateRef(columnLabel: String, x: Ref): Unit =
    underlying.updateRef(columnLabel, x)
  def getAsciiStream(columnIndex: Int): InputStream =
    underlying.getAsciiStream(columnIndex)
  def getAsciiStream(columnLabel: String): InputStream =
    underlying.getAsciiStream(columnLabel)
  def getShort(columnIndex: Int): Short = underlying.getShort(columnIndex)
  def getShort(columnLabel: String): Short = underlying.getShort(columnLabel)
  def updateShort(columnIndex: Int, x: Short): Unit =
    underlying.updateShort(columnIndex, x)
  def updateShort(columnLabel: String, x: Short): Unit =
    underlying.updateShort(columnLabel, x)
  def getNCharacterStream(columnIndex: Int): Reader =
    underlying.getNCharacterStream(columnIndex)
  def getNCharacterStream(columnLabel: String): Reader =
    underlying.getNCharacterStream(columnLabel)
  def close(): Unit = underlying.close()
  def relative(rows: Int): Boolean = underlying.relative(rows)
  def updateInt(columnIndex: Int, x: Int): Unit =
    underlying.updateInt(columnIndex, x)
  def updateInt(columnLabel: String, x: Int): Unit =
    underlying.updateInt(columnLabel, x)
  def wasNull(): Boolean = underlying.wasNull()
  def rowUpdated(): Boolean = underlying.rowUpdated()
  def getRef(columnIndex: Int): Ref = underlying.getRef(columnIndex)
  def getRef(columnLabel: String): Ref = underlying.getRef(columnLabel)
  def updateLong(columnIndex: Int, x: Long): Unit =
    underlying.updateLong(columnIndex, x)
  def updateLong(columnLabel: String, x: Long): Unit =
    underlying.updateLong(columnLabel, x)
  def moveToCurrentRow(): Unit = underlying.moveToCurrentRow()
  def isClosed: Boolean = underlying.isClosed
  def updateClob(columnIndex: Int, x: Clob): Unit =
    underlying.updateClob(columnIndex, x)
  def updateClob(columnLabel: String, x: Clob): Unit =
    underlying.updateClob(columnLabel, x)
  def updateClob(columnIndex: Int, reader: Reader, length: Long): Unit =
    underlying.updateClob(columnIndex, reader, length)
  def updateClob(columnLabel: String, reader: Reader, length: Long): Unit =
    underlying.updateClob(columnLabel, reader, length)
  def updateClob(columnIndex: Int, reader: Reader): Unit =
    underlying.updateClob(columnIndex, reader)
  def updateClob(columnLabel: String, reader: Reader): Unit =
    underlying.updateClob(columnLabel, reader)
  def findColumn(columnLabel: String): Int = underlying.findColumn(columnLabel)
  def getWarnings: SQLWarning = underlying.getWarnings
  def getDate(columnIndex: Int): Date = underlying.getDate(columnIndex)
  def getDate(columnLabel: String): Date = underlying.getDate(columnLabel)
  def getDate(columnIndex: Int, cal: Calendar): Date =
    underlying.getDate(columnIndex)
  def getDate(columnLabel: String, cal: Calendar): Date =
    underlying.getDate(columnLabel)
  def getCursorName: String = underlying.getCursorName
  def updateNull(columnIndex: Int): Unit = underlying.updateNull(columnIndex)
  def updateNull(columnLabel: String): Unit = underlying.updateNull(columnLabel)
  def getStatement: Statement = underlying.getStatement
  def cancelRowUpdates(): Unit = underlying.cancelRowUpdates()
  def getSQLXML(columnIndex: Int): SQLXML = underlying.getSQLXML(columnIndex)
  def getSQLXML(columnLabel: String): SQLXML = underlying.getSQLXML(columnLabel)
  def getUnicodeStream(columnIndex: Int): InputStream =
    underlying.getUnicodeStream(columnIndex)
  def getUnicodeStream(columnLabel: String): InputStream =
    underlying.getUnicodeStream(columnLabel)
  def getInt(columnIndex: Int): Int = underlying.getInt(columnIndex)
  def getInt(columnLabel: String): Int = underlying.getInt(columnLabel)
  def updateTime(columnIndex: Int, x: Time): Unit =
    underlying.updateTime(columnIndex, x)
  def updateTime(columnLabel: String, x: Time): Unit =
    underlying.updateTime(columnLabel, x)
  def setFetchSize(rows: Int): Unit = underlying.setFetchSize(rows)
  def previous(): Boolean = underlying.previous()
  def updateAsciiStream(columnIndex: Int, x: InputStream, length: Int): Unit =
    underlying.updateAsciiStream(columnIndex, x, length)
  def updateAsciiStream(
    columnLabel: String,
    x: InputStream,
    length: Int
  ): Unit = underlying.updateAsciiStream(columnLabel, x, length)
  def updateAsciiStream(columnIndex: Int, x: InputStream, length: Long): Unit =
    underlying.updateAsciiStream(columnIndex, x, length)
  def updateAsciiStream(
    columnLabel: String,
    x: InputStream,
    length: Long
  ): Unit = underlying.updateAsciiStream(columnLabel, x, length)
  def updateAsciiStream(columnIndex: Int, x: InputStream): Unit =
    underlying.updateAsciiStream(columnIndex, x)
  def updateAsciiStream(columnLabel: String, x: InputStream): Unit =
    underlying.updateAsciiStream(columnLabel, x)
  def rowDeleted(): Boolean = underlying.rowDeleted()
  def getBlob(columnIndex: Int): Blob = underlying.getBlob(columnIndex)
  def getBlob(columnLabel: String): Blob = underlying.getBlob(columnLabel)
  def first(): Boolean = underlying.first()
  def getBytes(columnIndex: Int): scala.Array[Byte] =
    underlying.getBytes(columnIndex)
  def getBytes(columnLabel: String): scala.Array[Byte] =
    underlying.getBytes(columnLabel)
  def updateBytes(columnIndex: Int, x: scala.Array[Byte]): Unit =
    underlying.updateBytes(columnIndex, x)
  def updateBytes(columnLabel: String, x: scala.Array[Byte]): Unit =
    underlying.updateBytes(columnLabel, x)
  def updateSQLXML(columnIndex: Int, xmlObject: SQLXML): Unit =
    underlying.updateSQLXML(columnIndex, xmlObject)
  def updateSQLXML(columnLabel: String, xmlObject: SQLXML): Unit =
    underlying.updateSQLXML(columnLabel, xmlObject)
  def getString(columnIndex: Int): String = underlying.getString(columnIndex)
  def getString(columnLabel: String): String = underlying.getString(columnLabel)
  def unwrap[T](iface: Class[T]): T = underlying.unwrap(iface)
  def isWrapperFor(iface: Class[?]): Boolean = underlying.isWrapperFor(iface)

}
