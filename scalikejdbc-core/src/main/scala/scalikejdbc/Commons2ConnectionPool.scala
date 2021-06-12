package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection
import org.apache.commons.dbcp2.PoolingDataSource
import org.apache.commons.dbcp2.PoolableConnection
import org.apache.commons.dbcp2.PoolableConnectionFactory
import org.apache.commons.dbcp2.DriverManagerConnectionFactory
import org.apache.commons.pool2.impl.GenericObjectPool

/**
 * Commons DBCP Connection Pool
 *
 * @see [[https://commons.apache.org/proper/commons-dbcp/]]
 */
class Commons2ConnectionPool(
  override val url: String,
  override val user: String,
  password: String,
  override val settings: ConnectionPoolSettings = ConnectionPoolSettings()
) extends ConnectionPool(url, user, password, settings) {

  private[this] val _poolFactory = new PoolableConnectionFactory(
    new DriverManagerConnectionFactory(url, user, password),
    null
  )
  _poolFactory.setValidationQuery(settings.validationQuery)

  private[this] val _pool: GenericObjectPool[PoolableConnection] =
    new GenericObjectPool(_poolFactory)
  _poolFactory.setPool(_pool)

  _pool.setMinIdle(settings.initialSize)
  _pool.setMaxIdle(settings.maxSize)
  _pool.setBlockWhenExhausted(true)
  _pool.setMaxTotal(settings.maxSize)
  _pool.setMaxWaitMillis(settings.connectionTimeoutMillis)

  // To fix MS SQLServer jtds driver issue
  // https://github.com/scalikejdbc/scalikejdbc/issues/461
  if (Option(settings.validationQuery).exists(_.trim.nonEmpty)) {
    _pool.setTestOnBorrow(true)
  }

  private[this] val _dataSource: DataSource = new PoolingDataSource(_pool)

  override def dataSource: DataSource = _dataSource

  override def borrow(): Connection = dataSource.getConnection()

  override def numActive: Int = _pool.getNumActive

  override def numIdle: Int = _pool.getNumIdle

  override def maxActive: Int = _pool.getMaxTotal

  override def maxIdle: Int = _pool.getMaxIdle

  override def close(): Unit = _pool.close()

}
