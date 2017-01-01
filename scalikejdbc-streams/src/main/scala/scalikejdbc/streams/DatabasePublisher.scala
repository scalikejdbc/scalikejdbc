package scalikejdbc.streams

import org.reactivestreams.{ Publisher, Subscriber }
import scalikejdbc.streams.iterator.{ CloseableIterator, StreamingIterator }
import scalikejdbc.streams.sql.StreamingSQL
import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, LogSupport, NamedDB, SQL, WithExtractor }

import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * A database backend Publisher in the context of Reactive Streams
 *
 * http://www.reactive-streams.org/
 */
class DatabasePublisher[A, E <: WithExtractor](
    val publisherSettings: DatabasePublisherSettings[A, E],
    val streamingSql: StreamingSQL[A, E],
    val emitter: StreamingEmitter[A, E]
) extends Publisher[A] with LogSupport {

  override def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    if (subscriber == null) {
      throw new NullPointerException("given a null Subscriber in subscribe. (see Reactive Streams spec, 1.9)")
    }
    val subscription: DatabaseSubscription[A, E] = new DatabaseSubscription[A, E](this, subscriber)
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
        subscription.emitter = emitter
        scheduleSynchronousStreaming(subscription.emitter, subscription, null)
        subscription.streamingResultPromise.future.onComplete {
          case Success(_) => subscription.tryOnComplete()
          case Failure(t) => subscription.tryOnError(t)
        }(publisherSettings.executor.executionContext)
      } catch { case NonFatal(ex) => subscription.tryOnError(ex) }
    }
  }

  private[streams] def scheduleSynchronousStreaming(
    emitter: StreamingEmitter[A, E],
    subscription: DatabaseSubscription[A, E],
    iterator: CloseableIterator[A]
  ): Unit = {
    try {

      val task: Runnable = new Runnable {
        private[this] def str(l: Long) = if (l != Long.MaxValue) l else "Inf"
        private[this] val debug = log.isDebugEnabled
        private[this] val _subscription: DatabaseSubscription[A, E] = subscription
        private[this] val _iterator = iterator

        def run(): Unit = try {
          // must start with remaining iterator every time invoking this Runnable
          var remainingIterator = _iterator
          val _ = _subscription.sync

          if (remainingIterator == null) {
            borrowNewSessionAndSetAsCurrentSession(_subscription)
          }

          var demand: Long = _subscription.demandBatch
          var realDemand: Long = if (demand < 0) demand - Long.MinValue else demand

          do {
            try {
              debugLogStart(remainingIterator, realDemand)

              if (_subscription.cancelled) {
                // ------------------------
                // cancelled stream

                if (_subscription.deferredError != null) {
                  throw _subscription.deferredError
                }

                if (remainingIterator != null) { // streaming cancelled before finishing
                  val iteratorToConsume = remainingIterator
                  remainingIterator = null
                  emitter.cancel(_subscription, iteratorToConsume)
                }

              } else if (realDemand > 0 || remainingIterator == null) {
                // ------------------------
                // proceed with remaining iterator

                val iteratorToConsume = {
                  if (remainingIterator != null) remainingIterator
                  else issueQueryAndCreateNewIterator(subscription.session)
                }
                remainingIterator = null
                remainingIterator = emitter.emit(_subscription, realDemand, iteratorToConsume)
              }

              if (remainingIterator == null) { // streaming finished and cleaned up
                releaseCurrentSession(_subscription, true)
                _subscription.streamingResultPromise.trySuccess(null)
              }

            } catch {
              case NonFatal(ex) =>
                if (remainingIterator != null) {
                  try {
                    emitter.cancel(_subscription, remainingIterator)
                  } catch { case NonFatal(_) => () }
                }
                releaseCurrentSession(_subscription, true)
                throw ex

            } finally {
              _subscription.replaceCurrentIterator(remainingIterator)
              _subscription.sync = 0
            }

            debugLogSentEvent(remainingIterator, realDemand)

            demand = _subscription.delivered(demand)
            realDemand = if (demand < 0) demand - Long.MinValue else demand

          } while (remainingIterator != null && realDemand > 0)

          debugLogEnd(remainingIterator)

        } catch { case NonFatal(ex) => _subscription.streamingResultPromise.tryFailure(ex) }

        // ------------------------------
        // debug logging

        private[this] def debugLogStart(currentIterator: CloseableIterator[A], realDemand: Long): Unit = {
          if (debug) {
            log.debug((if (currentIterator != null) "Starting initial" else "Restarting") + " streaming action, realDemand = " + str(realDemand))
          }
        }

        private[this] def debugLogSentEvent(currentIterator: CloseableIterator[A], realDemand: Long): Unit = {
          if (debug) {
            val message = if (currentIterator == null) {
              s"Sent up to ${str(realDemand)} elements - Stream ${if (_subscription.cancelled) "cancelled" else "completely delivered"}"
            } else {
              s"Sent ${str(realDemand)} elements, more available - Performing atomic state transition"
            }
            log.debug(message)
          }
        }

        private[this] def debugLogEnd(currentIterator: CloseableIterator[A]): Unit = {
          if (debug) {
            if (currentIterator != null) {
              log.debug("Suspending streaming action with continuation (more data available)")
            } else {
              log.debug("Finished streaming action")
            }
          }
        }
      }

      publisherSettings.executor.execute(task)

    } catch {
      case NonFatal(ex) =>
        log.warn("Error scheduling synchronous streaming", ex)
        throw ex
    }
  }

  private[this] def issueQueryAndCreateNewIterator(session: DBSession): CloseableIterator[A] = {
    new StreamingInvoker(streamingSql).issueQueryAndCreateNewIterator(session)
  }

  private class StreamingInvoker[A, E <: WithExtractor](streamingSql: StreamingSQL[A, E]) {

    def issueQueryAndCreateNewIterator(originalSession: DBSession): CloseableIterator[A] = {
      val session = streamingSql.updateDBSessionWithSQLAttributes(originalSession)
      val sql: SQL[A, E] = streamingSql.underlying
      val statementExecutor = session.toStatementExecutor(sql.statement, sql.rawParameters)
      val resultSetProxy = new DBConnectionAttributesWiredResultSet(statementExecutor.executeQuery(), session.connectionAttributes)

      new StreamingIterator[A](resultSetProxy, true)(sql.extractor) {
        private[this] var closed = false
        override def close(): Unit = {
          if (!closed) {
            statementExecutor.close()
            closed = true
          }
        }
      }
    }
  }

  private[this] def borrowNewSessionAndSetAsCurrentSession(context: DatabaseSubscription[A, E]): Unit = {
    val session: DBSession = {
      val db = NamedDB(publisherSettings.name, publisherSettings.settings)(publisherSettings.connectionPoolContext)
      db.autoClose(false).readOnlySession()
    }
    context.currentSession = session
  }

  private[this] def releaseCurrentSession(
    context: DatabaseSubscription[A, E],
    discardErrors: Boolean
  ): Unit = {
    try {
      context.currentSession.close()
    } catch { case NonFatal(_) if discardErrors => }
    context.currentSession = null
  }

}
