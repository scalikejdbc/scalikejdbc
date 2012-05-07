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
import java.net.URL

/**
 * DB Session (readOnly/autoCommit/localTx/withinTx)
 */
case class DBSession(conn: Connection, tx: Option[Tx] = None) extends LogSupport {

  def connection: Connection = conn

  tx match {
    case Some(transaction) if transaction.isActive() =>
    case None => conn.setAutoCommit(true)
    case _ => throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
  }

  def createPreparedStatement(con: Connection, template: String): PreparedStatement = {
    log.debug("template : " + template)
    conn.prepareStatement(template, Statement.RETURN_GENERATED_KEYS)
  }

  private def bindParams(stmt: PreparedStatement, params: Any*): Unit = {

    val paramsWithIndices = params.map {
      case option: Option[_] => option.orNull[Any]
      case other => other
    }.zipWithIndex

    for ((param, idx) <- paramsWithIndices; i = idx + 1) {
      param match {
        case null => stmt.setObject(i, null)
        case p: Array => stmt.setArray(i, p)
        case p: BigDecimal => stmt.setBigDecimal(i, p.bigDecimal)
        case p: Boolean => stmt.setBoolean(i, p)
        case p: Byte => stmt.setByte(i, p)
        case p: Date => stmt.setDate(i, p)
        case p: Double => stmt.setDouble(i, p)
        case p: Float => stmt.setFloat(i, p)
        case p: Int => stmt.setInt(i, p)
        case p: Long => stmt.setLong(i, p)
        case p: Short => stmt.setShort(i, p)
        case p: SQLXML => stmt.setSQLXML(i, p)
        case p: String => stmt.setString(i, p)
        case p: Time => stmt.setTime(i, p)
        case p: Timestamp => stmt.setTimestamp(i, p)
        case p: URL => stmt.setURL(i, p)
        case p: java.util.Date => stmt.setTimestamp(i, p.toSqlTimestamp)
        case p: org.joda.time.DateTime => stmt.setTimestamp(i, p.toDate.toSqlTimestamp)
        case p: org.joda.time.LocalDateTime => stmt.setTimestamp(i, p.toDate.toSqlTimestamp)
        case p: org.joda.time.LocalDate => stmt.setDate(i, p.toDate.toSqlDate)
        case p: org.joda.time.LocalTime => stmt.setTime(i, p.toSqlTime)
        case p => throw new IllegalArgumentException(p.toString)
      }
    }

  }

  def single[A](template: String, params: Any*)(extract: WrappedResultSet => A): Option[A] = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        val resultSet = new ResultSetTraversable(stmt.executeQuery())
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
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        val resultSet = new ResultSetTraversable(stmt.executeQuery())
        (resultSet map (rs => extract(rs))).toList
    }
  }

  def foreach[A](template: String, params: Any*)(f: WrappedResultSet => Unit) = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        new ResultSetTraversable(stmt.executeQuery()) foreach (rs => f(rs))
    }
  }

  def traversable[A](template: String, params: Any*)(extract: WrappedResultSet => A): Traversable[A] = {
    val stmt = createPreparedStatement(conn, template)
    bindParams(stmt, params: _*)
    new ResultSetTraversable(stmt.executeQuery()) map (rs => extract(rs))
  }

  def executeUpdate(template: String, params: Any*): Int = update(template, params: _*)

  def execute[A](template: String, params: Any*): Boolean = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        stmt.execute()
    }
  }

  def executeWithFilters[A](before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Boolean = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        before(stmt)
        val result = stmt.execute()
        after(stmt)
        result
    }
  }

  def update(template: String, params: Any*): Int = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        stmt.executeUpdate()
    }
  }

  def updateWithFilters(before: (PreparedStatement) => Unit,
    after: (PreparedStatement) => Unit,
    template: String,
    params: Any*): Int = {
    val stmt = createPreparedStatement(conn, template)
    using(stmt) {
      stmt =>
        bindParams(stmt, params: _*)
        before(stmt)
        val count = stmt.executeUpdate()
        after(stmt)
        count
    }
  }

  def updateAndReturnGeneratedKey(template: String, params: Any*): Long = {
    var generatedKey: Long = -1
    val before = (stmt: PreparedStatement) => {}
    val after = (stmt: PreparedStatement) => {
      val rs = stmt.getGeneratedKeys
      while (rs.next()) {
        generatedKey = rs.getLong(1)
      }
    }
    updateWithFilters(before, after, template, params: _*)
    if (generatedKey == -1) {
      throw new IllegalStateException("Failed to retrieve the generated key (template:" + template + ")")
    }
    generatedKey
  }

  def close(): Unit = conn.close()

}
