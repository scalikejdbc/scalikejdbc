package scalikejdbc

/**
 * Additional attributes for current JDBC connection.
 */
case class DBConnectionAttributes(
  driverName: Option[String] = None,
  timeZoneSettings: TimeZoneSettings = TimeZoneSettings()
)
