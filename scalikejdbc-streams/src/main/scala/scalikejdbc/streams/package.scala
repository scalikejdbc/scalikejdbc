package scalikejdbc

import scalikejdbc.GeneralizedTypeConstraintsForWithExtractor.=:=

import scala.concurrent.ExecutionContext

/**
 * Reactive Streams support.
 *
 * see also [[https://www.reactive-streams.org/]]
 */
package object streams {

  val DefaultFetchSize: Int = 1000

  /**
   * Creates a new DatabasePublisher.
   */
  private def createDatabasePublisher[A](
    sql: StreamReadySQL[A],
    connectionPoolName: Any = ConnectionPool.DEFAULT_NAME
  )(implicit
    executionContext: ExecutionContext,
    cpContext: DB.CPContext,
    settings: SettingsProvider
  ): DatabasePublisher[A] = {
    val publisherSettings = DatabasePublisherSettings[A](connectionPoolName)
    DatabasePublisherFactory.createNewPublisher[A](
      publisherSettings,
      AsyncExecutor(executionContext),
      sql
    )
  }

  implicit class EnableConnectionPoolCodeBlockToProvideDatabasePublisher(
    private val pool: ConnectionPool
  ) extends AnyVal {

    def readOnlyStream[A](sql: StreamReadySQL[A])(implicit
      executionContext: ExecutionContext,
      settings: SettingsProvider = SettingsProvider.default
    ): DatabasePublisher[A] = {

      createDatabasePublisher(sql, ConnectionPool.DEFAULT_NAME)(using
        executionContext,
        MultipleConnectionPoolContext(ConnectionPool.DEFAULT_NAME -> pool),
        settings
      )
    }
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
  implicit class EnableDBCodeBlockToProvideDatabasePublisher(
    private val db: DB.type
  ) extends AnyVal {

    def readOnlyStream[A](sql: StreamReadySQL[A])(implicit
      executionContext: ExecutionContext,
      cpContext: DB.CPContext = DB.NoCPContext,
      settings: SettingsProvider = SettingsProvider.default
    ): DatabasePublisher[A] = {

      createDatabasePublisher(sql)
    }
  }

  /**
   * An implicit to enable the `NamedDB("name").readOnlyStream` method:
   *
   * {{{
   * val publisher = NamedDB("name").readOnlyStream {
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class EnableNamedDBCodeBlockToProvideDatabasePublisher(
    private val db: NamedDB
  ) extends AnyVal {

    def readOnlyStream[A](sql: StreamReadySQL[A])(implicit
      executionContext: ExecutionContext,
      cpContext: DB.CPContext = DB.NoCPContext
    ): DatabasePublisher[A] = {

      createDatabasePublisher(sql, db.name)(using
        executionContext,
        cpContext,
        db.settingsProvider
      )
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
  implicit class FromSQLToStreamSQLConverter[A, E <: WithExtractor](
    private val sql: SQL[A, E]
  ) extends AnyVal {

    def iterator()(implicit
      hasExtractor: SQL[A, E]#ThisSQL =:= SQL[A, E]#SQLWithExtractor
    ): StreamReadySQL[A] = {
      StreamReadySQL[A, E](sql, sql.fetchSize.getOrElse(DefaultFetchSize))
    }

  }

}
