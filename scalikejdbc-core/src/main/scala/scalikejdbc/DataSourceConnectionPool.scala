package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection

/**
 * Connection Pool using external DataSource
 */
class DataSourceConnectionPool(
  override val dataSource: DataSource,
  settings: DataSourceConnectionPoolSettings,
  closer: DataSourceCloser
)
    extends ConnectionPool(
      url = "<external-data-source>",
      user = "<external-data-source>",
      password = "<external-data-source>",
      settings = ConnectionPoolSettings(driverName = settings.driverName)
    ) {

  def this(dataSource: DataSource) {
    this(dataSource, DataSourceConnectionPoolSettings(), DefaultDataSourceCloser)
  }

  def this(dataSource: DataSource, settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings()) {
    this(dataSource, settings, DefaultDataSourceCloser)
  }

  def this(dataSource: DataSource, closer: DataSourceCloser) {
    this(dataSource, DataSourceConnectionPoolSettings(), closer)
  }

  override def borrow(): Connection = dataSource.getConnection()

  override def close(): Unit = closer.close()
}

/**
 * Connection Pool using external DataSource
 *
 * Note: Commons-DBCP doesn't support this API.
 */
class AuthenticatedDataSourceConnectionPool(
  override val dataSource: DataSource,
  override val user: String,
  password: String,
  settings: DataSourceConnectionPoolSettings,
  closer: DataSourceCloser
)
    extends ConnectionPool(
      url = "<external-data-source>",
      user = user,
      password = password,
      settings = ConnectionPoolSettings(driverName = settings.driverName)
    ) {

  def this(dataSource: DataSource, user: String, password: String) {
    this(dataSource, user, password, DataSourceConnectionPoolSettings(), DefaultDataSourceCloser)
  }

  def this(dataSource: DataSource, user: String, password: String, settings: DataSourceConnectionPoolSettings) {
    this(dataSource, user, password, settings, DefaultDataSourceCloser)
  }

  def this(dataSource: DataSource, user: String, password: String, closer: DataSourceCloser) {
    this(dataSource, user, password, DataSourceConnectionPoolSettings(), closer)
  }

  override def borrow(): Connection = dataSource.getConnection(user, password)

  override def close(): Unit = closer.close()
}

/**
 * Closing support for DataSource
 */
trait DataSourceCloser {
  def close(): Unit
}

private case object DefaultDataSourceCloser extends DataSourceCloser {
  def close(): Unit = throw new UnsupportedOperationException
}
