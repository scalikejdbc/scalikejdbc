package scalikejdbc

/**
 * ConnectionPoolFactoryRepository
 */
object ConnectionPoolFactoryRepository {

  val COMMONS_DBCP = "commons-dbcp"
  val COMMONS_DBCP2 = "commons-dbcp2"
  val BONECP = "bonecp"

  private[this] val factories =
    new scala.collection.concurrent.TrieMap[String, ConnectionPoolFactory]()

  factories.update(COMMONS_DBCP, CommonsConnectionPoolFactory)
  factories.update(COMMONS_DBCP2, Commons2ConnectionPoolFactory)
  factories.update(BONECP, BoneCPConnectionPoolFactory)

  /**
   * Registers a connection pool factory to repository.
   */
  def add(name: String, factory: ConnectionPoolFactory): Unit =
    factories.update(name, factory)

  /**
   * Returns a connection pool factory.
   */
  def get(name: String): Option[ConnectionPoolFactory] = factories.get(name)

  /**
   * Removes a connection pool factory from repository.
   */
  def remove(name: String): Unit = factories.remove(name)

}
