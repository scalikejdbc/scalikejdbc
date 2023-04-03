package scalikejdbc

/**
 * Connection Pool Factory
 *
 * @see [[https://commons.apache.org/proper/commons-dbcp/]]
 */
object CommonsConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ): CommonsConnectionPool = {
    new CommonsConnectionPool(url, user, password, settings)
  }

}
