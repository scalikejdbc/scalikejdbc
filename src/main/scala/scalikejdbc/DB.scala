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
import java.lang.IllegalStateException
import scala.util.control.Exception._

object DB {

  def apply(conn: => Connection): DB = new DB(connect = () => conn)

  private def ensureDBInstance(db: DB): Unit = {
    if (db == null) {
      throw new IllegalStateException(ErrorMessage.IMPLICIT_DB_INSTANCE_REQUIRED)
    }
  }

  /**
   * Begins a readOnly block easily with ConnectionPool
   */
  def readOnly[A](execution: DBSession => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).readOnly(execution)
    }
  }

  def readOnlyWithConnection[A](execution: Connection => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).readOnlyWithConnection(execution)
    }
  }

  def readOnlySession(): DBSession = DB(ConnectionPool.borrow()).readOnlySession()

  /**
   * Begins a autoCommit block easily with ConnectionPool
   */
  def autoCommit[A](execution: DBSession => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).autoCommit(execution)
    }
  }

  def autoCommitWithConnection[A](execution: Connection => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).autoCommitWithConnection(execution)
    }
  }

  def autoCommitSession(): DBSession = DB(ConnectionPool.borrow()).autoCommitSession()

  /**
   * Begins a localTx block easily with ConnectionPool
   */
  def localTx[A](execution: DBSession => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).localTx(execution)
    }
  }

  def localTxWithConnection[A](execution: Connection => A): A = {
    using(ConnectionPool.borrow()) {
      conn => DB(conn).localTxWithConnection(execution)
    }
  }

  /**
   * Begins a withinTx block easily with a DB instance as an implicit parameter
   */
  def withinTx[A](execution: DBSession => A)(implicit db: DB): A = {
    ensureDBInstance(db: DB)
    db.withinTx(execution)
  }

  def withinTxWithConnection[A](execution: Connection => A)(implicit db: DB): A = {
    ensureDBInstance(db: DB)
    db.withinTxWithConnection(execution)
  }

  def withinTxSession(): DBSession = DB(ConnectionPool.borrow()).withinTxSession()

  /**
   * Get a connection and returns a DB instance
   */
  def connect(conn: Connection = ConnectionPool.borrow()): DB = DB(conn)

  /**
   * Returns a DB instance by using an implicit Connection object
   */
  def connected(implicit conn: Connection) = DB(conn)

}

/**
 * DB accessor
 */
class DB(connect: () => Connection) {

  lazy val conn: Connection = connect()

  def isTxNotActive = conn == null || conn.isClosed || conn.isReadOnly

  def isTxNotYetStarted = conn != null && conn.getAutoCommit

  def isTxAlreadyStarted = conn != null && !conn.getAutoCommit

  private def newTx(conn: Connection): Tx = {
    if (isTxNotActive || isTxAlreadyStarted) {
      throw new IllegalStateException(ErrorMessage.CANNOT_START_A_NEW_TRANSACTION)
    }
    new Tx(conn)
  }

  def newTx: Tx = newTx(conn)

  def currentTx: Tx = {
    if (isTxNotActive || isTxNotYetStarted) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    new Tx(conn)
  }

  def tx: Tx = {
    handling(classOf[IllegalStateException]) by {
      e =>
        throw new IllegalStateException(
          "DB#tx is an alias of DB#currentTx. You cannot call this API before beginning a transaction")
    } apply currentTx
  }

  def close() = conn.close()

  def begin() = newTx.begin()

  def beginIfNotYet(): Unit = {
    ignoring(classOf[IllegalStateException]) apply {
      begin()
    }
  }

  def commit(): Unit = tx.commit()

  def rollback(): Unit = tx.rollback()

  def rollbackIfActive(): Unit = {
    ignoring(classOf[IllegalStateException]) apply {
      tx.rollbackIfActive()
    }
  }

  def readOnlySession(): DBSession = {
    conn.setReadOnly(true)
    new DBSession(connect = () => conn, isReadOnly = true)
  }

  def readOnly[A](execution: DBSession => A): A = {
    using(conn) { conn =>
      execution(readOnlySession())
    }
  }

  def readOnlyWithConnection[A](execution: Connection => A): A = {
    // cannot control if jdbc drivers ignore the readOnly attribute.
    using(conn) { conn =>
      execution(readOnlySession().conn)
    }
  }

  def autoCommitSession(): DBSession = {
    conn.setReadOnly(false)
    conn.setAutoCommit(true)
    new DBSession(connect = () => conn)
  }

  def autoCommit[A](execution: DBSession => A): A = {
    using(conn) { conn =>
      execution(autoCommitSession())
    }
  }

  def autoCommitWithConnection[A](execution: Connection => A): A = {
    using(conn) { conn =>
      execution(autoCommitSession().conn)
    }
  }

  def withinTxSession(tx: Tx = currentTx): DBSession = {
    if (!tx.isActive) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    new DBSession(connect = () => conn, tx = Some(tx))
  }

  def withinTx[A](execution: DBSession => A): A = {
    execution(withinTxSession(currentTx))
  }

  def withinTxWithConnection[A](execution: Connection => A): A = {
    execution(withinTxSession(currentTx).conn)
  }

  private def begin(tx: Tx) {
    tx.begin()
    if (!tx.isActive) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
  }

  private val rollbackIfThrowable = handling(classOf[Throwable]) by {
    t =>
      tx.rollback()
      throw t
  }

  def localTx[A](execution: DBSession => A): A = {
    using(conn) { conn =>
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = new DBSession(connect = () => conn, tx = Option(tx))
        val result: A = execution(session)
        tx.commit()
        result
      }
    }
  }

  def localTxWithConnection[A](execution: Connection => A): A = {
    using(conn) { conn =>
      val tx = newTx
      begin(tx)
      rollbackIfThrowable[A] {
        val session = new DBSession(connect = () => conn, tx = Option(tx))
        val result: A = execution(session.conn)
        tx.commit()
        result
      }
    }
  }

}
