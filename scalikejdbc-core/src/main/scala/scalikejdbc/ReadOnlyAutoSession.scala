package scalikejdbc

import java.sql.Connection

/**
 * Represents that already existing session will be used or a new read-only session will be started.
 */
case object ReadOnlyAutoSession extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  override val tx: Option[Tx] = None
  val isReadOnly: Boolean = true

  override def fetchSize(fetchSize: Int): this.type = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): this.type =
    unexpectedInvocation
  override def tags(tags: String*): this.type = unexpectedInvocation
  override def queryTimeout(seconds: Int): this.type = unexpectedInvocation
  override def queryTimeout(seconds: Option[Int]): this.type =
    unexpectedInvocation
  override private[scalikejdbc] lazy val connectionAttributes
    : DBConnectionAttributes = unexpectedInvocation

  override protected[scalikejdbc] def settings = SettingsProvider.default
}

/**
 * Represents that already existing session will be used or a new read-only session
 * which is retrieved from named connection pool will be started.
 */
case class ReadOnlyNamedAutoSession(
  name: Any,
  settings: SettingsProvider = SettingsProvider.default
) extends DBSession {
  override private[scalikejdbc] val conn: Connection = null
  override val tx: Option[Tx] = None
  val isReadOnly: Boolean = true

  override def fetchSize(fetchSize: Int): this.type = unexpectedInvocation
  override def fetchSize(fetchSize: Option[Int]): this.type =
    unexpectedInvocation
  override def tags(tags: String*): this.type = unexpectedInvocation
  override def queryTimeout(seconds: Int): this.type = unexpectedInvocation
  override def queryTimeout(seconds: Option[Int]): this.type =
    unexpectedInvocation
  override private[scalikejdbc] lazy val connectionAttributes
    : DBConnectionAttributes = unexpectedInvocation
}
