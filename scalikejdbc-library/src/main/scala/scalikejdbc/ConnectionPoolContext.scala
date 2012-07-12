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

/**
 * Connection Pool Context
 */
trait ConnectionPoolContext {

  lazy val connectionPool: ConnectionPool = ConnectionPool()

}

/**
 * No Connection Pool Context
 */
object NoConnectionPoolContext extends ConnectionPoolContext {

  override lazy val connectionPool: ConnectionPool = throw new IllegalStateException(ErrorMessage.NO_CONNECTION_POOL_CONTEXT)

}

/**
 * Default Connection Pool Context
 */
object DefaultConnectionPoolContext extends ConnectionPoolContext

/**
 * Named Connection Pool Context
 */
case class NamedConnectionPoolContext(name: Any) extends ConnectionPoolContext {

  override lazy val connectionPool: ConnectionPool = ConnectionPool(name)

}

