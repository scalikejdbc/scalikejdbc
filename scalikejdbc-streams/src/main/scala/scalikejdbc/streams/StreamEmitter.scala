package scalikejdbc.streams

import scala.util.control.NonFatal

/**
 * Database stream emitter.
 */
class StreamEmitter[A] {

  def emit(
    subscription: DatabaseSubscription[A],
    limit: Long,
    iterator: StreamResultSetIterator[A]
  ): StreamResultSetIterator[A] = {
    val bufferNext = subscription.publisher.settings.bufferNext
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

    if ((bufferNext && iterator.hasNext) || (!bufferNext && count == limit)) iterator
    else null
  }

  def cancel(
    subscription: DatabaseSubscription[A], // FIXME: unused
    iterator: StreamResultSetIterator[A]
  ): Unit = {
    if (iterator != null) {
      iterator.close()
    }
  }

}
