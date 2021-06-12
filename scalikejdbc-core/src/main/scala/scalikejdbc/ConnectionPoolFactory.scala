package scalikejdbc

/**
 * Connection Pool Factory
 */
trait ConnectionPoolFactory {

  def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ): ConnectionPool

}
