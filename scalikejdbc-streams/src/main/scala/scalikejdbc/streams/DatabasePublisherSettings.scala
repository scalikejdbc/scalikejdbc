package scalikejdbc.streams

import scalikejdbc._

/**
 * Settings for DatabasePublisher.
 */
private[streams] class DatabasePublisherSettings[A](
  /**
   * Connection pool name.
   */
  private[streams] val dbName: Any,
  /**
   * Context for connection pool
   */
  private[streams] val connectionPoolContext: DB.CPContext,
  /**
   * Connection pool settings provider.
   */
  private[streams] val settingsProvider: SettingsProvider,
  /**
   * DatabasePublisher has a buffer internally if true.
   */
  private[streams] val bufferNext: Boolean = true
)

private[streams] object DatabasePublisherSettings {

  /**
   * Creates and returns a DatabasePublisherSettings.
   */
  private[streams] def apply[A](dbName: Any)(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default
  ): DatabasePublisherSettings[A] = {
    new DatabasePublisherSettings(dbName, context, settingsProvider)
  }

}
