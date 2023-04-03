package scalikejdbc

/**
 * JDBC Settings
 */
case class JDBCSettings(
  url: String,
  user: String,
  password: String,
  driverName: String
) {

  override def toString(): String =
    s"JDBCSettings(${url},${user},[REDACTED],${driverName})"
}
