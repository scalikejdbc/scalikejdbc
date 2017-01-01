package scalikejdbc.streams

import scalikejdbc.{ WithExtractor, _ }

case class DatabasePublisherSettings[A, E <: WithExtractor](
  executor: AsyncExecutor,
  name: Any,
  connectionPoolContext: DB.CPContext,
  settings: SettingsProvider,
  bufferNext: Boolean = true
)

object DatabasePublisherSettings {

  def apply[A, E <: WithExtractor](
    dbName: Any,
    executor: AsyncExecutor
  )(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): DatabasePublisherSettings[A, E] = {
    new DatabasePublisherSettings(executor, dbName, context, settingsProvider)
  }

}