package scalikejdbc

import scalikejdbc.GeneralizedTypeConstraintsForWithExtractor.=:=

/**
 * Reactive Streams support.
 *
 * see also [[http://www.reactive-streams.org/]]
 */
package object streams { self =>

  val DefaultFetchSize: Int = 1000

  private def readOnlyStream[A, E <: WithExtractor](
    sql: StreamSQL[A],
    dbName: Any = ConnectionPool.DEFAULT_NAME
  )(
    implicit
    asyncExecutor: AsyncExecutor,
    cpContext: DB.CPContext = DB.NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): DatabasePublisher[A] = {
    val publisherSettings = DatabasePublisherSettings[A](dbName)
    DatabasePublisherFactory.createNewPublisher[A](publisherSettings, asyncExecutor, sql)
  }

  /**
   * An implicit to enable the `DB.stream` method:
   *
   * {{{
   * val publisher = DB.readOnlyStream { implicit session =>
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class EnableDBCodeBlockToProvideDatabasePublisher(val db: DB.type) extends AnyVal {

    def readOnlyStream[A](sql: StreamSQL[A])(implicit
      asyncExecutor: AsyncExecutor,
      cpContext: DB.CPContext = DB.NoCPContext,
      settings: SettingsProvider = SettingsProvider.default): DatabasePublisher[A] = {

      self.readOnlyStream(sql)
    }
  }

  /**
   * An implicit to enable the `NamedDB('name).stream` method:
   *
   * {{{
   * val publisher = NamedDB('name).readOnlyStream { implicit session =>
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class EnableNamedDBCodeBlockToProvideDatabasePublisher(val db: NamedDB) extends AnyVal {

    def readOnlyStream[A, E <: WithExtractor](sql: StreamSQL[A])(implicit
      asyncExecutor: AsyncExecutor,
      cpContext: DB.CPContext = DB.NoCPContext): DatabasePublisher[A] = {

      // I think that it is better to use ConnectionPoolContext of NamedDB,
      // but since it can not be referenced in this scope,
      // so I am trying to receive it with implicit parameter.
      // (In addition, since NamedDB persists one DBConnection inside statically, it is not suitable for streaming.)
      self.readOnlyStream(sql, db.name)(asyncExecutor, cpContext, db.settingsProvider)
    }
  }

  /**
   * An implicit to enable the `iterator` method:
   *
   * {{{
   * val publisher = DB.readOnlyStream { implicit session =>
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit class FromSQLToStreamSQLConverter[A, E <: WithExtractor](val sql: SQL[A, E]) extends AnyVal {

    def iterator()(
      implicit
      hasExtractor: SQL[A, E]#ThisSQL =:= SQL[A, E]#SQLWithExtractor
    ): StreamSQL[A] = {
      StreamSQL[A, E](sql, sql.fetchSize.getOrElse(DefaultFetchSize))
    }

  }

}
