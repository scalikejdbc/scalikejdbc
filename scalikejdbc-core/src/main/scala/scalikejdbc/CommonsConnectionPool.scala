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
 * Commons DBCP Connection Pool
 *
 * @see [[http://commons.apache.org/dbcp/]]
 */
class CommonsConnectionPool(
  override val url: String,
  override val user: String,
  password: String,
  override val settings: ConnectionPoolSettings = ConnectionPoolSettings())
    extends ConnectionPool(url, user, password, settings) {

  import org.apache.commons.pool.impl.GenericObjectPool
  import org.apache.commons.dbcp.{ PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory }

  private[this] val _pool = new GenericObjectPool(null)
  _pool.setMinIdle(settings.initialSize)
  _pool.setMaxIdle(settings.maxSize)
  _pool.setMaxActive(settings.maxSize)
  _pool.setMaxWait(settings.connectionTimeoutMillis)
  _pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_BLOCK)
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

