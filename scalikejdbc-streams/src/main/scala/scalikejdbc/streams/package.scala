package scalikejdbc

import scalikejdbc.GeneralizedTypeConstraintsForWithExtractor.=:=

import scala.concurrent.ExecutionContext

/**
 * Reactive Streams support.
 *
 * see also [[http://www.reactive-streams.org/]]
 */
package object streams {

  val DefaultFetchSize: Int = 1000

  /**
   * Creates a new DatabasePublisher.
   */
  private def createDatabasePublisher[A, E <: WithExtractor](
    sql: StreamReadySQL[A],
    connectionPoolName: Any = ConnectionPool.DEFAULT_NAME
  )(
    implicit
    executionContext: ExecutionContext,
    cpContext: DB.CPContext,
    settings: SettingsProvider
  ): DatabasePublisher[A] = {
    val publisherSettings = DatabasePublisherSettings[A](connectionPoolName)
    DatabasePublisherFactory.createNewPublisher[A](publisherSettings, AsyncExecutor(executionContext), sql)
  }

  /**
   * An implicit to enable the `DB.readOnlyStream` method:
   *
   * {{{
   * val publisher = DB.readOnlyStream {
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class EnableDBCodeBlockToProvideDatabasePublisher(val db: DB.type) extends AnyVal {

    def readOnlyStream[A](sql: StreamReadySQL[A])(implicit
      executionContext: ExecutionContext,
      cpContext: DB.CPContext = DB.NoCPContext,
      settings: SettingsProvider = SettingsProvider.default): DatabasePublisher[A] = {

      createDatabasePublisher(sql)
    }
  }

  /**
   * An implicit to enable the `NamedDB('name).readOnlyStream` method:
   *
   * {{{
   * val publisher = NamedDB('name).readOnlyStream {
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class EnableNamedDBCodeBlockToProvideDatabasePublisher(val db: NamedDB) extends AnyVal {

    def readOnlyStream[A, E <: WithExtractor](sql: StreamReadySQL[A])(implicit
      executionContext: ExecutionContext,
      cpContext: DB.CPContext = DB.NoCPContext): DatabasePublisher[A] = {

      createDatabasePublisher(sql, db.name)(executionContext, cpContext, db.settingsProvider)
    }
  }

  /**
   * An implicit to enable the `iterator` method:
   *
   * {{{
   * val publisher = DB.readOnlyStream {
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class FromSQLToStreamSQLConverter[A, E <: WithExtractor](val sql: SQL[A, E]) extends AnyVal {

    def iterator()(
      implicit
      hasExtractor: SQL[A, E]#ThisSQL =:= SQL[A, E]#SQLWithExtractor
    ): StreamReadySQL[A] = {
      StreamReadySQL[A, E](sql, sql.fetchSize.getOrElse(DefaultFetchSize))
    }

  }

}
