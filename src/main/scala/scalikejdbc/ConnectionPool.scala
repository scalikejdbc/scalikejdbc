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

import org.apache.commons.pool.impl.GenericObjectPool
import org.apache.commons.dbcp.{ PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory }
import javax.sql.DataSource
import java.sql.Connection

object ConnectionPool extends LogSupport {

  private val DEFAULT_NAME: Any = "default"

  private val pools = new collection.mutable.HashMap[Any, ConnectionPool]()

  def isInitialized(name: Any = DEFAULT_NAME) = pools.get(DEFAULT_NAME).isDefined

  def ensureInitialized(name: Any): Unit = {
    if (!isInitialized(name)) {
      val message = ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED + "(name:" + name + ")"
      throw new IllegalStateException(message)
    }
  }

  def apply(name: Any = DEFAULT_NAME) = get(name)

  def get(name: Any = DEFAULT_NAME): ConnectionPool = pools.get(name).orNull

  def add(name: Any, url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()) {
    pools.update(name, new ConnectionPool(url, user, password, settings))
    log.debug("Registered connection pool : " + get(name).toString())
  }

  def singleton(url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()): Unit = {
    add(DEFAULT_NAME, url, user, password, settings)
    log.debug("Registered singleton connection pool : " + get().toString())
  }

  def dataSource(name: Any = DEFAULT_NAME): DataSource = {
    ensureInitialized(name)
    get(name).dataSource
  }

  def borrow(name: Any = DEFAULT_NAME): Connection = {
    ensureInitialized(name)
    val pool = get(name)
    log.debug("Borrowed a new connection from " + pool.toString())
    pool.borrow()
  }

}

class ConnectionPool(url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()) {

  private val pool = new GenericObjectPool(null)
  pool.setMinIdle(settings.initialSize)
  pool.setMaxIdle(settings.maxSize)
  pool.setMaxActive(settings.maxSize)
  pool.setMaxWait(5000)
  pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL)
  pool.setTestOnBorrow(true)

  private val connFactory = new DriverManagerConnectionFactory(url, user, password)

  // not read-only, auto-commit
  val poolableConnectionFactory = new PoolableConnectionFactory(
    connFactory, pool, null, settings.validationQuery, false, true)

  val dataSource: DataSource = new PoolingDataSource(pool)

  def borrow(): Connection = dataSource.getConnection()

  override def toString() = "ConnectionPool(url:" + url + ", user:" + user + ")"

}
