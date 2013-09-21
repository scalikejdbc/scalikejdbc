/*
 * Copyright 2011 Kazuhiro Sera
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

import java.sql.{ DatabaseMetaData, Connection }
import java.lang.IllegalStateException
import scala.util.control.Exception._

import scalikejdbc.metadata._

/**
 * Basic Database Accessor
 *
 * You can start with DB and blocks if using [[scalikejdbc.ConnectionPool.singleton()]].
 *
 * Using DBSession:
 *
 * {{{
 *   ConnectionPool.singleton("jdbc:...","user","password")
 *   case class User(id: Int, name: String)
 *
 *   val users = DB readOnly { session =>
 *     session.list("select * from user") { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }
 *   }
 *
 *   DB autoCommit { session =>
 *     session.update("insert into user values (?,?)", 123, "Alice")
 *   }
 *
 *   DB localTx { session =>
 *     session.update("insert into user values (?,?)", 123, "Alice")
 *   }
 *
 *   using(DB(ConnectionPool.borrow())) { db =>
 *     db.begin()
 *     try {
 *       DB withTx { session =>
 *         session.update("update user set name = ? where id = ?", "Alice", 123)
 *       }
 *       db.commit()
 *     } catch { case e =>
 *       db.rollbackIfActive()
 *       throw e
 *     }
 *   }
 * }}}
 *
 * Using SQL:
 *
 * {{{
 *   ConnectionPool.singleton("jdbc:...","user","password")
 *   case class User(id: Int, name: String)
 *
 *   val users = DB readOnly { session =>
 *     SQL("select * from user").map { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }.list.apply()
 *   }
 *
 *   DB autoCommit { session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   DB localTx { session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   using(DB(ConnectionPool.borrow())) { db =>
 *     db.begin()
 *     try {
 *       DB withTx { session =>
 *         SQL("update user set name = ? where id = ?").bind("Alice", 123).update.apply()
 *       }
 *       db.commit()
 *     } catch { case e =>
 *       db.rollbackIfActive()
 *       throw e
 *     }
 *   }
 * }}}
 */
object DB {

  type CPContext = ConnectionPoolContext
  val NoCPContext = NoConnectionPoolContext

  private def ensureDBInstance(db: DB): Unit = {
    if (db == null) {
      throw new IllegalStateException(ErrorMessage.IMPLICIT_DB_INSTANCE_REQUIRED)
    }
  }

  private def connectionPool(context: CPContext): ConnectionPool = opt(context match {
    case NoCPContext => ConnectionPool()
    case _: MultipleConnectionPoolContext => context.get(ConnectionPool.DEFAULT_NAME)
    case _ => throw new IllegalStateException(ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT)
  }) getOrElse {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  /**
   * Begins a read-only block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def readOnly[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).readOnly(execution)
    }
  }

  /**
   * Begins a read-only block easily with ConnectionPool
   * and pass not session but connection to execution block.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def readOnlyWithConnection[A](execution: Connection => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).readOnlyWithConnection(execution)
    }
  }

  /**
   * Returns read-only session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def readOnlySession()(implicit context: CPContext = NoCPContext): DBSession = {
    DB(connectionPool(context).borrow()).readOnlySession()
  }

  /**
   * Begins a auto-commit block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def autoCommit[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).autoCommit(execution)
    }
  }

  /**
   * Begins a auto-commit block easily with ConnectionPool
   * and pass not session but connection to execution block.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def autoCommitWithConnection[A](execution: Connection => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).autoCommitWithConnection(execution)
    }
  }

  /**
   * Returns auto-commit session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def autoCommitSession()(implicit context: CPContext = NoCPContext): DBSession = {
    DB(connectionPool(context).borrow()).autoCommitSession()
  }

  /**
   * Begins a local-tx block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def localTx[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).localTx(execution)
    }
  }

  /**
   * Begins a local-tx block easily with ConnectionPool
   * and pass not session but connection to execution block.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def localTxWithConnection[A](execution: Connection => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).localTxWithConnection(execution)
    }
  }

  /**
   * Begins a within-tx block easily with a DB instance as an implicit parameter.
   *
   * @param execution execution
   * @param db DB instance as an implicit parameter
   * @tparam A return type
   * @return result value
   */
  def withinTx[A](execution: DBSession => A)(implicit db: DB): A = {
    ensureDBInstance(db: DB)
    db.withinTx(execution)
  }

  /**
   * Begins a within-tx block easily with a DB instance as an implicit parameter
   * and pass not session but connection to execution block.
   *
   * @param execution execution
   * @param db DB instance as an implicit parameter
   * @tparam A return type
   * @return result value
   */
  def withinTxWithConnection[A](execution: Connection => A)(implicit db: DB): A = {
    ensureDBInstance(db: DB)
    db.withinTxWithConnection(execution)
  }

  /**
   * Returns within-tx session instance. You SHOULD close this instance by yourself.
   *
   * @param db DB instance as an implicit parameter
   * @return session
   */
  def withinTxSession()(implicit db: DB): DBSession = db.withinTxSession()

  /**
   * Returns multiple table information
   *
   * @param tableNamePattern table name pattern (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getTableNames(tableNamePattern: String)(implicit context: CPContext = NoCPContext): List[String] = {
    DB(connectionPool(context).borrow()).getTableNames(tableNamePattern)
  }

  /**
   * Returns all the table names
   *
   * @param tableNamePattern table name pattern (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getAllTableNames()(implicit context: CPContext = NoCPContext): List[String] = {
    DB(connectionPool(context).borrow()).getTableNames("%")
  }

  /**
   * Returns table information
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getTable(table: String)(implicit context: CPContext = NoCPContext): Option[Table] = {
    DB(connectionPool(context).borrow()).getTable(table)
  }

  def getColumnNames(table: String)(implicit context: CPContext = NoCPContext): List[String] = {
    if (table != null) {
      DB(connectionPool(context).borrow()).getColumnNames(table)
    } else {
      Nil
    }
  }

  /**
   * Returns table name list
   *
   * @param tableNamePattern table name pattern (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table name list
   */
  def showTables(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW"))(implicit context: CPContext = NoCPContext): String = {
    DB(connectionPool(context).borrow()).showTables(tableNamePattern, tableTypes)
  }

  /**
   * Returns describe style string value for the table
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return described information
   */
  def describe(table: String)(implicit context: CPContext = NoCPContext): String = {
    DB(connectionPool(context).borrow()).describe(table)
  }

  /**
   * Get a connection and returns a DB instance.
   *
   * @param conn connection
   * @return DB instance
   */
  def connect(conn: Connection = ConnectionPool.borrow()): DB = DB(conn)

  /**
   * Returns a DB instance by using an implicit Connection object.
   *
   * @param conn connection
   * @return  DB instance
   */
  def connected(implicit conn: Connection) = DB(conn)

}

/**
 * Basic Database Accessor
 *
 * Using DBSession:
 *
 * {{{
 *   import scalikejdbc._
 *   case class User(id: Int, name: String)
 *
 *   using(connectionPool(context).borrow()) { conn =>
 *
 *     val users = DB(conn) readOnly { session =>
 *       session.list("select * from user") { rs =>
 *         User(rs.int("id"), rs.string("name"))
 *       }
 *     }
 *
 *     DB(conn) autoCommit { session =>
 *       session.update("insert into user values (?,?)", 123, "Alice")
 *     }
 *
 *     DB(conn) localTx { session =>
 *       session.update("insert into user values (?,?)", 123, "Alice")
 *     }
 *
 *   }
 * }}}
 *
 * Using SQL:
 *
 * {{{
 *   import scalikejdbc._
 *   case class User(id: Int, name: String)
 *
 *   using(ConnectionPool.borrow()) { conn =>
 *
 *     val users = DB(conn) readOnly { session =>
 *       SQL("select * from user").map { rs =>
 *         User(rs.int("id"), rs.string("name"))
 *       }.list.apply()
 *     }
 *
 *     DB(conn) autoCommit { session =>
 *       SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *     }
 *
 *     DB(conn) localTx { session =>
 *       SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *     }
 *
 *   }
 * }}}
 */
case class DB(conn: Connection) extends LogSupport {

  type RSTraversable = ResultSetTraversable

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
    using(conn) { conn =>
      execution(readOnlySession())
    }
  }

  /**
   * Provides read-only session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def readOnlyWithConnection[A](execution: Connection => A): A = {
    // cannot control if jdbc drivers ignore the readOnly attribute.
    using(conn) { conn =>
      execution(readOnlySession().conn)
    }
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
    using(conn) { conn =>
      execution(autoCommitSession())
    }
  }

  /**
   * Provides auto-commit session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def autoCommitWithConnection[A](execution: Connection => A): A = {
    using(conn) { conn =>
      execution(autoCommitSession().conn)
    }
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
    using(conn) { conn =>
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = DBSession(conn, tx = Option(tx))
        val result: A = execution(session)
        tx.commit()
        result
      }
    }
  }

  /**
   * Provides local-tx session block.
   * @param execution block
   * @tparam A  return type
   * @return result value
   */
  def localTxWithConnection[A](execution: Connection => A): A = {
    using(conn) { conn =>
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = DBSession(conn, tx = Option(tx))
        val result: A = execution(session.conn)
        tx.commit()
        result
      }
    }
  }

  /**
   * Splits the name to schema and table name
   *
   * @param name name
   * @return schema and table
   */
  private[this] def toSchemaAndTable(name: String): (String, String) = {
    val schema = if (name.split("\\.").size > 1) name.split("\\.").head else null
    val table = if (name.split("\\.").size > 1) name.split("\\.")(1) else name
    (schema, table)
  }

  /**
   * Returns all the table information that match the pattern
   *
   * @param tableNamePattern table name pattern (with schema optionally)
   * @return table information
   */
  def getTableNames(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW")): List[String] = readOnlyWithConnection { conn =>
    val meta = conn.getMetaData
    val (schema, _tableNamePattern) = toSchemaAndTable(tableNamePattern.replaceAll("\\*", "%"))
    new RSTraversable(meta.getTables(null, schema, _tableNamePattern, tableTypes))
      .map { rs =>
        if (schema != null) schema + "." + rs.string("TABLE_NAME")
        else rs.string("TABLE_NAME")
      }.toList
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
        .orElse(_getTableName(meta, schema, table.toUpperCase, tableTypes))
        .orElse(_getTableName(meta, schema, table.toLowerCase, tableTypes)).map { tableName =>
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
  def getTable(table: String): Option[Table] = readOnlyWithConnection { conn =>
    val meta = conn.getMetaData
    _getTable(meta, table).orElse(_getTable(meta, table.toUpperCase)).orElse(_getTable(meta, table.toLowerCase))
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
            name = rs.string("COLUMN_NAME"),
            typeCode = rs.int("DATA_TYPE"),
            typeName = rs.string("TYPE_NAME"),
            size = rs.int("COLUMN_SIZE"),
            isRequired = rs.string("IS_NULLABLE") != null && rs.string("IS_NULLABLE") == "NO",
            isPrimaryKey = pkNames.exists(_ == rs.string("COLUMN_NAME")),
            isAutoIncrement = {
              // Oracle throws java.sql.SQLException: Invalid column name
              try {
                rs.string("IS_AUTOINCREMENT") != null && rs.string("IS_AUTOINCREMENT") == "YES"
              } catch { case e: java.sql.SQLException => false }
            },
            description = {
              try {
                rs.string("REMARKS")
              } catch { case e: java.sql.SQLException => null }
            },
            defaultValue = {
              // for Oracle support
              try {
                rs.string("COLUMN_DEF")
              } catch { case e: java.sql.SQLException => null }
            }
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
          } catch { case e: java.sql.SQLException => Nil }
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
          } catch { case e: java.sql.SQLException => Nil }
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
