package scalikejdbc

/**
 * Connection Pool Factory
 *
 * @see [[http://commons.apache.org/dbcp/]]
 */
object Commons2ConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ) = {
    new Commons2ConnectionPool(url, user, password, settings)
  }

}
