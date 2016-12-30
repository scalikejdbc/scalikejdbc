package scalikejdbc.streams

import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, WithExtractor }

import scala.util.control.NonFatal

class StreamingAction[A, E <: WithExtractor](streamingSql: StreamingSQL[A, E]) {

  private[this] def createInvoker(streamingSql: StreamingSQL[A, E]): StreamingInvoker[A, E] = new StreamingInvoker(streamingSql)

  private class StreamingInvoker[A, E <: WithExtractor](streamingSql: StreamingSQL[A, E]) {

    def results()(implicit session: DBSession): CloseableIterator[A] = {
      val streamingSession = streamingSql.setSessionAttributes(session)
      val sql = streamingSql.underlying
      val executor = streamingSession.toStatementExecutor(sql.statement, sql.rawParameters)
      val proxy = new DBConnectionAttributesWiredResultSet(executor.executeQuery(), streamingSession.connectionAttributes)
      new ResultSetExtractionIterator[A](proxy, true)(sql.extractor) {
        private[this] var closed = false

        override def close(): Unit = if (!closed) {
          executor.close()
          closed = true
        }
      }
    }
  }

  def emitStream(
    context: StreamingContext[A, E],
    limit: Long,
    iterator: CloseableIterator[A]
  ): CloseableIterator[A] = {
    val bufferNext = context.bufferNext
    val _iterator = {
      if (iterator ne null) iterator
      else createInvoker(streamingSql).results()(context.session)
    }
    var count = 0L
    try {
      while ({
        if (bufferNext) _iterator.hasNext && count < limit
        else count < limit && _iterator.hasNext
      }) {
        count += 1
        context.emit(_iterator.next())
      }
    } catch {
      case NonFatal(ex) =>
        try {
          _iterator.close()
        } catch { case NonFatal(_) => () }
        throw ex
    }
    if (if (bufferNext) _iterator.hasNext else count == limit) _iterator
    else null
  }

  def cancelStream(
    context: StreamingContext[A, E],
    iterator: CloseableIterator[A]
  ): Unit = {
    if (iterator != null) {
      iterator.close()
    }
  }

}
