package scalikejdbc

/**
 * JDBC Settings
 */
case class JDBCSettings(
  url: String,
  user: String,
  password: String,
  driverName: String
)
