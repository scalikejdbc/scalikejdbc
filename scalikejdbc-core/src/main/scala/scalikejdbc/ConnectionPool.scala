package scalikejdbc

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import javax.sql.DataSource
import java.sql.Connection
import scala.concurrent.{ ExecutionContextExecutor, ExecutionContext }

/**
 * Connection Pool
 *
 * The default implementation uses Commons DBCP 2 internally.
 *
 * @see [[https://commons.apache.org/proper/commons-dbcp/]]
 */
object ConnectionPool extends LogSupport {

  /**
   * The default execution context used by async workers for connection pools management.
   */
  private lazy val DEFAULT_EXECUTION_CONTEXT: ExecutionContextExecutor = {
    ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(
        3,
        new ThreadFactory {
          private val i = new AtomicInteger(0)
          override def newThread(r: Runnable): Thread = {
            val thread = new Thread(
              r,
              s"scalikejdbc-connection-pool-default-ec-${i.incrementAndGet()}"
            )
            thread.setDaemon(true)
            thread
          }
        }
      )
    )
  }

  type MutableMap[A, B] = scala.collection.mutable.HashMap[A, B]
  type CPSettings = ConnectionPoolSettings
  type CPFactory = ConnectionPoolFactory

  val DEFAULT_NAME: String = "default"
  val DEFAULT_CONNECTION_POOL_FACTORY = Commons2ConnectionPoolFactory

  private[this] val pools = new MutableMap[Any, ConnectionPool]()

  /**
   * Returns true when the specified Connection pool is already initialized.
   *
   * @param name pool name
   * @return is initialized
   */
  def isInitialized(name: Any = DEFAULT_NAME): Boolean = pools.synchronized {
    pools.get(name).isDefined
  }

  private def ensureInitialized(name: Any): Unit = {
    if (!isInitialized(name)) {
      val message =
        ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED + "(name:" + name + ")"
      throw new IllegalStateException(message)
    }
  }

  /**
   * Returns Connection pool. If the specified Connection pool does not exist, throws IllegalStateException.
   *
   * @param name pool name
   * @return connection pool
   * @throws IllegalStateException if the specified Connection pool does not exist
   */
  def apply(name: Any = DEFAULT_NAME): ConnectionPool = get(name)

  /**
   * Returns Connection pool. If the specified Connection pool does not exist, throws IllegalStateException.
   *
   * @param name pool name
   * @return connection pool
   * @throws IllegalStateException if the specified Connection pool does not exist
   */
  def get(name: Any = DEFAULT_NAME): ConnectionPool = pools.synchronized {
    pools.getOrElse(
      name, {
        val message =
          ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED + "(name:" + name + ")"
        throw new IllegalStateException(message)
      }
    )
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
  def add(
    name: Any,
    url: String,
    user: String,
    password: String,
    settings: CPSettings = ConnectionPoolSettings()
  )(implicit
    factory: CPFactory = DEFAULT_CONNECTION_POOL_FACTORY,
    ec: ExecutionContext = DEFAULT_EXECUTION_CONTEXT
  ): Unit = {

    import scalikejdbc.JDBCUrl._

    val (_factory, factoryName) = Option(settings.connectionPoolFactoryName)
      .map { name =>
        ConnectionPoolFactoryRepository
          .get(name)
          .map(f => (f, name))
          .getOrElse {
            val message =
              ErrorMessage.INVALID_CONNECTION_POOL_FACTORY_NAME + "(name:" + name + ")"
            throw new IllegalArgumentException(message)
          }
      }
      .getOrElse((factory, "<default>"))

    // register new pool or replace existing pool
    pools.synchronized {
      val oldPoolOpt: Option[ConnectionPool] = pools.get(name)

      // Heroku support
      val pool = url match {
        case HerokuPostgresRegexp(_user, _password, _host, _dbname) =>
          val _url = s"jdbc:postgresql://${_host}/${_dbname}"
          _factory.apply(_url, _user, _password, settings)
        case url @ HerokuMySQLRegexp(_user, _password, _host, _dbname) =>
          val defaultProperties =
            """?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci"""
          val addDefaultPropertiesIfNeeded = MysqlCustomProperties
            .findFirstMatchIn(url)
            .map(_ => "")
            .getOrElse(defaultProperties)
          val _url =
            s"jdbc:mysql://${_host}/${_dbname + addDefaultPropertiesIfNeeded}"
          _factory.apply(_url, _user, _password, settings)
        case _ =>
          _factory.apply(url, user, password, settings)
      }
      pools.update(name, pool)

      // wait a little because rarely NPE occurs when immediately accessed.
      Thread.sleep(settings.warmUpTime)

      // asynchronously close the old pool if exists
      oldPoolOpt.foreach(pool => abandonOldPool(name, pool))
    }
    if (GlobalSettings.loggingConnections) {
      log.debug(
        s"Registered connection pool : ${get(name)} using factory : $factoryName"
      )
    }
  }

  /**
   * Registers new named Connection pool.
   *
   * @param name pool name
   * @param dataSource DataSource based ConnectionPool
   */
  def add(name: Any, dataSource: DataSourceConnectionPool): Unit = {
    // NOTE: cannot pass ExecutionContext from outside due to overload issue
    // (multiple overloaded alternatives of method add define default arguments.)
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
  def add(
    name: Any,
    dataSource: AuthenticatedDataSourceConnectionPool
  ): Unit = {
    // NOTE: cannot pass ExecutionContext from outside due to overload issue
    // (multiple overloaded alternatives of method add define default arguments.)
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

  private[this] def abandonOldPool(name: Any, oldPool: ConnectionPool)(implicit
    ec: ExecutionContext = DEFAULT_EXECUTION_CONTEXT
  ) = {
    scala.concurrent.Future {
      if (GlobalSettings.loggingConnections) {
        log.debug(
          "The old pool destruction started. connection pool : " + get(name)
            .toString()
        )
      }
      var millis = 0L
      while (millis < 60000L && oldPool.numActive > 0) {
        Thread.sleep(100L)
        millis += 100L
      }
      oldPool.close()
      if (GlobalSettings.loggingConnections) {
        log.debug(
          "The old pool is successfully closed. connection pool : " + get(name)
            .toString()
        )
      }
    }
  }

  /**
   * Registers the default Connection pool.
   *
   * @param url JDBC URL
   * @param user JDBC username
   * @param password JDBC password
   * @param settings Settings
   */
  def singleton(
    url: String,
    user: String,
    password: String,
    settings: CPSettings = ConnectionPoolSettings()
  )(implicit factory: CPFactory = DEFAULT_CONNECTION_POOL_FACTORY): Unit = {
    add(DEFAULT_NAME, url, user, password, settings)(factory)
    if (GlobalSettings.loggingConnections) {
      log.debug("Registered singleton connection pool : " + get().toString())
    }
  }

  /**
   * Registers the default Connection pool.
   *
   * @param dataSource DataSource
   */
  def singleton(dataSource: DataSourceConnectionPool): Unit = {
    add(DEFAULT_NAME, dataSource)
    if (GlobalSettings.loggingConnections) {
      log.debug("Registered singleton connection pool : " + get().toString())
    }
  }

  /**
   * Registers the default Connection pool.
   *
   * @param dataSource DataSource
   */
  def singleton(dataSource: AuthenticatedDataSourceConnectionPool): Unit = {
    add(DEFAULT_NAME, dataSource)
    if (GlobalSettings.loggingConnections) {
      log.debug("Registered singleton connection pool : " + get().toString())
    }
  }

  /**
   * Returns javax.sql.DataSource.
   *
   * @param name pool name
   * @return datasource
   */
  def dataSource(name: Any = DEFAULT_NAME): DataSource = {
    ensureInitialized(name)
    get(name).dataSource
  }

  /**
   * Borrows a java.sql.Connection from the specified connection pool.
   *
   * @param name pool name
   * @return connection
   */
  def borrow(name: Any = DEFAULT_NAME): Connection = {
    ensureInitialized(name)
    val pool = get(name)
    if (GlobalSettings.loggingConnections) {
      log.debug("Borrowed a new connection from " + pool.toString())
    }
    pool.borrow()
  }

  /**
   * Close a pool by name
   *
   * @param name pool name
   */
  def close(name: Any = DEFAULT_NAME): Unit = {
    pools.synchronized {
      val removed = pools.remove(name)
      removed.foreach { _.close() }
    }
  }

  /**
   * Close all connection pools
   */
  def closeAll(): Unit = {
    pools.synchronized {
      pools.foreach { case (name, pool) =>
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
  val settings: ConnectionPoolSettings = ConnectionPoolSettings()
) extends AutoCloseable {

  /**
   * Borrows java.sql.Connection from pool.
   *
   * @return connection
   */
  def borrow(): Connection

  /**
   * Returns javax.sql.DataSource object.
   *
   * @return datasource
   */
  def dataSource: DataSource

  /**
   * Returns num of active connections.
   *
   * @return num
   */
  def numActive: Int = throw new UnsupportedOperationException

  /**
   * Returns num of idle connections.
   *
   * @return num
   */
  def numIdle: Int = throw new UnsupportedOperationException

  /**
   * Returns max limit of active connections.
   *
   * @return num
   */
  def maxActive: Int = throw new UnsupportedOperationException

  /**
   * Returns max limit of idle connections.
   *
   * @return num
   */
  def maxIdle: Int = throw new UnsupportedOperationException

  /**
   * Returns self as a String value.
   *
   * @return printable String value
   */
  override def toString(): String =
    "ConnectionPool(url:" + url + ", user:" + user + ")"

  /**
   * Close this connection pool.
   */
  def close(): Unit = throw new UnsupportedOperationException

  def connectionAttributes: DBConnectionAttributes = {
    val timeZoneSettings = Option(settings.timeZone).fold(TimeZoneSettings()) {
      timeZone =>
        TimeZoneSettings(true, java.util.TimeZone.getTimeZone(timeZone))
    }
    DBConnectionAttributes(
      driverName = Option(settings.driverName),
      timeZoneSettings = timeZoneSettings
    )
  }

}
