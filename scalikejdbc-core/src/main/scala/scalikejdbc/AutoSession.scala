/*
 * Copyright 2011 - 2014 scalikejdbc.org
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
 * Represents that already existing session will be used or a new session will be started.
 */
case object AutoSession extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = false

  override def fetchSize(fetchSize: Int): DBSession = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): DBSession = unexpectedInvocation
  override def tags(tags: String*): DBSession = unexpectedInvocation
}

/**
 * Represents that already existing session will be used or a new session
 * which is retrieved from named connection pool will be started.
 */
case class NamedAutoSession(name: Any) extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = false

  override def fetchSize(fetchSize: Int): DBSession = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): DBSession = unexpectedInvocation
  override def tags(tags: String*): DBSession = unexpectedInvocation
}
