package scalikejdbc.streams

import scalikejdbc._

/**
 * Settings for DatabasePublisher.
 */
class DatabasePublisherSettings[A](
  val dbName: Any,
  val connectionPoolContext: DB.CPContext,
  val settingsProvider: SettingsProvider,
  val bufferNext: Boolean = true
)

object DatabasePublisherSettings {

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