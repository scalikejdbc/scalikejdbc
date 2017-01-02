package scalikejdbc.streams

import scalikejdbc._

/**
 * Settings for DatabasePublisher.
 */
private[streams] class DatabasePublisherSettings[A](
  /**
   * Connection pool name.
   */
  val dbName: Any,

  /**
   * Context for connection pool
   */
  val connectionPoolContext: DB.CPContext,

  /**
   * Connection pool settings provider.
   */
  val settingsProvider: SettingsProvider,

  /**
   * DatabasePublisher has a buffer internally if true.
   */
  val bufferNext: Boolean = true
)

object DatabasePublisherSettings {

  /**
   * Creates and returns a DatabasePublisherSettings.
   */
  def apply[A](dbName: Any)(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): DatabasePublisherSettings[A] = {
    new DatabasePublisherSettings(
      dbName,
      context,
      settingsProvider
    )
  }

}