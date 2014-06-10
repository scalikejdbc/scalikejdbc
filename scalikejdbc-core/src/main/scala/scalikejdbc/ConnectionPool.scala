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

  val DEFAULT_NAME: Symbol = 'default
  // TODO commons-dbcp2 will be the default implementation since ScalikeJDBC 2.1
  val DEFAULT_CONNECTION_POOL_FACTORY = CommonsConnectionPoolFactory

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
    pools.get(name).getOrElse {
      val message = ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED + "(name:" + name + ")"
      throw new IllegalStateException(message)
    }
  }

  /**
   * Registers new named Connection pool.
   *
   * @param name pool name
   * @param url JDBC URL
   * @param user JDBC username
   * @param password JDBC password
   * @param settings Settings
   */
  def add(name: Any, url: String, user: String, password: String,
    settings: CPSettings = ConnectionPoolSettings())(implicit factory: CPFactory = DEFAULT_CONNECTION_POOL_FACTORY) {

    import scalikejdbc.JDBCUrl._

    val _factory = Option(settings.connectionPoolFactoryName).flatMap { name =>
      ConnectionPoolFactoryRepository.get(name)
    }.getOrElse(factory)

    // register new pool or replace existing pool
    pools.synchronized {
      val oldPoolOpt: Option[ConnectionPool] = pools.get(name)

      // Heroku support
      val pool = url match {
        case HerokuPostgresRegexp(_user, _password, _host, _dbname) =>
          val _url = "jdbc:postgresql://%s/%s".format(_host, _dbname)
          _factory.apply(_url, _user, _password, settings)
        case url @ HerokuMySQLRegexp(_user, _password, _host, _dbname) =>
          val defaultProperties = """?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"""
          val addDefaultPropertiesIfNeeded = MysqlCustomProperties.findFirstMatchIn(url).map(_ => "").getOrElse(defaultProperties)
          val _url = "jdbc:mysql://%s/%s".format(_host, _dbname + addDefaultPropertiesIfNeeded)
          _factory.apply(_url, _user, _password, settings)
        case _ =>
          _factory.apply(url, user, password, settings)
      }
      pools.update(name, pool)

      // wait a little because rarely NPE occurs when immediately accessed.
      Thread.sleep(100L)

      // asynchronously close the old pool if exists
      oldPoolOpt.foreach(pool => abandonOldPool(name, pool))
    }

    log.debug("Registered connection pool : " + get(name).toString())
  }

  private[this] def abandonOldPool(name: Any, oldPool: ConnectionPool) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    scala.concurrent.Future {
      log.debug("The old pool destruction started. connection pool : " + get(name).toString())
      var millis = 0L
      while (millis < 60000L && oldPool.numActive > 0) {
        Thread.sleep(100L)
        millis += 100L
      }
      oldPool.close()
      log.debug("The old pool is successfully closed. connection pool : " + get(name).toString())
    }
  }

  /**
   * Registers new named Connection pool.
   *
   * @param name pool name
   * @param dataSource DataSource based ConnectionPool
   */
  def add(name: Any, dataSource: DataSourceConnectionPool) = {
    val oldPoolOpt: Option[ConnectionPool] = pools.get(name)
    // register new pool or replace existing pool
    pools.synchronized {
      pools.update(name, dataSource)
      // wait a little because rarely NPE occurs when immediately accessed.
      Thread.sleep(100L)
    }
    // asynchronously close the old pool if exists
    oldPoolOpt.foreach(pool => abandonOldPool(name, pool))
  }

  /**
   * Registers new named Connection pool.
   *
   * @param name pool name
   * @param dataSource DataSource based ConnectionPool
   */
  def add(name: Any, dataSource: AuthenticatedDataSourceConnectionPool) = {
    val oldPoolOpt: Option[ConnectionPool] = pools.get(name)
    // register new pool or replace existing pool
    pools.synchronized {
      pools.update(name, dataSource)
      // wait a little because rarely NPE occurs when immediately accessed.
      Thread.sleep(100L)
    }
    // asynchronously close the old pool if exists
    oldPoolOpt.foreach(pool => abandonOldPool(name, pool))
  }

  /**
   * Registers the default Connection pool.
   *
   * @param url JDBC URL
   * @param user JDBC username
   * @param password JDBC password
   * @param settings Settings
   */
  def singleton(url: String, user: String, password: String,
    settings: CPSettings = ConnectionPoolSettings())(implicit factory: CPFactory = DEFAULT_CONNECTION_POOL_FACTORY): Unit = {
    add(DEFAULT_NAME, url, user, password, settings)(factory)
    log.debug("Registered singleton connection pool : " + get().toString())
  }

  /**
   * Registers the default Connection pool.
   * @param dataSource DataSource
   */
  def singleton(dataSource: DataSourceConnectionPool): Unit = {
    add(DEFAULT_NAME, dataSource)
    log.debug("Registered singleton connection pool : " + get().toString())
  }

  /**
   * Registers the default Connection pool.
   * @param dataSource DataSource
   */
  def singleton(dataSource: AuthenticatedDataSourceConnectionPool): Unit = {
    add(DEFAULT_NAME, dataSource)
    log.debug("Registered singleton connection pool : " + get().toString())
  }

  /**
   * Returns javax.sql.DataSource.
   * @param name pool name
   * @return datasource
   */
  def dataSource(name: Any = DEFAULT_NAME): DataSource = {
    ensureInitialized(name)
    get(name).dataSource
  }

  /**
   * Borrows a java.sql.Connection from the specified connection pool.
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
  def close(name: Any = DEFAULT_NAME): Unit = {
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
abstract class ConnectionPool(
    val url: String,
    val user: String,
    password: String,
    val settings: ConnectionPoolSettings = ConnectionPoolSettings()) {

  /**
   * Borrows java.sql.Connection from pool.
   * @return connection
   */
  def borrow(): Connection

  /**
   * Returns javax.sql.DataSource object.
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

