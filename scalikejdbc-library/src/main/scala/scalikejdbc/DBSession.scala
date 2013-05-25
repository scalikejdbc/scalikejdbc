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
 * DB Session
 *
 * This class provides readOnly/autoCommit/localTx/withinTx blocks and session objects.
 *
 * {{{
 * import scalikejdbc._
 *
 * val userIdList = DB autoCommit { session: DBSession =>
 *   session.list("select * from user") { rs => rs.int("id") }
 * }
 * }}}
 */
trait DBSession extends LogSupport {

  /**
   * Connection
   */
  val conn: Connection
  lazy val connection: Connection = conn

  val isReadOnly: Boolean

  /**
   * Create [[java.sql.Statement]] executor.
   *
   * @param conn connection
   * @param template SQL template
   * @param params parameters
   * @param returnGeneratedKeys is generated keys required
   * @return statement executor
   */
  private def createStatementExecutor(conn: Connection, template: String, params: Seq[Any],
    returnGeneratedKeys: Boolean = false): StatementExecutor = {
    try {
      val statement = if (returnGeneratedKeys) {
        conn.prepareStatement(template, Statement.RETURN_GENERATED_KEYS)
      } else {
        conn.prepareStatement(template)
      }
      StatementExecutor(
        underlying = statement,
        template = template,
        singleParams = params)
    } catch {
      case e: Exception =>
        val formattedTemplate = if (GlobalSettings.sqlFormatter.formatter.isDefined) {
          try {
            val formatter = GlobalSettings.sqlFormatter.formatter.get
            formatter.format(template)
          } catch {
            case e: Exception =>
              log.debug("Failed to format SQL because " + e.getMessage, e)
              template
          }
        } else {
          template
        }
        log.error("Failed preparing the statement (Reason: " + e.getMessage + "):\n\n  " + formattedTemplate + "\n")
        throw e
    }
  }

  /**
   * Create [[java.sql.Statement]] executor.
   * @param conn connection
   * @param template SQL template
   * @return statement executor
   */
  private def createBatchStatementExecutor(conn: Connection, template: String): StatementExecutor = {
    StatementExecutor(
      underlying = conn.prepareStatement(template),
      template = template,
      isBatch = true)
  }

  /**
   * Ensures the current session is not in read-only mode.
   * @param template
   */
  private def ensureNotReadOnlySession(template: String): Unit = {
    if (isReadOnly) {
      throw new java.sql.SQLException(
        ErrorMessage.CANNOT_EXECUTE_IN_READ_ONLY_SESSION + " (template:" + template + ")")
    }
  }

  /**
   * Returns single result optionally.
   * If the result is not single, [[scalikejdbc.TooManyRowsException]] will be thrown.
   *
   * @param template SQL template
   * @param params parameters
   * @param extract extract function
   * @tparam A return type
   * @return result optionally
   */
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

  /**
   * Returns the first row optionally.
   *
   * @param template SQL template
   * @param params parameters
   * @param extract extract function
   * @tparam A return type
   * @return result optionally
   */
  def first[A](template: String, params: Any*)(extract: WrappedResultSet => A): Option[A] = {
    list(template, params: _*)(extract).headOption
  }

  /**
   * Returns query result as [[scala.collection.immutable.List]] object.
   *
   * @param template SQL template
   * @param params parameters
   * @param extract extract function
   * @tparam A return type
   * @return result as list
   */
  def list[A](template: String, params: Any*)(extract: WrappedResultSet => A): List[A] = {
    traversable(template, params: _*)(extract).toList
  }

  /**
   * Applies side-effect to each row iteratively.
   *
   * @param template SQL template
   * @param params parameters
   * @param f function
   * @return result as list
   */
  def foreach(template: String, params: Any*)(f: WrappedResultSet => Unit): Unit = {
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        new ResultSetTraversable(executor.executeQuery()) foreach (rs => f(rs))
    }
  }

  /**
   * folding into one value.
   *
   * @param template SQL template
   * @param params parameters
   * @param z initial value
   * @param op function
   * @return folded value
   */
  def foldLeft[A](template: String, params: Any*)(z: A)(op: (A, WrappedResultSet) => A): A = {
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        new ResultSetTraversable(executor.executeQuery()).foldLeft(z)(op)
    }
  }

  /**
   * Returns query result as [[scala.collection.Traversable]] object.
   *
   * @param template SQL template
   * @param params parameters
   * @param extract extract function
   * @tparam A return type
   * @return result as traversable
   */
  def traversable[A](template: String, params: Any*)(extract: WrappedResultSet => A): Traversable[A] = {
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        new ResultSetTraversable(executor.executeQuery()) map (rs => extract(rs))
    }
  }

  /**
   * Executes [[java.sql.PreparedStatement#execute()]].
   *
   * @param template SQL template
   * @param params  parameters
   * @return flag
   */
  def execute(template: String, params: Any*): Boolean = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        executor.execute()
    }
  }

  /**
   * Executes [[java.sql.PreparedStatement#execute()]].
   *
   * @param before before filter
   * @param after after filter
   * @param template SQL template
   * @param params parameters
   * @return flag
   */
  def executeWithFilters(before: (PreparedStatement) => Unit,
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

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]].
   *
   * @param template SQL template
   * @param params  parameters
   * @return result count
   */
  def executeUpdate(template: String, params: Any*): Int = update(template, params: _*)

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]].
   *
   * @param template SQL template
   * @param params parameters
   * @return result count
   */
  def update(template: String, params: Any*): Int = {
    ensureNotReadOnlySession(template)
    using(createStatementExecutor(conn, template, params)) {
      executor =>
        executor.executeUpdate()
    }
  }

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]].
   *
   * @param before before filter
   * @param after after filter
   * @param template SQL template
   * @param params parameters
   * @return  result count
   */
  def updateWithFilters(before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Int = updateWithFilters(false, before, after, template, params: _*)

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]].
   *
   * @param returnGeneratedKeys is generated keys required
   * @param before before filter
   * @param after after filter
   * @param template SQL template
   * @param params parameters
   * @return  result count
   */
  def updateWithFilters(returnGeneratedKeys: Boolean,
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

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]] and returns the generated key.
   *
   * @param template SQL template
   * @param params parameters
   * @return generated key as a long value
   */
  def updateAndReturnGeneratedKey(template: String, params: Any*): Long = updateAndReturnSpecifiedGeneratedKey(template, params: _*)(1)

  /**
   * Executes [[java.sql.PreparedStatement#executeUpdate()]] and returns the generated key.
   *
   * @param template SQL template
   * @param params parameters
   * @param key name
   * @return generated key as a long value
   */
  def updateAndReturnSpecifiedGeneratedKey(template: String, params: Any*)(key: Any): Long = {
    var generatedKeyFound = false
    var generatedKey: Long = -1
    val before = (stmt: PreparedStatement) => {}
    val after = (stmt: PreparedStatement) => {
      val rs = stmt.getGeneratedKeys
      while (rs.next()) {
        generatedKeyFound = true
        generatedKey = key match {
          case name: String => rs.getLong(name)
          case index: Int => try {
            rs.getLong(index)
          } catch {
            case e: Exception =>
              log.warn("Failed to get generated key value via index " + index + ". Going to retrieve it via index 1.")
              rs.getLong(1)
          }
          case _ => throw new IllegalArgumentException(ErrorMessage.FAILED_TO_RETRIEVE_GENERATED_KEY + "(key:" + key + ")")
        }
      }
    }
    updateWithFilters(true, before, after, template, params: _*)
    if (!generatedKeyFound) {
      throw new IllegalStateException(ErrorMessage.FAILED_TO_RETRIEVE_GENERATED_KEY + " (template:" + template + ")")
    }
    generatedKey
  }

  /**
   * Executes [[java.sql.PreparedStatement#executeBatch()]]
   * @param template SQL template
   * @param paramsList list of parameters
   * @return count list
   */
  def batch(template: String, paramsList: Seq[Any]*): Seq[Int] = {
    ensureNotReadOnlySession(template)
    using(createBatchStatementExecutor(conn = conn, template = template)) {
      executor =>
        paramsList.foreach {
          params =>
            executor.bindParams(params)
            executor.addBatch()
        }
        executor.executeBatch().toSeq
    }
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

}

object DBSession {

  def apply(conn: Connection, tx: Option[Tx] = None, isReadOnly: Boolean = false) = ActiveSession(conn, tx, isReadOnly)

}

/**
 * Active session implementation of [[scalikejdbc.DBSession]].
 *
 * This class provides readOnly/autoCommit/localTx/withinTx blocks and session objects.
 *
 * {{{
 * import scalikejdbc._
 *
 * val userIdList = DB autoCommit { session: DBSession =>
 *   session.list("select * from user") { rs => rs.int("id") }
 * }
 * }}}
 *
 * @param conn connection
 * @param tx transaction
 * @param isReadOnly is read only
 */
case class ActiveSession(conn: Connection, tx: Option[Tx] = None, isReadOnly: Boolean = false)
    extends DBSession {

  tx match {
    case Some(tx) if tx.isActive() => // nothing to do
    case None => conn.setAutoCommit(true)
    case _ => throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
  }

}

/**
 * Represents that there is no active session.
 */
case object NoSession extends DBSession {

  val conn: Connection = null
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = false

}

/**
 * Represents that already existing session will be used or a new session will be started.
 */
case object AutoSession extends DBSession {

  val conn: Connection = null
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = false

}

/**
 * Represents that already existing session will be used or a new session which is retrieved from named connection pool will be started.
 */
case class NamedAutoSession(name: Any) extends DBSession {

  val conn: Connection = null
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = false

}

