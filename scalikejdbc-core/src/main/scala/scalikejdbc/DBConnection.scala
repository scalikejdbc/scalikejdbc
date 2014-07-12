package scalikejdbc

import java.sql.{ DatabaseMetaData, Connection }
import scalikejdbc.metadata.{ Index, ForeignKey, Column, Table }
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Failure
import scala.util.control.Exception._
import java.util.Locale.{ ENGLISH => en }

/**
 * Basic Database Accessor
 */
trait DBConnection extends LogSupport with LoanPattern {

  type RSTraversable = ResultSetTraversable

  /**
   * Connection wil be closed automatically by default.
   */
  private[this] var autoCloseEnabled: Boolean = true

  /**
   * Switches auto close mode.
   * @param autoClose auto close enabled if true
   */
  def autoClose(autoClose: Boolean): Unit = this.autoCloseEnabled = autoClose

  def conn: Connection

  /**
   * Returns is the current transaction is active.
   * @return result
   */
  def isTxNotActive: Boolean = conn == null || conn.isClosed || conn.isReadOnly

  /**
   * Returns is the current transaction hasn't started yet.
   * @return result
   */
  def isTxNotYetStarted: Boolean = conn != null && conn.getAutoCommit

  /**
   * Returns is the current transaction already started.
   * @return result
   */
  def isTxAlreadyStarted: Boolean = conn != null && !conn.getAutoCommit

  private def newTx(conn: Connection): Tx = {
    conn.setReadOnly(false)
    if (isTxNotActive || isTxAlreadyStarted) {
      throw new IllegalStateException(ErrorMessage.CANNOT_START_A_NEW_TRANSACTION)
    }
    new Tx(conn)
  }

  /**
   * Starts a new transaction and returns it.
   * @return tx
   */
  def newTx: Tx = newTx(conn)

  /**
   * Returns the current transaction.
   * If the transaction has not started yet, IllegalStateException will be thrown.
   * @return tx
   */
  def currentTx: Tx = {
    if (isTxNotActive || isTxNotYetStarted) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    new Tx(conn)
  }

  /**
   * Returns the current transaction.
   * If the transaction has not started yet, IllegalStateException will be thrown.
   * @return tx
   */
  def tx: Tx = {
    handling(classOf[IllegalStateException]) by { e =>
      throw new IllegalStateException(
        ErrorMessage.TRANSACTION_IS_NOT_ACTIVE + " If you want to start a new transaction, use #newTx instead."
      )
    } apply currentTx
  }

  /**
   * Close the connection.
   */
  def close(): Unit = {
    ignoring(classOf[Throwable]) {
      conn.close()
    }
    log.debug("A Connection is closed.")
  }

  /**
   * Begins a new transaction.
   */
  def begin(): Unit = newTx.begin()

  /**
   * Begins a new transaction if the other one does not already start.
   */
  def beginIfNotYet(): Unit = {
    ignoring(classOf[IllegalStateException]) apply {
      begin()
    }
  }

  /**
   * Commits the current transaction.
   */
  def commit(): Unit = tx.commit()

  /**
   * Rolls back the current transaction.
   */
  def rollback(): Unit = tx.rollback()

  /**
   * Rolls back the current transaction if the transaction is still active.
   */
  def rollbackIfActive(): Unit = {
    ignoring(classOf[IllegalStateException]) apply {
      tx.rollbackIfActive()
    }
  }

  /**
   * Returns read-only session.
   * @return session
   */
  def readOnlySession(): DBSession = {
    conn.setReadOnly(true)
    DBSession(conn, isReadOnly = true)
  }

  /**
   * Provides read-only session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def readOnly[A](execution: DBSession => A): A = {
    if (autoCloseEnabled) using(conn)(_ => execution(readOnlySession()))
    else execution(readOnlySession())
  }

  /**
   * Provides read-only session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def readOnlyWithConnection[A](execution: Connection => A): A = {
    // cannot control if jdbc drivers ignore the readOnly attribute.
    if (autoCloseEnabled) using(conn)(_ => execution(readOnlySession().conn))
    else execution(readOnlySession().conn)
  }

  /**
   * Returns auto-commit session.
   * @return session
   */
  def autoCommitSession(): DBSession = {
    conn.setReadOnly(false)
    conn.setAutoCommit(true)
    DBSession(conn)
  }

  /**
   * Provides auto-commit session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def autoCommit[A](execution: DBSession => A): A = {
    if (autoCloseEnabled) using(conn)(_ => execution(autoCommitSession()))
    else execution(autoCommitSession())
  }

  /**
   * Provides auto-commit session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def autoCommitWithConnection[A](execution: Connection => A): A = {
    if (autoCloseEnabled) using(conn)(_ => execution(autoCommitSession().conn))
    else execution(autoCommitSession().conn)
  }

  /**
   * Returns within-tx session.
   * @return session
   */
  def withinTxSession(tx: Tx = currentTx): DBSession = {
    if (!tx.isActive) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    DBSession(conn, tx = Some(tx))
  }

  /**
   * Provides within-tx session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def withinTx[A](execution: DBSession => A): A = {
    execution(withinTxSession(currentTx))
  }

  /**
   * Provides within-tx session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def withinTxWithConnection[A](execution: Connection => A): A = {
    execution(withinTxSession(currentTx).conn)
  }

  private def begin(tx: Tx): Unit = {
    tx.begin()
    if (!tx.isActive) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
  }

  private[this] val rollbackIfThrowable = handling(classOf[Throwable]) by { t =>
    tx.rollback()
    throw t
  }

  /**
   * Provides local-tx session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def localTx[A](execution: DBSession => A): A = {
    def _localTx[A](execution: DBSession => A): A = {
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = DBSession(conn, tx = Option(tx))
        val result: A = execution(session)
        tx.commit()
        result
      }
    }
    if (autoCloseEnabled) using(conn)(_ => _localTx(execution))
    else _localTx(execution)
  }

  /**
   * Easy way to checkout the current connection to be used in a transaction
   * that needs to be committed/rolled back depending on Future results.
   * @param execution block that takes a session and returns a future
   * @tparam A future result type
   * @return future result
   */
  def futureLocalTx[A](execution: DBSession => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    def _futureLocalTx[A](checkedOutConn: Connection, execution: DBSession => Future[A])(implicit ec: ExecutionContext): Future[A] = {
      val tx = newTx(checkedOutConn)
      begin(tx)

      Future(DBSession(checkedOutConn, Some(tx)))
        .flatMap(session => execution(session))
        .map { result =>
          tx.commit()
          result
        }.andThen {
          case _: Failure[_] => tx.rollback()
          case _ =>
        }
    }
    if (autoCloseEnabled) futureUsing(conn) { c => _futureLocalTx(c, execution) }
    else _futureLocalTx(conn, execution)
  }

  /**
   * Provides local-tx session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def localTxWithConnection[A](execution: Connection => A): A = {
    def _localTxWithConnection[A](execution: Connection => A): A = {
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = DBSession(conn, tx = Option(tx))
        val result: A = execution(session.conn)
        tx.commit()
        result
      }
    }
    if (autoCloseEnabled) using(conn)(_ => _localTxWithConnection(execution))
    else _localTxWithConnection(execution)
  }

  /**
   * Splits the name to schema and table name
   *
   * @param name name
   * @return schema and table
   */
  private[this] def toSchemaAndTable(name: String): (String, String) = {
    val schema = {
      if (name.split("\\.").size > 1) {
        val s = name.split("\\.").head
        // H2 Database cannot accept "public" for metadata retrieving
        if (s == "public") null else s
      } else null
    }
    val table = if (name.split("\\.").size > 1) name.split("\\.")(1) else name
    (schema, table)
  }

  /**
   * Returns all the table information that match the pattern
   *
   * @param tableNamePattern table name pattern (with schema optionally)
   * @return table information
   */
  def getTableNames(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW")): List[String] = {
    readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      val (schema, _tableNamePattern) = toSchemaAndTable(tableNamePattern.replaceAll("\\*", "%"))
      new RSTraversable(meta.getTables(null, schema, _tableNamePattern, tableTypes))
        .map { rs =>
          if (schema != null) schema + "." + rs.string("TABLE_NAME")
          else rs.string("TABLE_NAME")
        }.toList
    }
  }

  /**
   * Returns all the column names on the matched table name
   */
  def getColumnNames(tableName: String, tableTypes: Array[String] = Array("TABLE", "VIEW")): List[String] = {
    def _getTableName(meta: DatabaseMetaData, schema: String, table: String, tableTypes: Array[String]): Option[String] = {
      new RSTraversable(meta.getTables(null, schema, table, tableTypes)).map(rs => rs.string("TABLE_NAME")).headOption
    }
    val (schema, table) = toSchemaAndTable(tableName)
    readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      _getTableName(meta, schema, table, tableTypes)
        .orElse(_getTableName(meta, schema, table.toUpperCase(en), tableTypes))
        .orElse(_getTableName(meta, schema, table.toLowerCase(en), tableTypes)).map { tableName =>
          new RSTraversable(meta.getColumns(null, schema, tableName, "%")).map(_.string("COLUMN_NAME")).toList.distinct
        }
    }.getOrElse(Nil)
  }

  /**
   * Returns table information if exists
   *
   * @param table table name (with schema optionally)
   * @return table information
   */
  def getTable(table: String, tableTypes: Array[String] = Array("TABLE", "VIEW")): Option[Table] = {
    readOnlyWithConnection { conn =>
      val meta = conn.getMetaData
      _getTable(meta, table, tableTypes)
        .orElse(_getTable(meta, table.toUpperCase(en), tableTypes))
        .orElse(_getTable(meta, table.toLowerCase(en), tableTypes))
    }
  }

  /**
   * Returns table information if exists
   *
   * @param meta database meta data
   * @param table table name (with schema optionally)
   * @param tableTypes target table types
   * @return table information
   */
  private[this] def _getTable(meta: DatabaseMetaData, table: String, tableTypes: Array[String] = Array("TABLE", "VIEW")): Option[Table] = {
    val (schema, _table) = toSchemaAndTable(table)
    new RSTraversable(meta.getTables(null, schema, _table, tableTypes)).map(rs => rs.string("TABLE_NAME")).headOption.map { tableNameFound =>
      val pkNames: Traversable[String] = new RSTraversable(meta.getPrimaryKeys(null, schema, _table)).map(rs => rs.string("COLUMN_NAME"))

      Table(
        name = _table,
        schema = schema,
        description = new RSTraversable(meta.getTables(null, schema, _table, tableTypes)).map(rs => rs.string("REMARKS")).headOption.orNull[String],
        columns = new RSTraversable(meta.getColumns(null, schema, _table, "%")).map { rs =>
          Column(
            name = try rs.string("COLUMN_NAME") catch { case e: ResultSetExtractorException => null },
            typeCode = try rs.int("DATA_TYPE") catch { case e: ResultSetExtractorException => -1 },
            typeName = rs.string("TYPE_NAME"),
            size = try rs.int("COLUMN_SIZE") catch { case e: ResultSetExtractorException => -1 },
            isRequired = try {
              rs.string("IS_NULLABLE") != null && rs.string("IS_NULLABLE") == "NO"
            } catch { case e: ResultSetExtractorException => false },
            isPrimaryKey = try {
              pkNames.exists(_ == rs.string("COLUMN_NAME"))
            } catch { case e: ResultSetExtractorException => false },
            isAutoIncrement = try {
              // Oracle throws java.sql.SQLException: Invalid column name
              rs.string("IS_AUTOINCREMENT") != null && rs.string("IS_AUTOINCREMENT") == "YES"
            } catch { case e: ResultSetExtractorException => false },
            description = try rs.string("REMARKS") catch { case e: ResultSetExtractorException => null },
            defaultValue = try rs.string("COLUMN_DEF") catch { case e: ResultSetExtractorException => null }
          )
        }.toList.distinct,
        foreignKeys = {
          try {
            new RSTraversable(meta.getImportedKeys(null, schema, _table)).map { rs =>
              ForeignKey(
                name = rs.string("FKCOLUMN_NAME"),
                foreignColumnName = rs.string("PKCOLUMN_NAME"),
                foreignTableName = rs.string("PKTABLE_NAME")
              )
            }.toList.distinct
          } catch { case e: ResultSetExtractorException => Nil }
        },
        indices = {
          try {
            new RSTraversable(meta.getIndexInfo(null, schema, _table, false, true))
              .foldLeft(Map[String, Index]()) {
                case (map, rs) =>
                  val indexName = rs.string("INDEX_NAME")
                  val index = map.get(indexName).map { index =>
                    index.copy(columnNames = rs.string("COLUMN_NAME") :: index.columnNames)
                  }.getOrElse {
                    Index(
                      name = indexName,
                      columnNames = List(rs.string("COLUMN_NAME")),
                      isUnique = !rs.boolean("NON_UNIQUE"))
                  }
                  map.updated(indexName, index)
              }.map { case (k, v) => v }.toList.distinct
          } catch { case e: ResultSetExtractorException => Nil }
        }
      )
    }
  }

  /**
   * Returns table name list
   *
   * @param tableNamePattern table name pattern
   * @param tableTypes table types
   * @return table name list
   */
  def showTables(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW")): String = {
    getTableNames(tableNamePattern, tableTypes).mkString("\n")
  }

  /**
   * Returns describe style string value for the table
   *
   * @param table table name (with schema optionally)
   * @return described information
   */
  def describe(table: String): String = {
    getTable(table).map(t => t.toDescribeStyleString).getOrElse("Not found.")
  }

}