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

import java.sql._
import util.control.Exception._

/**
 * DB Session (readOnly/autoCommit/localTx/withinTx)
 */
case class DBSession(conn: Connection, tx: Option[Tx] = None, isReadOnly: Boolean = false) extends LogSupport {

  lazy val connection: Connection = conn

  tx match {
    case Some(tx) if tx.isActive() => // nothing to do
    case None => conn.setAutoCommit(true)
    case _ => throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
  }

  private def createStatementExecutor(conn: Connection, template: String, params: Seq[Any],
    returnGeneratedKeys: Boolean = false): StatementExecutor = {
    val statement = if (returnGeneratedKeys) {
      conn.prepareStatement(template, Statement.RETURN_GENERATED_KEYS)
    } else {
      conn.prepareStatement(template, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)
    }
    StatementExecutor(
      underlying = statement,
      template = template,
      params = params)
  }

  private def ensureNotReadOnlySession(template: String): Unit = {
    if (isReadOnly) {
      throw new java.sql.SQLException(
        ErrorMessage.CANNOT_EXECUTE_IN_READ_ONLY_SESSION + " (template:" + template + ")")
    }
  }

  def single[A](template: String, params: Any*)(extract: WrappedResultSet => A): Option[A] = {
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        val resultSet = new ResultSetTraversable(executor.executeQuery())
        val rows = (resultSet map (rs => extract(rs))).toList
        rows match {
          case Nil => None
          case one :: Nil => Option(one)
          case _ => throw new TooManyRowsException(1, rows.size)
        }
    }
  }

  def first[A](template: String, params: Any*)(extract: WrappedResultSet => A): Option[A] = {
    list(template, params: _*)(extract).headOption
  }

  def list[A](template: String, params: Any*)(extract: WrappedResultSet => A): List[A] = {
    traversable(template, params: _*)(extract).toList
  }

  def foreach[A](template: String, params: Any*)(f: WrappedResultSet => Unit): Unit = {
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        new ResultSetTraversable(executor.executeQuery()) foreach (rs => f(rs))
    }
  }

  def traversable[A](template: String, params: Any*)(extract: WrappedResultSet => A): Traversable[A] = {
    using(createStatementExecutor(conn, template, params)) { executor =>
      new ResultSetTraversable(executor.executeQuery()) map (rs => extract(rs))
    }
  }

  def executeUpdate(template: String, params: Any*): Int = update(template, params: _*)

  def execute[A](template: String, params: Any*): Boolean = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        executor.execute()
    }
  }

  def executeWithFilters[A](before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Boolean = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        before(executor.underlying)
        val result = executor.execute()
        after(executor.underlying)
        result
    }
  }

  def update(template: String, params: Any*): Int = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        executor.executeUpdate()
    }
  }

  def updateWithFilters(before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Int = _updateWithFilters(false, before, after, template, params: _*)

  private def _updateWithFilters(returnGeneratedKeys: Boolean,
    before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Int = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(
      conn = conn,
      template = template,
      params = params,
      returnGeneratedKeys = returnGeneratedKeys)) {
      executor =>
        before(executor.underlying)
        val count = executor.executeUpdate()
        after(executor.underlying)
        count
    }
  }

  def updateAndReturnGeneratedKey(template: String, params: Any*): Long = {
    var generatedKeyFound = false
    var generatedKey: Long = -1
    val before = (stmt: PreparedStatement) => {}
    val after = (stmt: PreparedStatement) => {
      val rs = stmt.getGeneratedKeys
      while (rs.next()) {
        generatedKeyFound = true
        generatedKey = rs.getLong(1)
      }
    }
    _updateWithFilters(true, before, after, template, params: _*)
    if (!generatedKeyFound) {
      throw new IllegalStateException(ErrorMessage.FAILED_TO_RETRIEVE_GENERATED_KEY + " (template:" + template + ")")
    }
    generatedKey
  }

  def close(): Unit = {
    ignoring(classOf[Throwable]) {
      conn.close()
    }
    log.debug("A Connection is closed.")
  }

}
