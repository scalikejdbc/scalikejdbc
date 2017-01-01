package scalikejdbc.streams

import org.reactivestreams.{ Publisher, Subscriber }
import scalikejdbc.LogSupport

import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * A database backend Publisher in the context of Reactive Streams
 *
 * see also: [[http://www.reactive-streams.org/]]
 */
case class DatabasePublisher[A](
    settings: DatabasePublisherSettings[A],
    sql: StreamSQL[A],
    asyncExecutor: AsyncExecutor,
    streamEmitter: StreamEmitter[A]
) extends Publisher[A] with LogSupport {

  override def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    if (subscriber == null) {
      throw new NullPointerException("given a null Subscriber in DatabasePublisher#subscribe. (see Reactive Streams spec, 1.9)")
    }
    val subscription: DatabaseSubscription[A] = new DatabaseSubscription[A](this, subscriber)
    val subscribed: Boolean = {
      try {
        subscriber.onSubscribe(subscription)
        true
      } catch {
        case NonFatal(ex) =>
          log.warn("Subscriber#onSubscribe failed unexpectedly", ex)
          false
      }
    }
    if (subscribed) {
      try {
        subscription.scheduleSynchronousStreaming(null)
        subscription.streamingResultPromise.future.onComplete {
          case Success(_) => subscription.tryOnComplete()
          case Failure(t) => subscription.tryOnError(t)
        }(asyncExecutor.executionContext)
      } catch { case NonFatal(ex) => subscription.tryOnError(ex) }
    }
  }

}
