package scalikejdbc.streams

import org.reactivestreams.{ Publisher, Subscriber }
import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, LogSupport, NamedDB, SQL, WithExtractor }

import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * A database backend Publisher in the context of Reactive Streams
 *
 * http://www.reactive-streams.org/
 */
class DatabasePublisher[A, E <: WithExtractor](
    val db: StreamingDatabaseConfig[A, E],
    val streamingSql: StreamingSQL[A, E],
    val emitter: StreamingEmitter[A, E]
) extends Publisher[A] with LogSupport {

  override def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
    if (subscriber eq null) {
      throw new NullPointerException("given a null Subscriber in subscribe. (see Reactive Streams spec, 1.9)")
    }
    val context = new StreamingContext[A, E](this, subscriber)
    val subscribed = try { subscriber.onSubscribe(context); true } catch {
      case NonFatal(ex) =>
        log.warn("Subscriber.onSubscribe failed unexpectedly", ex)
        false
    }
    if (subscribed) {
      try {
        context.emitter = emitter
        scheduleSynchronousStreaming(context.emitter, context, null)
        context.streamingResultPromise.future.onComplete {
          case Success(_) => context.tryOnComplete()
          case Failure(t) => context.tryOnError(t)
        }(db.executor.executionContext)
      } catch { case NonFatal(ex) => context.tryOnError(ex) }
    }
  }

  private[streams] def scheduleSynchronousStreaming(
    emitter: StreamingEmitter[A, E],
    ctx: StreamingContext[A, E],
    iterator: CloseableIterator[A]
  ): Unit = {
    try {

      val task: Runnable = new Runnable {
        private[this] def str(l: Long) = if (l != Long.MaxValue) l else "Inf"
        private[this] val debug = log.isDebugEnabled
        private[this] val _ctx: StreamingContext[A, E] = ctx
        private[this] val _iterator = iterator

        def run(): Unit = try {
          // must start with remaining iterator every time invoking this Runnable
          var remainingIterator = _iterator
          val _ = _ctx.sync

          if (remainingIterator == null) {
            borrowNewSessionAndSetAsCurrentSession(_ctx)
          }

          var demand: Long = _ctx.demandBatch
          var realDemand: Long = if (demand < 0) demand - Long.MinValue else demand

          do {
            try {
              debugLogStart(remainingIterator, realDemand)

              if (_ctx.cancelled) {
                // ------------------------
                // cancelled stream

                if (_ctx.deferredError != null) {
                  throw _ctx.deferredError
                }

                if (remainingIterator != null) { // streaming cancelled before finishing
                  val iteratorToConsume = remainingIterator
                  remainingIterator = null
                  emitter.cancel(_ctx, iteratorToConsume)
                }

              } else if (realDemand > 0 || remainingIterator == null) {
                // ------------------------
                // proceed with remaining iterator

                val iteratorToConsume = {
                  if (remainingIterator != null) remainingIterator
                  else issueQueryAndCreateNewIterator(ctx.session)
                }
                remainingIterator = null
                remainingIterator = emitter.emit(_ctx, realDemand, iteratorToConsume)
              }

              if (remainingIterator == null) { // streaming finished and cleaned up
                releaseCurrentSession(_ctx, true)
                _ctx.streamingResultPromise.trySuccess(null)
              }

            } catch {
              case NonFatal(ex) =>
                if (remainingIterator != null) {
                  try {
                    emitter.cancel(_ctx, remainingIterator)
                  } catch { case NonFatal(_) => () }
                }
                releaseCurrentSession(_ctx, true)
                throw ex

            } finally {
              _ctx.replaceCurrentIterator(remainingIterator)
              _ctx.sync = 0
            }

            debugLogSentEvent(remainingIterator, realDemand)

            demand = _ctx.delivered(demand)
            realDemand = if (demand < 0) demand - Long.MinValue else demand

          } while (remainingIterator != null && realDemand > 0)

          debugLogEnd(remainingIterator)

        } catch { case NonFatal(ex) => _ctx.streamingResultPromise.tryFailure(ex) }

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
              s"Sent up to ${str(realDemand)} elements - Stream ${if (_ctx.cancelled) "cancelled" else "completely delivered"}"
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

      db.executor.execute(task)

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

  private[this] def borrowNewSessionAndSetAsCurrentSession(context: StreamingContext[A, E]): Unit = {
    val session = NamedDB(db.name, db.settings)(db.connectionPoolContext).autoClose(false).readOnlySession()
    context.currentSession = session
  }

  private[this] def releaseCurrentSession(
    context: StreamingContext[A, E],
    discardErrors: Boolean
  ): Unit = {
    try {
      context.currentSession.close()
    } catch { case NonFatal(_) if discardErrors => }
    context.currentSession = null
  }

}
