package scalikejdbc.streams

import scalikejdbc.{ WithExtractor, _ }

private[streams] case class StreamingDatabaseConfig[A, E <: WithExtractor](
    executor: AsyncExecutor,
    name: Any,
    connectionPoolContext: DB.CPContext,
    settings: SettingsProvider,
    bufferNext: Boolean = true
) {

  def stream(streamingSql: StreamingSQL[A, E]): DatabasePublisher[A, E] = {
    val emitter = new StreamingEmitter[A, E]()
    new DatabasePublisher[A, E](this, streamingSql, emitter)
  }

}

object StreamingDatabaseConfig {

  def apply[A, E <: WithExtractor](
    dbName: Any,
    executor: AsyncExecutor
  )(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): StreamingDatabaseConfig[A, E] = {
    new StreamingDatabaseConfig(executor, dbName, context, settingsProvider)
  }

}

