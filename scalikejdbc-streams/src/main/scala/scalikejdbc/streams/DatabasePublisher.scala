package scalikejdbc.streams

import org.reactivestreams.{ Publisher, Subscriber }
import scalikejdbc.LogSupport

import scala.util.control.NonFatal

/**
 * A database backend Publisher in the fashion of Reactive Streams
 *
 * see also: [[https://www.reactive-streams.org/]]
 */
class DatabasePublisher[A] private[streams] (
  private[streams] val settings: DatabasePublisherSettings[A],
  private[streams] val sql: StreamReadySQL[A],
  private[streams] val asyncExecutor: AsyncExecutor
) extends Publisher[A]
  with LogSupport {

  /**
   * Requests Publisher to start streaming data.
   */
  override def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    // 1. Publisher - 9
    // https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#1-publisher-code
    //
    // Publisher.subscribe MUST call onSubscribe on the provided Subscriber prior to any other signals to that Subscriber
    // and MUST return normally, except when the provided Subscriber is null
    // in which case it MUST throw a java.lang.NullPointerException to the caller,
    // for all other situations the only legal way to signal failure (or reject the Subscriber)
    // is by calling onError (after calling onSubscribe).
    //
    if (subscriber == null) {
      // 1-9: Publisher.subscribe MUST return normally, except when the provided Subscriber is null
      // in which case it MUST throw a java.lang.NullPointerException to the caller,
      // for all other situations the only legal way to signal failure (or reject the Subscriber)
      throw new NullPointerException(
        "given Subscriber to DatabasePublisher#subscribe is null. (Reactive Streams spec, 1.9)"
      )
    }

    try {
      val subscription: DatabaseSubscription[A] =
        new DatabaseSubscription[A](this, subscriber)
      try {
        try {
          // 1-9: Publisher.subscribe MUST call onSubscribe on the provided Subscriber prior to any other signals to that Subscriber
          subscriber.onSubscribe(subscription)
        } catch {
          case NonFatal(e) =>
            log.warn(
              s"Subscriber#onSubscribe for subscriber: ${subscriber} unexpectedly failed because ${e.getMessage}",
              e
            )
            // for all other situations the only legal way to signal failure (or reject the Subscriber)
            // is by calling onError (after calling onSubscribe).
            subscription.onError(e)
            return
        }

        subscription.startNewStreaming()
        subscription.prepareCompletionHandler()

        log.info(
          s"Database stream requested by subscriber: ${subscriber} is ready"
        )

      } catch {
        case NonFatal(e) =>
          log.warn(
            s"Failed to make preparation for subscriber: ${subscriber}",
            e
          )
          // 1-9: for all other situations the only legal way to signal failure (or reject the Subscriber)
          // is by calling onError (after calling onSubscribe).
          subscription.onError(e)
      }
    } catch {
      case NonFatal(e) =>
        // 1-9: Publisher.subscribe MUST return normally, except when the provided Subscriber is null
        if (log.isDebugEnabled) {
          log.debug(
            s"Ignore exceptions for subscriber: ${subscriber} to obey Reactive Streams spec 1-9",
            e
          )
        }
    }
  }

}
