package scalikejdbc

import javax.sql.DataSource
import java.sql.Connection

/**
 * Connection Pool using external DataSource
 */
class DataSourceConnectionPool(
  override val dataSource: DataSource,
  settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings()
)
    extends ConnectionPool(
      url = "<external-data-source>",
      user = "<external-data-source>",
      password = "<external-data-source>",
      settings = ConnectionPoolSettings(driverName = settings.driverName)
    ) {

  override def borrow(): Connection = dataSource.getConnection()
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
  settings: DataSourceConnectionPoolSettings = DataSourceConnectionPoolSettings()
)
    extends ConnectionPool(
      url = "<external-data-source>",
      user = user,
      password = password,
      settings = ConnectionPoolSettings(driverName = settings.driverName)
    ) {

  override def borrow(): Connection = dataSource.getConnection(user, password)
}
