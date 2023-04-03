package scalikejdbc

/**
 * Connection Pool Factory
 *
 * @see [[https://commons.apache.org/proper/commons-dbcp/]]
 */
object Commons2ConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ): Commons2ConnectionPool = {
    new Commons2ConnectionPool(url, user, password, settings)
  }

}
