package scalikejdbc

/**
 * Connection Pool Factory
 *
 * @see [[http://commons.apache.org/dbcp/]]
 */
object CommonsConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ) = {
    new CommonsConnectionPool(url, user, password, settings)
  }

}
