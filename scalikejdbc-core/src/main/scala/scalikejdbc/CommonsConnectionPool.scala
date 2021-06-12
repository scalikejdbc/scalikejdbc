package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection

/**
 * Commons DBCP Connection Pool
 *
 * @see [[https://commons.apache.org/proper/commons-dbcp/]]
 */
class CommonsConnectionPool(
  override val url: String,
  override val user: String,
  password: String,
  override val settings: ConnectionPoolSettings = ConnectionPoolSettings()
) extends ConnectionPool(url, user, password, settings) {

  import org.apache.commons.pool.impl.GenericObjectPool
  import org.apache.commons.dbcp.{
    PoolingDataSource,
    PoolableConnectionFactory,
    DriverManagerConnectionFactory
  }

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
    true
  )

  private[this] val _dataSource: DataSource = new PoolingDataSource(_pool)

  override def dataSource: DataSource = _dataSource

  override def borrow(): Connection = dataSource.getConnection()

  override def numActive: Int = _pool.getNumActive

  override def numIdle: Int = _pool.getNumIdle

  override def maxActive: Int = _pool.getMaxActive

  override def maxIdle: Int = _pool.getMaxIdle

  override def close(): Unit = _pool.close()

}
