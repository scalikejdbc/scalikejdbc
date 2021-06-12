package scalikejdbc

/**
 * Connection Pool Factory
 *
 * @see [[https://github.com/wwadge/bonecp]]
 */
object BoneCPConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
    url: String,
    user: String,
    password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()
  ): BoneCPConnectionPool = {
    new BoneCPConnectionPool(url, user, password, settings)
  }

}
