package scalikejdbc.streams

import scalikejdbc._

/**
 * Settings for DatabasePublisher.
 */
class DatabasePublisherSettings[A](
  val connectionPoolName: Any,
  val connectionPoolContext: DB.CPContext,
  val settingsProvider: SettingsProvider,
  val bufferNext: Boolean = true
)

object DatabasePublisherSettings {

  def apply[A](connectionPoolName: Any)(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): DatabasePublisherSettings[A] = {
    new DatabasePublisherSettings(
      connectionPoolName,
      context,
      settingsProvider
    )
  }

}