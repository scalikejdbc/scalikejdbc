package scalikejdbc.streams

import scalikejdbc.{ ConnectionPool, DB, SettingsProvider, WithExtractor }

import scala.concurrent.ExecutionContext

/**
 * Provider of DatabasePublisher.
 */
final class DatabasePublisherProvider private[streams] (
    private[streams] val connectionPoolName: Any = ConnectionPool.DEFAULT_NAME,
    private[streams] val settings: SettingsProvider = SettingsProvider.default,
    private[streams] val sessionModification: SessionModification = SessionModification.default
) {

  /**
   * Returns a provider instance providing a DatabasePublisher
   * that uses the DB session modified with SessionModification.
   */
  def withSessionModification(sessionModification: SessionModification): DatabasePublisherProvider = {
    new DatabasePublisherProvider(connectionPoolName, settings, sessionModification)
  }

  /**
   * Create a new DatabasePublisher for streaming.
   */
  def readOnlyStream[A](sql: StreamReadySQL[A])(implicit
    executionContext: ExecutionContext,
    cpContext: DB.CPContext = DB.NoCPContext): DatabasePublisher[A] = {

    createDatabasePublisher(sql, connectionPoolName, sessionModification, executionContext, cpContext, settings)
  }

  /**
   * Creates a new DatabasePublisher.
   */
  private def createDatabasePublisher[A, E <: WithExtractor](
    sql: StreamReadySQL[A],
    connectionPoolName: Any,
    sessionModification: SessionModification,
    executionContext: ExecutionContext,
    cpContext: DB.CPContext,
    settings: SettingsProvider
  ): DatabasePublisher[A] = {
    val publisherSettings = DatabasePublisherSettings[A](connectionPoolName, sessionModification)
    DatabasePublisherFactory.createNewPublisher[A](publisherSettings, AsyncExecutor(executionContext), sql)
  }
}
