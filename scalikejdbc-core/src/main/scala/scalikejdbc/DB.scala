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
case class DB(conn: Connection) extends DBConnection

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
   * Begins a read-only block easily with ConnectionPool.
   *
   * @param execution execution
   * @param context connection pool context
   * @tparam A return type
   * @return result value
   */
  def readOnly[A](execution: DBSession => A)(implicit context: CPContext = NoCPContext): A = {
    using(connectionPool(context).borrow()) { conn =>
      DB(conn).autoClose(false).readOnly(execution)
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
      DB(conn).autoClose(false).readOnlyWithConnection(execution)
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
      DB(conn).autoClose(false).autoCommit(execution)
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
      DB(conn).autoClose(false).autoCommitWithConnection(execution)
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
      DB(conn).autoClose(false).localTx(execution)
    }
  }

  /**
   * Begins a generalized local-tx block that returns a value easily with ConnectionPool.
   *
   * @param execution execution that returns a value
   * @param context connection pool context
   * @tparam A result type
   * @return result value
   */
  def generalizedLocalTx[A: TxBoundary](execution: DBSession => A)(
    implicit context: CPContext = NoCPContext): A = {
    DB(connectionPool(context).borrow()).autoClose(true).generalizedLocalTx(execution)
  }

  /**
   * Begins a local-tx block that returns a Future value easily with ConnectionPool.
   *
   * @param execution execution that returns a future value
   * @param context connection pool context
   * @tparam A future result type
   * @return future result value
   */
  def futureLocalTx[A](execution: DBSession => Future[A])(
    implicit context: CPContext = NoCPContext, ec: ExecutionContext): Future[A] = {
    generalizedLocalTx(execution)
  }

  /**
   * Begins a local-tx block that returns a Try value easily with ConnectionPool.
   *
   * @param execution execution that returns a Try value
   * @tparam A Try result type
   * @return Try result value
   */
  def tryLocalTx[A](execution: DBSession => Try[A])(
    implicit context: CPContext = NoCPContext): Try[A] = {
    generalizedLocalTx(execution)
  }

  /**
   * Begins a local-tx block that returns a Either value easily with ConnectionPool.
   *
   * @param execution execution that returns a Either value
   * @tparam A Either result type
   * @return Either result value
   */
  def eitherLocalTx[A, B](execution: DBSession => Either[B, A])(
    implicit context: CPContext = NoCPContext): Either[B, A] = {
    generalizedLocalTx(execution)
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
      DB(conn).autoClose(false).localTxWithConnection(execution)
    }
  }

  /**
   * Begins a generalized local-tx block that returns a value easily with ConnectionPool.
   *
   * @param execution execution that returns a value
   * @param context connection pool context
   * @tparam A result type
   * @return result value
   */
  def generalizedLocalTxWithConnection[A: TxBoundary](execution: Connection => A)(
    implicit context: CPContext = NoCPContext): A = {
    DB(connectionPool(context).borrow()).autoClose(true).generalizedLocalTxWithConnection(execution)
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
  def showTables(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW"))(
    implicit context: CPContext = NoCPContext): String = {
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
