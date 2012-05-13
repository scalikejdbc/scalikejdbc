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

/**
 * Connection Pool
 *
 * Using Commons DBCP internally.
 *
 * @see http://commons.apache.org/dbcp/
 */
object ConnectionPool extends LogSupport {

  private val DEFAULT_NAME: Any = "default"

  private val pools = new collection.mutable.HashMap[Any, ConnectionPool]()

  /**
   * Returns true when the specified Connection pool is already initialized.
   * @param name pool name
   * @return is initialized
   */
  def isInitialized(name: Any = DEFAULT_NAME) = pools.get(DEFAULT_NAME).isDefined

  private def ensureInitialized(name: Any): Unit = {
    if (!isInitialized(name)) {
      val message = ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED + "(name:" + name + ")"
      throw new IllegalStateException(message)
    }
  }

  /**
   * Returns Connection pool. If the specified Connection pool does not exist, returns null.
   *
   * @param name pool name
   * @return              connection pool
   */
  def apply(name: Any = DEFAULT_NAME): ConnectionPool = get(name)

  /**
   * Returns Connection pool. If the specified Connection pool does not exist, returns null.
   *
   * @param name pool name
   * @return              connection pool
   */
  def get(name: Any = DEFAULT_NAME): ConnectionPool = pools.get(name).orNull

  /**
   * Register new named Connection pool.
   *
   * @param name  pool name
   * @param url   JDBC URL
   * @param user          JDBC username
   * @param password  JDBC password
   * @param settings  Settings
   */
  def add(name: Any, url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()) {
    pools.update(name, new ConnectionPool(url, user, password, settings))
    log.debug("Registered connection pool : " + get(name).toString())
  }

  /**
   * Register the default Connection pool.
   *
   * @param url   JDBC URL
   * @param user          JDBC username
   * @param password  JDBC password
   * @param settings  Settings
   */
  def singleton(url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()): Unit = {
    add(DEFAULT_NAME, url, user, password, settings)
    log.debug("Registered singleton connection pool : " + get().toString())
  }

  /**
   * Returns [[javax.sql.DataSource]].
   * @param name pool name
   * @return datasource
   */
  def dataSource(name: Any = DEFAULT_NAME): DataSource = {
    ensureInitialized(name)
    get(name).dataSource
  }

  /**
   * Borrows a [[java.sql.Connection]] from the specified connection pool.
   * @param name pool name
   * @return connection
   */
  def borrow(name: Any = DEFAULT_NAME): Connection = {
    ensureInitialized(name)
    val pool = get(name)
    log.debug("Borrowed a new connection from " + pool.toString())
    pool.borrow()
  }

}

/**
 * Connection Pool
 *
 * Using Commons DBCP internally.
 *
 * @see http://commons.apache.org/dbcp/
 */
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

  // Initialize Connection Factory
  // (not read-only, auto-commit)
  new PoolableConnectionFactory(
    new DriverManagerConnectionFactory(url, user, password),
    pool,
    null,
    settings.validationQuery,
    false,
    true)

  private val dataSource: DataSource = new PoolingDataSource(pool)

  /**
   * Borrows [[java.sql.Connection]] from pool.
   * @return connection
   */
  def borrow(): Connection = dataSource.getConnection()

  /**
   * Returns self as a String value.
   * @return printable String value
   */
  override def toString() = "ConnectionPool(url:" + url + ", user:" + user + ")"

}
