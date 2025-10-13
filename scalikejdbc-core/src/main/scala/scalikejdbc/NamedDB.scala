package scalikejdbc

import java.sql.Connection

/**
 * Named Basic DB Accessor
 *
 * It's easier to use named ConnectionPool with this class.
 *
 * {{{
 * ConnectionPool.add("named", "jdbc:...", "user", "password")
 * val users = NamedDB("named") readOnly { session =>
 *   session.list("select * from user")
 * }
 * }}}
 *
 * Please note that a single NamedDB instance should be used only once, as the connection is closed
 * after being used. To re-use an instance, use the .setAutoClose(false) method.
 */
case class NamedDB(
  name: Any,
  settingsProvider: SettingsProvider = SettingsProvider.default
)(implicit context: ConnectionPoolContext = NoConnectionPoolContext)
  extends DBConnection {

  private[this] def connectionPool(): ConnectionPool = Option(context match {
    case NoConnectionPoolContext          => ConnectionPool(name)
    case _: MultipleConnectionPoolContext => context.get(name)
    case _                                =>
      throw new IllegalStateException(
        ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT
      )
  }) getOrElse {
    throw new IllegalStateException(
      ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED
    )
  }

  override def connectionAttributes: DBConnectionAttributes = {
    connectionPool().connectionAttributes
  }

  private lazy val db: DB = {
    val cp = connectionPool()
    DB(cp.borrow(), connectionAttributes, settingsProvider)
  }

  def toDB(): DB = db

  def conn: Connection = db.conn

}
