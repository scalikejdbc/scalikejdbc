package scalikejdbc

import java.sql.Connection
import scalikejdbc.metadata._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

/**
 * Basic Database Accessor
 *
 * Using DBSession:
 *
 * {{{
 *   import scalikejdbc._
 *   case class User(id: Int, name: String)
 *
 *   using(ConnectionPool(name).borrow()) { conn =>
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
 *     val users = DB(conn) readOnly { implicit session =>
 *       SQL("select * from user").map { rs =>
 *         User(rs.int("id"), rs.string("name"))
 *       }.list.apply()
 *     }
 *
 *     DB(conn) autoCommit { implicit session =>
 *       SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *     }
 *
 *     DB(conn) localTx { implicit session =>
 *       SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *     }
 *
 *   }
 * }}}
 */
case class DB(
  conn: Connection,
  override val connectionAttributes: DBConnectionAttributes =
    DBConnectionAttributes(),
  settingsProvider: SettingsProvider = SettingsProvider.default
) extends DBConnection

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
 *   val users = DB readOnly { implicit session =>
 *     SQL("select * from user").map { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }.list.apply()
 *   }
 *
 *   DB autoCommit { implicit session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   DB localTx { implicit session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   using(DB(ConnectionPool.borrow())) { db =>
 *     db.begin()
 *     try {
 *       DB withTx { implicit session =>
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
object DB extends LoanPattern {

  type CPContext = ConnectionPoolContext
  val NoCPContext = NoConnectionPoolContext

  private[this] def ensureDBInstance(db: DB): Unit = {
    if (db == null) {
      throw new IllegalStateException(
        ErrorMessage.IMPLICIT_DB_INSTANCE_REQUIRED
      )
    }
  }

  private[this] def connectionPool(context: CPContext): ConnectionPool =
    Option(context match {
      case NoCPContext => ConnectionPool()
      case _: MultipleConnectionPoolContext =>
        context.get(ConnectionPool.DEFAULT_NAME)
      case _ =>
        throw new IllegalStateException(
          ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT
        )
    }) getOrElse {
      throw new IllegalStateException(
        ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED
      )
    }

  /**
   * Provides default TxBoundary type class instance.
   */
  private[this] def defaultTxBoundary[A]: TxBoundary[A] =
    TxBoundary.Exception.exceptionTxBoundary[A]

  /**
   * Begins a read-only block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def readOnly[A](execution: DBSession => A)(implicit
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes, settings)
        .autoClose(false)
        .readOnly(execution)
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
  def readOnlyWithConnection[A](execution: Connection => A)(implicit
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes, settings)
        .autoClose(false)
        .readOnlyWithConnection(execution)
    }
  }

  /**
   * Returns read-only session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def readOnlySession(
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): DBSession = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).readOnlySession()
  }

  /**
   * Begins a auto-commit block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def autoCommit[A](execution: DBSession => A)(implicit
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes, settings)
        .autoClose(false)
        .autoCommit(execution)
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
  def autoCommitWithConnection[A](execution: Connection => A)(implicit
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes, settings)
        .autoClose(false)
        .autoCommitWithConnection(execution)
    }
  }

  /**
   * Returns auto-commit session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def autoCommitSession(
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): DBSession = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).autoCommitSession()
  }

  /**
   * Begins a local-tx block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def localTx[A](execution: DBSession => A)(implicit
    context: CPContext = NoCPContext,
    boundary: TxBoundary[A] = defaultTxBoundary[A],
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings)
      .autoClose(true)
      .localTx(execution)
  }

  /**
   * Begins a local-tx block that returns a Future value easily with ConnectionPool.
   *
   * @param execution execution that returns a future value
   * @param context connection pool context
   * @tparam A future result type
   * @return future result value
   */
  def futureLocalTx[A](execution: DBSession => Future[A])(implicit
    context: CPContext = NoCPContext,
    ec: ExecutionContext,
    settings: SettingsProvider = SettingsProvider.default
  ): Future[A] = {
    // Enable TxBoundary implicits
    import scalikejdbc.TxBoundary.Future._
    Try(localTx(execution)(context, implicitly, settings))
      .fold(err => Future.failed(err), a => a)
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
  def localTxWithConnection[A](execution: Connection => A)(implicit
    context: CPContext = NoCPContext,
    boundary: TxBoundary[A] = defaultTxBoundary[A],
    settings: SettingsProvider = SettingsProvider.default
  ): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes, settings)
        .autoClose(false)
        .localTxWithConnection(execution)
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
  def withinTxWithConnection[A](
    execution: Connection => A
  )(implicit db: DB): A = {
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
  def getTableNames(
    tableNamePattern: String,
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): List[String] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings)
      .getTableNames(tableNamePattern)
  }

  /**
   * Returns all the table names
   *
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getAllTableNames(
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): List[String] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).getTableNames("%")
  }

  /**
   * Returns table information
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getTable(
    table: String,
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): Option[Table] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).getTable(table)
  }

  /**
   * Returns all table informations
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table informations
   */
  def getTables(
    table: String,
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): Seq[Table] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).getTables(table)
  }

  def getColumnNames(
    table: String,
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): List[String] = {
    if (table != null) {
      val cp = connectionPool(context)
      DB(cp.borrow(), cp.connectionAttributes, settings).getColumnNames(table)
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
  def showTables(
    tableNamePattern: String = "%",
    tableTypes: Array[String] = Array("TABLE", "VIEW"),
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): String = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings)
      .showTables(tableNamePattern, tableTypes)
  }

  /**
   * Returns describe style string value for the table
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return described information
   */
  def describe(
    table: String,
    settings: SettingsProvider = SettingsProvider.default
  )(implicit context: CPContext = NoCPContext): String = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes, settings).describe(table)
  }

  /**
   * Get a connection and returns a DB instance.
   *
   * @param conn connection
   * @return DB instance
   */
  def connect(
    conn: Connection = ConnectionPool.borrow(),
    settings: SettingsProvider = SettingsProvider.default
  ): DB = DB(conn, DBConnectionAttributes(), settings)

  /**
   * Returns a DB instance by using an implicit Connection object.
   *
   * @param conn connection
   * @return  DB instance
   */
  def connected(implicit
    conn: Connection,
    settings: SettingsProvider = SettingsProvider.default
  ): DB = DB(conn, DBConnectionAttributes(), settings)
}
