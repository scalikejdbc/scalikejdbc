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
import org.apache.commons.dbcp.{PoolingDataSource, PoolableConnectionFactory, DriverManagerConnectionFactory}
import javax.sql.DataSource
import java.sql.Connection

object ConnectionPool {

  var SINGLETON: ConnectionPool = null

  def isInitialized = SINGLETON != null

  def ensureInitialized(): Unit = if (!isInitialized) {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  def initialize(url: String,
                 user: String,
                 password: String,
                 settings: ConnectionPoolSettings = ConnectionPoolSettings()): Unit = {
    SINGLETON = new ConnectionPool(url, user, password, settings)
  }

  def dataSource(): DataSource = {
    ensureInitialized()
    SINGLETON.dataSource
  }

  def borrow(): Connection = {
    ensureInitialized()
    SINGLETON.borrow()
  }

}

case class ConnectionPoolSettings(initialSize: Int = 0,
                                  maxSize: Int = 8,
                                  validationQuery: String = null)

class ConnectionPool(url: String,
                     user: String,
                     password: String,
                     settings: ConnectionPoolSettings = ConnectionPoolSettings()) {

  private val pool = new GenericObjectPool(null);
  pool.setMinIdle(settings.initialSize);
  pool.setMaxIdle(settings.maxSize)
  pool.setMaxActive(settings.maxSize)
  pool.setMaxWait(5000)
  pool.setWhenExhaustedAction(GenericObjectPool.WHEN_EXHAUSTED_FAIL)
  pool.setTestOnBorrow(true)

  private val connFactory = new DriverManagerConnectionFactory(url, user, password)

  // not read-only, auto-commit
  val poolableConnectionFactory = new PoolableConnectionFactory(
    connFactory, pool, null, settings.validationQuery, false, true);

  val dataSource: DataSource = new PoolingDataSource(pool)

  def borrow(): Connection = dataSource.getConnection()

}