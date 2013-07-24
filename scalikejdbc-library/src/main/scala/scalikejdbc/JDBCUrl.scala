/*
 * Copyright 2013 Kazuhiro Sera
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

/**
 * Companion object of JDBC URL
 */
object JDBCUrl {

  // Heroku support
  val HerokuPostgresRegexp = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val HerokuMySQLRegexp = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
  val MysqlCustomProperties = ".*\\?(.*)".r

  def apply(url: String): JDBCUrl = try {
    val urlParts = url.split("/")
    val hostAndPort = urlParts(2).split(":")
    val database = urlParts(3)
    JDBCUrl(
      host = hostAndPort.head,
      port = hostAndPort.tail.headOption.map(_.toInt).getOrElse(defaultPort(url)),
      database = database
    )
  } catch {
    case e: Exception =>
      throw new IllegalArgumentException("Failed to parse JDBC URL (" + url + ")")
  }

  private[this] def defaultPort(url: String): Int = if (url.startsWith("jdbc:mysql://")) 3306 else 5432

}

/**
 * JDBC URL
 *
 * @param host
 * @param port
 * @param database
 */
case class JDBCUrl(host: String, port: Int, database: String)

