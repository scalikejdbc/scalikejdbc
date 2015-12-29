package scalikejdbc

import java.sql.Connection
import scalikejdbc.metadata._
import scala.concurrent.{ ExecutionContext, Future }

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
  override val connectionAttributes: DBConnectionAttributes = DBConnectionAttributes())
    extends DBConnection

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
      throw new IllegalStateException(ErrorMessage.IMPLICIT_DB_INSTANCE_REQUIRED)
    }
  }

  private[this] def connectionPool(context: CPContext): ConnectionPool = Option(context match {
    case NoCPContext => ConnectionPool()
    case _: MultipleConnectionPoolContext => context.get(ConnectionPool.DEFAULT_NAME)
    case _ => throw new IllegalStateException(ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT)
  }) getOrElse {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  /**
   * Provides default TxBoundary type class instance.
   */
  private[this] def defaultTxBoundary[A]: TxBoundary[A] = TxBoundary.Exception.exceptionTxBoundary[A]

  /**
   * Begins a read-only block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def readOnly[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes).autoClose(false).readOnly(execution)
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
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes).autoClose(false).readOnlyWithConnection(execution)
    }
  }

  /**
   * Returns read-only session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def readOnlySession()(implicit context: CPContext = NoCPContext): DBSession = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).readOnlySession()
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
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes).autoClose(false).autoCommit(execution)
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
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes).autoClose(false).autoCommitWithConnection(execution)
    }
  }

  /**
   * Returns auto-commit session instance. You SHOULD close this instance by yourself.
   *
   * @param context connection pool context
   * @return session
   */
  def autoCommitSession()(implicit context: CPContext = NoCPContext): DBSession = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).autoCommitSession()
  }

  /**
   * Begins a local-tx block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def localTx[A](execution: DBSession => A)(
    implicit context: CPContext = NoCPContext, boundary: TxBoundary[A] = defaultTxBoundary[A]): A = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).autoClose(true).localTx(execution)
  }

  /**
   * Begins a local-tx block that returns a Future value easily with ConnectionPool.
   *
   * @param execution execution that returns a future value
   * @param context connection pool context
   * @tparam A future result type
   * @return future result value
   */
  def futureLocalTx[A](execution: DBSession => Future[A])(implicit context: CPContext = NoCPContext, ec: ExecutionContext): Future[A] = {
    // Enable TxBoundary implicits
    import scalikejdbc.TxBoundary.Future._
    localTx(execution)
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
  def localTxWithConnection[A](execution: Connection => A)(
    implicit context: CPContext = NoCPContext, boundary: TxBoundary[A] = defaultTxBoundary[A]): A = {
    val cp = connectionPool(context)
    using(cp.borrow()) { conn =>
      DB(conn, cp.connectionAttributes).autoClose(false).localTxWithConnection(execution)
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
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).getTableNames(tableNamePattern)
  }

  /**
   * Returns all the table names
   *
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getAllTableNames()(implicit context: CPContext = NoCPContext): List[String] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).getTableNames("%")
  }

  /**
   * Returns table information
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return table information
   */
  def getTable(table: String)(implicit context: CPContext = NoCPContext): Option[Table] = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).getTable(table)
  }

  def getColumnNames(table: String)(implicit context: CPContext = NoCPContext): List[String] = {
    if (table != null) {
      val cp = connectionPool(context)
      DB(cp.borrow(), cp.connectionAttributes).getColumnNames(table)
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
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).showTables(tableNamePattern, tableTypes)
  }

  /**
   * Returns describe style string value for the table
   *
   * @param table table name (with schema optionally)
   * @param context connection pool context as implicit parameter
   * @return described information
   */
  def describe(table: String)(implicit context: CPContext = NoCPContext): String = {
    val cp = connectionPool(context)
    DB(cp.borrow(), cp.connectionAttributes).describe(table)
  }

  /**
   * Get a connection and returns a DB instance.
   *
   * @param conn connection
   * @return DB instance
   */
  def connect(conn: Connection = ConnectionPool.borrow()): DB = DB(conn, DBConnectionAttributes())

  /**
   * Returns a DB instance by using an implicit Connection object.
   *
   * @param conn connection
   * @return  DB instance
   */
  def connected(implicit conn: Connection) = DB(conn, DBConnectionAttributes())

}
