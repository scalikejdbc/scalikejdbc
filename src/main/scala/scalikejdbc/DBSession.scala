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
import scalikejdbc.LoanPattern.using

/**
 * DB Session (readOnly/autoCommit/localTx/withinTx)
 */
class DBSession(conn: Connection, tx: Option[Tx] = None) {

  tx match {
    case Some(transaction) if transaction.isActive() =>
    case None => conn.setAutoCommit(true)
    case _ => throw new IllegalStateException(ErrorMessage.TRANSACTION_IS_NOT_ACTIVE)
  }

  class ResultSetIterator(rs: ResultSet) extends Iterator[ResultSet] {

    def hasNext: Boolean = rs.next

    def next(): ResultSet = rs

  }

  private def bindParams(stmt: PreparedStatement, params: Any*): Unit = {
    for ((param, idx) <- params.zipWithIndex; i = idx + 1) {
      param match {
        case p: Array => stmt.setArray(i, p)
        case p: java.math.BigDecimal => stmt.setBigDecimal(i, p)
        case p: Boolean => stmt.setBoolean(i, p)
        case p: Byte => stmt.setByte(i, p)
        case p: Date => stmt.setDate(i, p)
        case p: Double => stmt.setDouble(i, p)
        case p: Float => stmt.setFloat(i, p)
        case p: Int => stmt.setInt(i, p)
        case p: Long => stmt.setLong(i, p)
        case p: SQLXML => stmt.setSQLXML(i, p)
        case p: String => stmt.setString(i, p)
        case p: Time => stmt.setTime(i, p)
        case p: Timestamp => stmt.setTimestamp(i, p)
        case p: URL => stmt.setURL(i, p)
        case p => throw new IllegalArgumentException(p.toString)
      }
    }
  }

  def execute[A](template: String, params: Any*): Boolean = {
    val stmt = conn.prepareStatement(template)
    using(stmt) {
      stmt => {
        bindParams(stmt, params: _*)
        stmt.execute()
      }
    }
  }

  def asOne[A](template: String, params: Any*)(extract: ResultSet => Option[A]): Option[A] = {
    val stmt = conn.prepareStatement(template)
    using(stmt) {
      stmt => {
        bindParams(stmt, params: _*)
        val resultSet = new ResultSetIterator(stmt.executeQuery())
        val rows = (resultSet flatMap (rs => extract(rs))).toList
        rows match {
          case Nil => None
          case one :: Nil => Some(one)
          case _ => throw new TooManyRowsException(1, rows.size)
        }
      }
    }
  }

  def asList[A](template: String, params: Any*)(extract: ResultSet => Option[A]): List[A] = {
    val stmt = conn.prepareStatement(template)
    using(stmt) {
      stmt => {
        bindParams(stmt, params: _*)
        val resultSet = new ResultSetIterator(stmt.executeQuery())
        (resultSet flatMap (rs => extract(rs))).toList
      }
    }
  }

  def update(template: String, params: Any*): Int = {
    val stmt = conn.prepareStatement(template)
    using(stmt) {
      stmt => {
        bindParams(stmt, params: _*)
        stmt.executeUpdate()
      }
    }
  }

}
