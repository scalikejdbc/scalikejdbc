package scalikejdbc.streams

import scalikejdbc.WithExtractor

import scala.util.control.NonFatal

abstract class StreamingAction[A, E <: WithExtractor] {
  type State = CloseableIterator[A]

  protected[this] def createInvoker(): StreamingInvoker[A, E]

  def emitStream(context: StreamingDB#StreamingActionContext, limit: Long, state: State): State = {
    val bufferNext = context.bufferNext
    val iterator = if (state ne null) state else createInvoker().results()(context.session)
    var count = 0L
    try {
      while (if (bufferNext) iterator.hasNext && count < limit else count < limit && iterator.hasNext) {
        count += 1
        context.emit(iterator.next())
      }
    } catch {
      case NonFatal(ex) =>
        try iterator.close() catch { case NonFatal(_) => () }
        throw ex
    }
    if (if (bufferNext) iterator.hasNext else count == limit) iterator else null
  }

  def cancelStream(context: StreamingDB#StreamingActionContext, state: State): Unit = {
    if (state ne null) {
      state.close()
    }
  }
}
