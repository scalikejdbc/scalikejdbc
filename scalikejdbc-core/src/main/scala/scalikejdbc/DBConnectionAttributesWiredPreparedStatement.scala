package scalikejdbc

import java.io.{ InputStream, Reader }
import java.math.BigDecimal
import java.net.URL
import java.sql.{ SQLWarning, _ }
import java.util.Calendar

/**
 * PreparedStatement proxy which holds current DBConnectionAttributes.
 */
private[scalikejdbc] class DBConnectionAttributesWiredPreparedStatement(
  underlying: PreparedStatement,
  connAttributes: DBConnectionAttributes
) extends PreparedStatement {

  /**
   * Converts Timestamp value to an appropriate timezone.
   */
  private[this] def convertTimeZoneIfNeeded(
    timestamp: java.sql.Timestamp
  ): java.sql.Timestamp =
    if (connAttributes.timeZoneSettings.conversionEnabled) {
      val clientTimeZone = java.util.TimeZone.getDefault
      val serverTimeZone = connAttributes.timeZoneSettings.serverTimeZone
      TimeZoneConverter
        .from(clientTimeZone)
        .to(serverTimeZone)
        .convert(timestamp)
    } else {
      timestamp
    }

  // --------------------------------------------
  // Converts timezone if needed
  // --------------------------------------------

  def setTimestamp(parameterIndex: Int, x: Timestamp): Unit =
    underlying.setTimestamp(parameterIndex, convertTimeZoneIfNeeded(x))
  def setTimestamp(parameterIndex: Int, x: Timestamp, cal: Calendar): Unit =
    underlying.setTimestamp(parameterIndex, convertTimeZoneIfNeeded(x), cal)

  // --------------------------------------------
  // Just delegates to underlying methods
  // --------------------------------------------

  def setByte(parameterIndex: Int, x: Byte): Unit =
    underlying.setByte(parameterIndex, x)
  def getParameterMetaData: ParameterMetaData = underlying.getParameterMetaData
  def setRef(parameterIndex: Int, x: Ref): Unit =
    underlying.setRef(parameterIndex, x)
  def clearParameters(): Unit = underlying.clearParameters()
  def setBytes(parameterIndex: Int, x: scala.Array[Byte]): Unit =
    underlying.setBytes(parameterIndex, x)
  def setBinaryStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    underlying.setBinaryStream(parameterIndex, x, length)
  def setBinaryStream(parameterIndex: Int, x: InputStream, length: Long): Unit =
    underlying.setBinaryStream(parameterIndex, x, length)
  def setBinaryStream(parameterIndex: Int, x: InputStream): Unit =
    underlying.setBinaryStream(parameterIndex, x)
  def setAsciiStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    underlying.setAsciiStream(parameterIndex, x, length)
  def setAsciiStream(parameterIndex: Int, x: InputStream, length: Long): Unit =
    underlying.setAsciiStream(parameterIndex, x, length)
  def setAsciiStream(parameterIndex: Int, x: InputStream): Unit =
    underlying.setAsciiStream(parameterIndex, x)
  def setObject(parameterIndex: Int, x: scala.Any, targetSqlType: Int): Unit =
    underlying.setObject(parameterIndex, x, targetSqlType)
  def setObject(parameterIndex: Int, x: scala.Any): Unit =
    underlying.setObject(parameterIndex, x)
  def setObject(
    parameterIndex: Int,
    x: scala.Any,
    targetSqlType: Int,
    scaleOrLength: Int
  ): Unit =
    underlying.setObject(parameterIndex, x, targetSqlType, scaleOrLength)
  override def setObject(
    parameterIndex: Int,
    x: scala.Any,
    targetSqlType: SQLType
  ): Unit = underlying.setObject(parameterIndex, x, targetSqlType)
  override def setObject(
    parameterIndex: Int,
    x: scala.Any,
    targetSqlType: SQLType,
    scaleOrLength: Int
  ): Unit =
    underlying.setObject(parameterIndex, x, targetSqlType, scaleOrLength)
  def setDate(parameterIndex: Int, x: Date): Unit =
    underlying.setDate(parameterIndex, x)
  def setDate(parameterIndex: Int, x: Date, cal: Calendar): Unit =
    underlying.setDate(parameterIndex, x, cal)
  def setUnicodeStream(parameterIndex: Int, x: InputStream, length: Int): Unit =
    underlying.setUnicodeStream(parameterIndex, x, length)
  def getMetaData: ResultSetMetaData = underlying.getMetaData
  def setBlob(parameterIndex: Int, x: Blob): Unit =
    underlying.setBlob(parameterIndex, x)
  def setBlob(
    parameterIndex: Int,
    inputStream: InputStream,
    length: Long
  ): Unit = underlying.setBlob(parameterIndex, inputStream, length)
  def setBlob(parameterIndex: Int, inputStream: InputStream): Unit =
    underlying.setBlob(parameterIndex, inputStream)
  def addBatch(): Unit = underlying.addBatch()
  def execute(): Boolean = underlying.execute()
  def executeQuery(): ResultSet = underlying.executeQuery()
  def setNClob(parameterIndex: Int, value: NClob): Unit =
    underlying.setNClob(parameterIndex, value)
  def setNClob(parameterIndex: Int, reader: Reader, length: Long): Unit =
    underlying.setNClob(parameterIndex, reader, length)
  def setNClob(parameterIndex: Int, reader: Reader): Unit =
    underlying.setNClob(parameterIndex, reader)
  def setArray(parameterIndex: Int, x: Array): Unit =
    underlying.setArray(parameterIndex, x)
  def setNCharacterStream(
    parameterIndex: Int,
    value: Reader,
    length: Long
  ): Unit = underlying.setNCharacterStream(parameterIndex, value, length)
  def setNCharacterStream(parameterIndex: Int, value: Reader): Unit =
    underlying.setNCharacterStream(parameterIndex, value)
  def setURL(parameterIndex: Int, x: URL): Unit =
    underlying.setURL(parameterIndex, x)
  def setRowId(parameterIndex: Int, x: RowId): Unit =
    underlying.setRowId(parameterIndex, x)
  def setSQLXML(parameterIndex: Int, xmlObject: SQLXML): Unit =
    underlying.setSQLXML(parameterIndex, xmlObject)
  def setString(parameterIndex: Int, x: String): Unit =
    underlying.setString(parameterIndex, x)
  def setFloat(parameterIndex: Int, x: Float): Unit =
    underlying.setFloat(parameterIndex, x)
  def setNString(parameterIndex: Int, value: String): Unit =
    underlying.setNString(parameterIndex, value)
  def setBoolean(parameterIndex: Int, x: Boolean): Unit =
    underlying.setBoolean(parameterIndex, x)
  def setDouble(parameterIndex: Int, x: Double): Unit =
    underlying.setDouble(parameterIndex, x)
  def setBigDecimal(parameterIndex: Int, x: BigDecimal): Unit =
    underlying.setBigDecimal(parameterIndex, x)
  def executeUpdate(): Int = underlying.executeUpdate()
  override def executeLargeUpdate(): Long = underlying.executeLargeUpdate()
  def setTime(parameterIndex: Int, x: Time): Unit =
    underlying.setTime(parameterIndex, x)
  def setTime(parameterIndex: Int, x: Time, cal: Calendar): Unit =
    underlying.setTime(parameterIndex, x, cal)
  def setShort(parameterIndex: Int, x: Short): Unit =
    underlying.setShort(parameterIndex, x)
  def setLong(parameterIndex: Int, x: Long): Unit =
    underlying.setLong(parameterIndex, x)
  def setCharacterStream(
    parameterIndex: Int,
    reader: Reader,
    length: Int
  ): Unit = underlying.setCharacterStream(parameterIndex, reader, length)
  def setCharacterStream(
    parameterIndex: Int,
    reader: Reader,
    length: Long
  ): Unit = underlying.setCharacterStream(parameterIndex, reader, length)
  def setCharacterStream(parameterIndex: Int, reader: Reader): Unit =
    underlying.setCharacterStream(parameterIndex, reader)
  def setClob(parameterIndex: Int, x: Clob): Unit =
    underlying.setClob(parameterIndex, x)
  def setClob(parameterIndex: Int, reader: Reader, length: Long): Unit =
    underlying.setClob(parameterIndex, reader, length)
  def setClob(parameterIndex: Int, reader: Reader): Unit =
    underlying.setClob(parameterIndex, reader)
  def setNull(parameterIndex: Int, sqlType: Int): Unit =
    underlying.setNull(parameterIndex, sqlType)
  def setNull(parameterIndex: Int, sqlType: Int, typeName: String): Unit =
    underlying.setNull(parameterIndex, sqlType, typeName)
  def setInt(parameterIndex: Int, x: Int): Unit =
    underlying.setInt(parameterIndex, x)
  def setMaxFieldSize(max: Int): Unit = underlying.setMaxFieldSize(max)
  def getMoreResults: Boolean = underlying.getMoreResults
  def getMoreResults(current: Int): Boolean = underlying.getMoreResults(current)
  def clearWarnings(): Unit = underlying.clearWarnings()
  def getGeneratedKeys: ResultSet = underlying.getGeneratedKeys
  def closeOnCompletion(): Unit = underlying.closeOnCompletion()
  def cancel(): Unit = underlying.cancel()
  def getResultSet: ResultSet = underlying.getResultSet
  def setPoolable(poolable: Boolean): Unit = underlying.setPoolable(poolable)
  def isPoolable: Boolean = underlying.isPoolable
  def setCursorName(name: String): Unit = underlying.setCursorName(name)
  def getUpdateCount: Int = underlying.getUpdateCount
  override def getLargeUpdateCount: Long = underlying.getLargeUpdateCount
  def addBatch(sql: String): Unit = underlying.addBatch(sql)
  def getMaxRows: Int = underlying.getMaxRows
  override def getLargeMaxRows: Long = underlying.getLargeMaxRows
  def execute(sql: String): Boolean = underlying.execute(sql)
  def execute(sql: String, autoGeneratedKeys: Int): Boolean =
    underlying.execute(sql, autoGeneratedKeys)
  def execute(sql: String, columnIndexes: scala.Array[Int]): Boolean =
    underlying.execute(sql, columnIndexes)
  def execute(sql: String, columnNames: scala.Array[String]): Boolean =
    underlying.execute(sql, columnNames)
  def executeQuery(sql: String): ResultSet = underlying.executeQuery(sql)
  def getResultSetType: Int = underlying.getResultSetType
  def setMaxRows(max: Int): Unit = underlying.setMaxRows(max)
  def getFetchSize: Int = underlying.getFetchSize
  def getResultSetHoldability: Int = underlying.getResultSetHoldability
  def setFetchDirection(direction: Int): Unit =
    underlying.setFetchDirection(direction)
  def getFetchDirection: Int = underlying.getFetchDirection
  def getResultSetConcurrency: Int = underlying.getResultSetConcurrency
  def clearBatch(): Unit = underlying.clearBatch()
  def close(): Unit = underlying.close()
  def isClosed: Boolean = underlying.isClosed
  def executeUpdate(sql: String): Int = underlying.executeUpdate(sql)
  def executeUpdate(sql: String, autoGeneratedKeys: Int): Int =
    underlying.executeUpdate(sql, autoGeneratedKeys)
  def executeUpdate(sql: String, columnIndexes: scala.Array[Int]): Int =
    underlying.executeUpdate(sql, columnIndexes)
  def executeUpdate(sql: String, columnNames: scala.Array[String]): Int =
    underlying.executeUpdate(sql, columnNames)
  override def executeLargeUpdate(sql: String): Long =
    underlying.executeLargeUpdate(sql)
  override def executeLargeUpdate(sql: String, autoGeneratedKeys: Int): Long =
    underlying.executeLargeUpdate(sql, autoGeneratedKeys)
  override def executeLargeUpdate(
    sql: String,
    columnIndexes: scala.Array[Int]
  ): Long = underlying.executeLargeUpdate(sql, columnIndexes)
  override def executeLargeUpdate(
    sql: String,
    columnNames: scala.Array[String]
  ): Long = underlying.executeLargeUpdate(sql, columnNames)
  def getQueryTimeout: Int = underlying.getQueryTimeout
  def getWarnings: SQLWarning = underlying.getWarnings
  def setFetchSize(rows: Int): Unit = underlying.setFetchSize(rows)
  def setQueryTimeout(seconds: Int): Unit = underlying.setQueryTimeout(seconds)
  def executeBatch(): scala.Array[Int] = underlying.executeBatch()
  def setEscapeProcessing(enable: Boolean): Unit =
    underlying.setEscapeProcessing(enable)
  def getConnection: Connection = underlying.getConnection
  def getMaxFieldSize: Int = underlying.getMaxFieldSize
  def isCloseOnCompletion: Boolean = underlying.isCloseOnCompletion
  def unwrap[T](iface: Class[T]): T = underlying.unwrap(iface)
  def isWrapperFor(iface: Class[_]): Boolean = underlying.isWrapperFor(iface)
}
