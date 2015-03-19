package scalikejdbc

import java.sql.Connection

/**
 * Represents that already existing session will be used or a new read-only session will be started.
 */
case object ReadOnlyAutoSession extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  @deprecated("this unused method will be removed", "2.2.7")
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = true

  override def fetchSize(fetchSize: Int): DBSession = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): DBSession = unexpectedInvocation
  override def tags(tags: String*): DBSession = unexpectedInvocation
  override private[scalikejdbc] lazy val connectionAttributes: DBConnectionAttributes = unexpectedInvocation
}

/**
 * Represents that already existing session will be used or a new read-only session
 * which is retrieved from named connection pool will be started.
 */
case class ReadOnlyNamedAutoSession(name: Any) extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  @deprecated("this unused method will be removed", "2.2.7")
  val tx: Option[Tx] = None
  val isReadOnly: Boolean = true

  override def fetchSize(fetchSize: Int): DBSession = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): DBSession = unexpectedInvocation
  override def tags(tags: String*): DBSession = unexpectedInvocation
  override private[scalikejdbc] lazy val connectionAttributes: DBConnectionAttributes = unexpectedInvocation
}
