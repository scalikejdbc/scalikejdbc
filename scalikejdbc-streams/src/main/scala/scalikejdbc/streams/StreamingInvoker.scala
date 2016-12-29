package scalikejdbc.streams

import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, WithExtractor }

abstract class StreamingInvoker[A, E <: WithExtractor] {
  protected[this] def streamingSql: StreamingSQL[A, E]

  def results()(implicit session: DBSession): CloseableIterator[A] = {
    val streamingSession = streamingSql.setSessionAttributes(session)
    val sql = streamingSql.underlying
    val executor = streamingSession.toStatementExecutor(sql.statement, sql.rawParameters)
    val proxy = new DBConnectionAttributesWiredResultSet(executor.executeQuery(), streamingSession.connectionAttributes)
    new ExtractedResultIterator[A](proxy, true)(sql.extractor) {
      private[this] var closed = false

      override def close(): Unit = if (!closed) {
        executor.close()
        closed = true
      }
    }
  }
}
