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

/**
 * DB accessor
 */
class DB(conn: Connection) {

  def isTxNotActive = conn == null || conn.isClosed || conn.isReadOnly

  def isTxNotYetStarted = conn != null && conn.getAutoCommit

  def isTxAlreadyStarted = conn != null && !conn.getAutoCommit

  def newTx(conn: Connection): Tx = {
    if (isTxNotActive || isTxAlreadyStarted) {
      throw new IllegalStateException(ErrorMessage.CANNOT_START_A_NEW_TRANSACTION)
    }
    new Tx(conn)
  }

  def newTx: Tx = newTx(conn)

  def currentTx = {
    if (isTxNotActive || isTxNotYetStarted) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    new Tx(conn)
  }

  def tx = {
    try {
      currentTx
    } catch {
      case (e: IllegalStateException) =>
        throw new IllegalStateException(
          "DB#tx is an alias of DB#currentTx. " +
            "You cannot call this API before beginning a transaction"
        )
    }
  }

  def begin() = newTx.begin()

  def beginIfNotYet() = {
    try {
      begin()
    } catch {
      case (e: IllegalStateException) =>
    }
  }

  def commit() = tx.commit()

  def rollback() = tx.rollback()

  def rollbackIfActive() = {
    try {
      tx.rollbackIfActive()
    } catch {
      case (e: IllegalStateException) =>
    }
  }

  def readOnlySession(): DBSession = {
    conn.setReadOnly(true)
    new DBSession(conn)
  }

  def readOnly[A](execution: DBSession => A): A = {
    val session = readOnlySession()
    execution(session)
  }

  def autoCommitSession(): DBSession = new DBSession(conn)

  def autoCommit[A](execution: DBSession => A): A = {
    val session = autoCommitSession()
    execution(session)
  }

  def withinTxSession(tx: Tx = currentTx): DBSession = {
    if (!tx.isActive) throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    new DBSession(conn, Some(tx))
  }

  def withinTx[A](execution: DBSession => A): A = {
    val tx = currentTx
    val session = withinTxSession(tx)
    execution(session)
  }

  def localTx[A](execution: DBSession => A): A = {
    val tx = newTx
    tx.begin()
    if (!tx.isActive) {
      throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
    }
    val session = new DBSession(conn, Some(tx))
    try {
      execution(session)
    } catch {
      case e: Exception => {
        tx.rollback()
        throw e
      }
    } finally {
      tx.commit()
    }
  }

}
