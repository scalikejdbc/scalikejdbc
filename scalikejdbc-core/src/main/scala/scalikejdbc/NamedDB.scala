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
case class NamedDB(name: Any)(implicit context: ConnectionPoolContext = NoConnectionPoolContext) extends DBConnection {

  private[this] def connectionPool(): ConnectionPool = Option(context match {
    case NoConnectionPoolContext => ConnectionPool(name)
    case _: MultipleConnectionPoolContext => context.get(name)
    case _ => throw new IllegalStateException(ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT)
  }) getOrElse {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  private lazy val db: DB = DB(connectionPool().borrow())

  def toDB(): DB = db

  def conn: Connection = db.conn

}
