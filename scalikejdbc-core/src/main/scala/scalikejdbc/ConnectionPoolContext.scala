package scalikejdbc

import scala.collection.mutable

/**
 * Connection pool context
 */
trait ConnectionPoolContext {

  def set(name: Any, pool: ConnectionPool): Unit

  def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool

}

/**
 * Multiple connection pool context
 */
case class MultipleConnectionPoolContext(contexts: (Any, ConnectionPool)*)
  extends ConnectionPoolContext {

  def this() = {
    this(Nil: _*)
  }

  private lazy val pools = new mutable.HashMap[Any, ConnectionPool]

  contexts foreach { case (name, pool) =>
    pools.put(name, pool)
  }

  override def set(name: Any, pool: ConnectionPool): Unit =
    pools.update(name, pool)

  override def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool =
    pools.getOrElse(
      name,
      throw new IllegalStateException("No connection context for " + name + ".")
    )

}

/**
 * No Connection Pool Context
 */
object NoConnectionPoolContext extends ConnectionPoolContext {

  override def set(name: Any, pool: ConnectionPool): Unit =
    throw new IllegalStateException(ErrorMessage.NO_CONNECTION_POOL_CONTEXT)

  override def get(name: Any = ConnectionPool.DEFAULT_NAME): ConnectionPool =
    throw new IllegalStateException(ErrorMessage.NO_CONNECTION_POOL_CONTEXT)

}
