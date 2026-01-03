package scalikejdbc

import scala.util.matching.Regex

/**
 * Companion object of JDBC URL
 */
object JDBCUrl {

  // Heroku support
  val HerokuPostgresRegexp: Regex =
    "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val HerokuMySQLRegexp: Regex =
    "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val MysqlCustomProperties: Regex = ".*\\?(.*)".r

  def apply(url: String): JDBCUrl = try {
    val urlParts = url.split("/")
    val hostAndPort = urlParts(2).split(":")
    val (host, port) = (
      hostAndPort.head,
      hostAndPort.tail.headOption.map(_.toInt).getOrElse(defaultPort(url))
    )
    val database = urlParts(3)

    JDBCUrl(host = host, port = port, database = database)
  } catch {
    case e: Exception =>
      throw new IllegalArgumentException(
        "Failed to parse JDBC URL (" + url + ")"
      )
  }

  private[this] def defaultPort(url: String): Int =
    if (url.startsWith("jdbc:mysql://")) 3306 else 5432

}

/**
 * JDBC URL which contains host, port and database name
 *
 */
case class JDBCUrl(host: String, port: Int, database: String)
