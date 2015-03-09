/*
 * Copyright 2011 - 2015 scalikejdbc.org
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

import scala.collection.mutable

/**
 * Connection pool context
 */
trait ConnectionPoolContext {

  def set(name: Any, pool: ConnectionPool): Unit

  def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool

}

/**
 * Multiple connection pool context
 */
case class MultipleConnectionPoolContext(contexts: (Any, ConnectionPool)*) extends ConnectionPoolContext {

  def this() {
    this(Nil: _*)
  }

  private lazy val pools = new mutable.HashMap[Any, ConnectionPool]

  contexts foreach {
    case (name, pool) =>
      pools.put(name, pool)
  }

  override def set(name: Any, pool: ConnectionPool): Unit = pools.update(name, pool)

  override def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool = pools.get(name).getOrElse {
    throw new IllegalStateException("No connection context for " + name + ".")
  }

}

/**
 * No Connection Pool Context
 */
object NoConnectionPoolContext extends ConnectionPoolContext {

  override def set(name: Any, pool: ConnectionPool): Unit = throw new IllegalStateException(ErrorMessage.NO_CONNECTION_POOL_CONTEXT)

  override def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool = throw new IllegalStateException(ErrorMessage.NO_CONNECTION_POOL_CONTEXT)

}
