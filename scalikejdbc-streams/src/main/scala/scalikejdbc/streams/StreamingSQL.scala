package scalikejdbc.streams

import scalikejdbc.{ DBSession, SQL, WithExtractor, WrappedResultSet }

abstract class StreamingSQL[A, E <: WithExtractor](val underlying: SQL[A, E]) {

  def statement: String

  def parameters: Seq[Any]

  def extractor: WrappedResultSet => A

  protected[streams] def setSessionAttributes(session: DBSession): DBSession = {
    session
      .fetchSize(underlying.fetchSize)
      .tags(underlying.tags: _*)
      .queryTimeout(underlying.queryTimeout)
  }
}

