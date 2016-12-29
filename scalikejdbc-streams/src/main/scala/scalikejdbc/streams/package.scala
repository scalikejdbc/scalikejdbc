package scalikejdbc

import scalikejdbc.GeneralizedTypeConstraintsForWithExtractor.=:=

package object streams { self =>
  final val DefaultFetchSize: Int = 1000

  def stream[A, E <: WithExtractor](
    sql: StreamingSQL[A, E],
    dbName: Any = ConnectionPool.DEFAULT_NAME
  )(
    implicit
    executor: StreamingDB.Executor,
    context: DB.CPContext = DB.NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): DatabasePublisher[A] = {
    val db = StreamingDB(dbName, executor)
    db.stream(sql)
  }

  implicit class StreamingDBConverter(val db: DB.type) extends AnyVal {

    def stream[A, E <: WithExtractor](sql: StreamingSQL[A, E])(implicit
      executor: StreamingDB.Executor,
      context: DB.CPContext = DB.NoCPContext,
      settings: SettingsProvider = SettingsProvider.default): DatabasePublisher[A] = {

      self.stream(sql)
    }
  }

  implicit class StreamingNamedDBConverter(val db: NamedDB) extends AnyVal {

    def stream[A, E <: WithExtractor](sql: StreamingSQL[A, E])(implicit
      executor: StreamingDB.Executor,
      context: DB.CPContext = DB.NoCPContext): DatabasePublisher[A] = {

      // I think that it is better to use ConnectionPoolContext of NamedDB,
      // but since it can not be referenced in this scope,
      // so I am trying to receive it with implicit parameter.
      // (In addition, since NamedDB persists one DBConnection inside statically, it is not suitable for streaming.)
      self.stream(sql, db.name)(executor, context, db.settingsProvider)
    }
  }

  implicit class StreamingSQLConverter[A, E <: WithExtractor](val sql: SQL[A, E]) extends AnyVal {
    def cursorWith(fetchSize: Int = DefaultFetchSize)(implicit hasExtractor: SQL[A, E]#ThisSQL =:= SQL[A, E]#SQLWithExtractor): StreamingSQL[A, E] = {
      new CursorStreamingSQL[A, E](sql, fetchSize)
    }

    def cursor()(implicit hasExtractor: SQL[A, E]#ThisSQL =:= SQL[A, E]#SQLWithExtractor): StreamingSQL[A, E] = cursorWith()
  }

}
