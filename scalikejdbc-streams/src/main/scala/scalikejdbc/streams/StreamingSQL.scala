package scalikejdbc.streams

import scalikejdbc.{ DBSession, SQL, WithExtractor, WrappedResultSet }

abstract class StreamingSQL[A, E <: WithExtractor](
    val underlying: SQL[A, E]
) {

  lazy val statement: String = underlying.statement

  lazy val parameters: Seq[Any] = underlying.parameters

  def extractor: WrappedResultSet => A

  private[streams] def updateDBSessionWithSQLAttributes(session: DBSession): DBSession = {
    session
      .fetchSize(underlying.fetchSize)
      .tags(underlying.tags: _*)
      .queryTimeout(underlying.queryTimeout)
  }

}