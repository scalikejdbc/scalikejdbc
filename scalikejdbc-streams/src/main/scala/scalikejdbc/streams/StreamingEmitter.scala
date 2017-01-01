package scalikejdbc.streams

import scalikejdbc.WithExtractor
import scalikejdbc.streams.iterator.CloseableIterator

import scala.util.control.NonFatal

class StreamingEmitter[A, E <: WithExtractor] {

  def emit(
    subscription: DatabaseSubscription[A, E],
    limit: Long,
    iterator: CloseableIterator[A]
  ): CloseableIterator[A] = {
    val bufferNext = subscription.publisher.publisherSettings.bufferNext
    var count = 0L

    try {
      while ({
        if (bufferNext) iterator.hasNext && count < limit
        else count < limit && iterator.hasNext
      }) {
        count += 1
        subscription.emit(iterator.next())
      }
    } catch {
      case NonFatal(ex) =>
        try {
          iterator.close()
        } catch { case NonFatal(_) => () }
        throw ex
    }

    if ((bufferNext && iterator.hasNext) || count == limit) iterator
    else null
  }

  def cancel(
    context: DatabaseSubscription[A, E],
    iterator: CloseableIterator[A]
  ): Unit = {
    if (iterator != null) {
      iterator.close()
    }
  }

}
