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

  type MutableMap[A, B] = scala.collection.mutable.HashMap[A, B]
  type CPSettings = ConnectionPoolSettings
  type CPFactory = ConnectionPoolFactory

  val DEFAULT_NAME: Any = 'default

  private[this] val pools = new MutableMap[Any, ConnectionPool]()

  /**
   * Returns true when the specified Connection pool is already initialized.
   * @param name pool name
   * @return is initialized
   */
  def isInitialized(name: Any = DEFAULT_NAME) = pools.synchronized {
    pools.get(name).isDefined
  }

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
   * @return connection pool
   */
  def apply(name: Any = DEFAULT_NAME): ConnectionPool = get(name)

  /**
   * Returns Connection pool. If the specified Connection pool does not exist, returns null.
   *
   * @param name pool name
   * @return connection pool
   */
  def get(name: Any = DEFAULT_NAME): ConnectionPool = pools.synchronized {
    pools.get(name).orNull
  }

  /**
   * Register new named Connection pool.
   *
   * @param name pool name
   * @param url JDBC URL
   * @param user JDBC username
   * @param password JDBC password
   * @param settings Settings
   */
  def add(name: Any, url: String, user: String, password: String,
    settings: CPSettings = ConnectionPoolSettings())(implicit factory: CPFactory = CommonsConnectionPoolFactory) {
    pools.synchronized {
      pools.update(name, factory.apply(url, user, password, settings))
    }
    log.debug("Registered connection pool : " + get(name).toString())
  }

  /**
   * Register the default Connection pool.
   *
   * @param url JDBC URL
   * @param user JDBC username
   * @param password JDBC password
   * @param settings Settings
   */
  def singleton(url: String, user: String, password: String,
    settings: CPSettings = ConnectionPoolSettings())(implicit factory: CPFactory = CommonsConnectionPoolFactory): Unit = {
    add(DEFAULT_NAME, url, user, password, settings)(factory)
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

  /**
   * Close a pool by name
   * @param name pool name
   */
  def close(name: Any): Unit = {
    pools.synchronized {
      val removed = pools.remove(name)
      removed.foreach { pool => pool.close() }
    }
  }

  /**
   * Close all connection pools
   */
  def closeAll(): Unit = {
    pools.synchronized {
      pools.foreach {
        case (name, pool) =>
          close(name)
      }
    }
  }

}

/**
 * Connection Pool
 */
abstract class ConnectionPool(url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()) {

  /**
   * Borrows [[java.sql.Connection]] from pool.
   * @return connection
   */
  def borrow(): Connection

  /**
   * Returns [[javax.sql.DataSource]] object.
   * @return datasource
   */
  def dataSource: DataSource

  /**
   * Returns num of active connections.
   * @return num
   */
  def numActive: Int = throw new UnsupportedOperationException

  /**
   * Returns num of idle connections.
   * @return num
   */
  def numIdle: Int = throw new UnsupportedOperationException

  /**
   * Returns max limit of active connections.
   * @return num
   */
  def maxActive: Int = throw new UnsupportedOperationException

  /**
   * Returns max limit of idle connections.
   * @return num
   */
  def maxIdle: Int = throw new UnsupportedOperationException

  /**
   * Returns self as a String value.
   * @return printable String value
   */
  override def toString() = "ConnectionPool(url:" + url + ", user:" + user + ")"

  /**
   * Close this connection pool.
   */
  def close(): Unit = throw new UnsupportedOperationException

}

/**
 * Commons DBCP Connection Pool
 *
 * @see http://commons.apache.org/dbcp/
 */
class CommonsConnectionPool(url: String,
  user: String,
  password: String,
  settings: ConnectionPoolSettings = ConnectionPoolSettings())
    extends ConnectionPool(url, user, password, settings) {

  import org.apache.commons.pool.impl.GenericObjectPool
  import org.apache.commons.dbcp.{ PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory }

  private[this] val _pool = new GenericObjectPool(null)
  _pool.setMinIdle(settings.initialSize)
  _pool.setMaxIdle(settings.maxSize)
  _pool.setMaxActive(settings.maxSize)
  _pool.setMaxWait(5000)
  _pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL)
  _pool.setTestOnBorrow(true)

  // Initialize Connection Factory
  // (not read-only, auto-commit)
  new PoolableConnectionFactory(
    new DriverManagerConnectionFactory(url, user, password),
    _pool,
    null,
    settings.validationQuery,
    false,
    true)

  private[this] val _dataSource: DataSource = new PoolingDataSource(_pool)

  override def dataSource: DataSource = _dataSource

  override def borrow(): Connection = dataSource.getConnection()

  override def numActive: Int = _pool.getNumActive

  override def numIdle: Int = _pool.getNumIdle

  override def maxActive: Int = _pool.getMaxActive

  override def maxIdle: Int = _pool.getMaxIdle

  override def close(): Unit = _pool.close()

}

