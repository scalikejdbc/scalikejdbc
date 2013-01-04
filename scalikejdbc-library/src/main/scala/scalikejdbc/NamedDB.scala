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

import java.sql.Connection
import scalikejdbc.metadata.Table

/**
 * Named Basic DB Accessor
 *
 * It's easier to use named ConnectionPool with this class.
 *
 * {{{
 * ConnectionPool.add('named, "jdbc:...", "user", "password")
 * val users = NamedDB('named) readOnly { session =>
 *   session.list("select * from user")
 * }
 * }}}
 */
case class NamedDB(name: Any)(implicit context: ConnectionPoolContext = NoConnectionPoolContext) {

  private[this] def connectionPool(): ConnectionPool = opt(context match {
    case NoConnectionPoolContext => ConnectionPool(name)
    case _: MultipleConnectionPoolContext => context.get(name)
    case _ => throw new IllegalStateException(ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT)
  }) getOrElse {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  private lazy val db: DB = DB(connectionPool().borrow())

  def toDB(): DB = db

  def isTxNotActive = db.isTxNotActive

  def isTxNotYetStarted = db.isTxNotYetStarted

  def isTxAlreadyStarted = db.isTxAlreadyStarted

  def newTx: Tx = db.newTx

  def currentTx: Tx = db.currentTx

  def tx: Tx = db.tx

  def close() = db.close()

  def begin() = db.begin()

  def beginIfNotYet(): Unit = db.beginIfNotYet()

  def commit(): Unit = db.commit()

  def rollback(): Unit = db.rollback()

  def rollbackIfActive(): Unit = db.rollbackIfActive()

  def readOnlySession(): DBSession = db.readOnlySession()

  def readOnly[A](execution: DBSession => A): A = db.readOnly(execution)

  def readOnlyWithConnection[A](execution: Connection => A): A = db.readOnlyWithConnection(execution)

  def autoCommitSession(): DBSession = db.autoCommitSession()

  def autoCommit[A](execution: DBSession => A): A = db.autoCommit(execution)

  def autoCommitWithConnection[A](execution: Connection => A): A = db.autoCommitWithConnection(execution)

  def withinTxSession(tx: Tx = currentTx): DBSession = db.withinTxSession(tx)

  def withinTx[A](execution: DBSession => A): A = db.withinTx(execution)

  def withinTxWithConnection[A](execution: Connection => A): A = db.withinTxWithConnection(execution)

  def localTx[A](execution: DBSession => A): A = db.localTx(execution)

  def localTxWithConnection[A](execution: Connection => A): A = db.localTxWithConnection(execution)

  def getTable(table: String): Option[Table] = db.getTable(table)

  def getTableNames(tableNamePattern: String, tableTypes: Array[String] = Array("TABLE", "VIEW")): List[String] = {
    db.getTableNames(tableNamePattern)
  }

  def showTables(tableNamePattern: String = "%", tableTypes: Array[String] = Array("TABLE", "VIEW")): String = {
    db.showTables(tableNamePattern, tableTypes)
  }

  def describe(table: String) = db.describe(table)

}
