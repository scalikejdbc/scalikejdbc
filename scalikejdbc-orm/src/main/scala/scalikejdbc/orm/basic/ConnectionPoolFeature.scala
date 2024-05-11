package scalikejdbc.orm.basic

// Don't change this import
import scalikejdbc._

/**
 * Provides ConnectionPool.
 */
trait ConnectionPoolFeature {
  self: SQLSyntaxSupport[?] =>

  /**
   * Returns connection pool.
   *
   * @return pool
   */
  def connectionPool: ConnectionPool = ConnectionPool(connectionPoolName)

}
