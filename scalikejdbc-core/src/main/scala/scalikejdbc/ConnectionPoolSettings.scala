package scalikejdbc

/**
 * Settings for ConnectionPool
 */
case class ConnectionPoolSettings(
  initialSize: Int = 0,
  maxSize: Int = 8,
  connectionTimeoutMillis: Long = 5000L,
  validationQuery: String = null,
  connectionPoolFactoryName: String = null,
  driverName: String = null,
  warmUpTime: Long = 100L,
  timeZone: String = null
)
