package scalikejdbc

import scalikejdbc.GeneralizedTypeConstraintsForWithExtractor.=:=

import scala.language.implicitConversions

/**
 * Reactive Streams support.
 *
 * see also [[http://www.reactive-streams.org/]]
 */
package object streams {

  val DefaultFetchSize: Int = 1000

  /**
   * An implicit to enable the `DB.readOnlyStream` method:
   *
   * {{{
   * val publisher = DB.readOnlyStream {
   *   sql"select id from users".map(_.long("id")).iterator
   * }
   * }}}
   */
  implicit final def enableDBCodeBlockToProvideDatabasePublisher(db: DB.type)(implicit settings: SettingsProvider = SettingsProvider.default): DatabasePublisherProvider = {
    new DatabasePublisherProvider(settings = settings)
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
  implicit final def enableNamedDBCodeBlockToProvideDatabasePublisher(db: NamedDB): DatabasePublisherProvider = {
    new DatabasePublisherProvider(connectionPoolName = db.name, settings = db.settingsProvider)
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
