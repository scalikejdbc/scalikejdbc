package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection

/**
 * Connection Pool using external DataSource
 */
class DataSourceConnectionPool(override val dataSource: DataSource, settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings())
    extends ConnectionPool("<external-data-source>", "<external-data-source>", "<external-data-source>", ConnectionPoolSettings(driverName = settings.driverName)) {

  override def borrow(): Connection = dataSource.getConnection()
}

/**
 * Connection Pool using external DataSource
 *
 * Note: Commons-DBCP doesn't support this API.
 */
class AuthenticatedDataSourceConnectionPool(
  override val dataSource: DataSource, override val user: String, password: String, settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings()
)
    extends ConnectionPool("<external-data-source>", user, password, ConnectionPoolSettings(driverName = settings.driverName)) {

  override def borrow(): Connection = dataSource.getConnection(user, password)
}
