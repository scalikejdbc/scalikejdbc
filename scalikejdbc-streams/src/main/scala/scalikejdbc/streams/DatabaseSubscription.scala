package scalikejdbc.streams

import java.util.concurrent.atomic.AtomicLong

import org.reactivestreams.{ Subscriber, Subscription }
import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, LogSupport, NamedDB }

import scala.concurrent.Promise
import scala.util.control.NonFatal

/**
 * A subscription of DatabasePublisher
 */
private[streams] class DatabaseSubscription[A](
    val publisher: DatabasePublisher[A],
    val subscriber: Subscriber[_ >: A]
) extends Subscription with LogSupport {

  // -----------------------------------------------
  // Internal state
  // -----------------------------------------------

  private lazy val sql = publisher.sql

  /**
   * A volatile variable to enforce the happens-before relationship when executing something in a synchronous action context.
   *
   * - [[https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html]]
   * - [[http://gee.cs.oswego.edu/dl/jmm/cookbook.html]]
   *
   * It is read when entering the context and written when leaving it
   * so that all writes to non-volatile variables within the context are visible to the next synchronous execution.
   */
  @volatile
  private var sync: Int = 0

  /**
   * A database session occupied by current subscription.
   */
  private[this] var _occupiedDBSession: DBSession = null

  /**
   * The state for a suspended streaming action.
   * Must be set only from a synchronous action context.
   */
  private var currentIterator: StreamResultSetIterator[A] = null

  /**
   * The total number of elements requested and not yet marked as delivered by the synchronous streaming action.
   * Whenever this value drops to 0, streaming is suspended.
   * When it is raised up from 0 in `request`, streaming is scheduled to be restarted.
   * It is initially set to `Long.MinValue` when streaming starts.
   * Any negative value above `Long.MinValue` indicates the actual demand at that point.
   * It is reset to 0 when the initial streaming ends.
   */
  private[this] val _numberOfRemainingElements = new AtomicLong(Long.MinValue)

  /**
   * Returns true if it has been cancelled by the Subscriber.
   */
  @volatile
  private[this] var _cancelRequested = false

  /**
   * Whether the Subscriber has been signaled with `onComplete` or `onError`.
   */
  private[this] var _isFinished = false

  /**
   * An error that will be signaled to the Subscriber when the stream is cancelled or terminated.
   * This is used for signaling demand overflow in `request()`
   * while guaranteeing that the `onError` message does not overlap with an active `onNext` call.
   */
  private[this] var _deferredError: Throwable = null

  // -----------------------------------------------
  // Reactive Streams Subscription APIs
  // -----------------------------------------------

  override def request(l: Long): Unit = {
    if (!_cancelRequested) {
      if (l <= 0) {
        _deferredError = new IllegalArgumentException("Requested count must not be <= 0 (see Reactive Streams spec, 3.9)")
        cancel()
      } else {
        // If a large number of requests are signaled, it will continue to receive the request signal (even though it might not stabilize),
        // even if it goes beyond Long.MaxValue. Because the record in the database is finite and streaming ends when it reaches its end.
        // Also, if there is a request of Long.MaxValue + Long.MaxValue + 2 at the time of continuity check of the (first) streaming loop,
        // it will be reset to Long.MinValue so there means no request, but the next request It will be signaled soon, so it would not be a serious problem.
        // ReactiveStreams Spec 3.17 has "effectively unbounded"
        // -> As it is not feasibly reachable with current or foreseen hardware within
        //    a reasonable amount of time (1 element per nanosecond would take 292 years) to fulfill a demand of 2^63-1,
        //    it is allowed for a Publisher to stop tracking demand beyond this point.
        if (!_cancelRequested && _numberOfRemainingElements.getAndAdd(l) == 0L) restartStreaming()
      }
    }
  }

  override def cancel(): Unit = if (!_cancelRequested) {
    _cancelRequested = true
    // Restart streaming because cancelling requires closing the result set and the session from within a synchronous action context.
    // This will also complete the result Promise and thus allow the rest of the scheduled Action to run.
    if (_numberOfRemainingElements.getAndSet(Long.MaxValue) == 0L) {
      restartStreaming()
    }
  }

  // -----------------------------------------------
  // scalikejdbc-streams internal APIs
  // -----------------------------------------------

  /**
   * Emits single value to the subscriber.
   */
  private[streams] def emit(v: A): Unit = subscriber.onNext(v)

  /**
   * The Promise to complete when streaming has finished.
   */
  private[streams] val streamingResultPromise: Promise[Null] = Promise[Null]()

  /**
   * Finishes the stream with `onComplete` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  private[streams] def tryOnComplete(): Unit = {
    if (!_isFinished && !_cancelRequested) {
      if (log.isDebugEnabled) log.debug("Signaling onComplete()")
      _isFinished = true
      try subscriber.onComplete() catch {
        case NonFatal(ex) => log.warn("Subscriber.onComplete failed unexpectedly", ex)
      }
    }
  }

  /**
   * Finish the stream with `onError` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  private[streams] def tryOnError(t: Throwable): Unit = if (!_isFinished) {
    if (log.isDebugEnabled) log.debug(s"Signaling onError($t)")
    _isFinished = true
    try subscriber.onError(t) catch {
      case NonFatal(ex) => log.warn("Subscriber.onError failed unexpectedly", ex)
    }
  }

  /**
   * Restart a suspended streaming action.
   * Must only be called from the Subscriber context.
   */
  private[this] def restartStreaming(): Unit = {
    val _ = sync
    val iteratorToConsume = currentIterator
    if (iteratorToConsume != null) {
      currentIterator = null
      if (log.isDebugEnabled) {
        log.debug("Scheduling stream continuation after transition from demand = 0")
      }

      scheduleSynchronousStreaming(iteratorToConsume)

    } else {
      if (log.isDebugEnabled) {
        log.debug("Saw transition from demand = 0, but no stream continuation available")
      }
    }
  }

  private[streams] def scheduleSynchronousStreaming(iterator: StreamResultSetIterator[A]): Unit = {

    val currentSubscription = this

    try {
      val task: Runnable = new Runnable {
        private[this] def str(l: Long) = if (l != Long.MaxValue) l else "Inf"
        private[this] val debug = log.isDebugEnabled
        private[this] val _iterator = iterator

        def run(): Unit = try {
          // must start with remaining iterator every time invoking this Runnable
          var remainingIterator = _iterator
          val _ = currentSubscription.sync

          if (remainingIterator == null) {
            currentSubscription.borrowNewDBSession()
          }

          var demand: Long = currentSubscription.demandBatch
          var realDemand: Long = if (demand < 0) demand - Long.MinValue else demand

          do {
            try {
              debugLogStart(remainingIterator, realDemand)

              if (currentSubscription.cancelled) {
                // ------------------------
                // cancelled stream

                if (currentSubscription.deferredError != null) {
                  throw currentSubscription.deferredError
                }

                if (remainingIterator != null) { // streaming cancelled before finishing
                  val iteratorToConsume = remainingIterator
                  remainingIterator = null
                  publisher.streamEmitter.cancel(currentSubscription, iteratorToConsume)
                }

              } else if (realDemand > 0 || remainingIterator == null) {
                // ------------------------
                // proceed with remaining iterator

                val iteratorToConsume: StreamResultSetIterator[A] = {
                  if (remainingIterator != null) remainingIterator
                  else currentSubscription.issueQueryAndCreateNewIterator()
                }
                remainingIterator = null
                remainingIterator = publisher.streamEmitter.emit(currentSubscription, realDemand, iteratorToConsume)
              }

              if (remainingIterator == null) { // streaming finished and cleaned up
                currentSubscription.releaseOccupiedDBSession(true)
                currentSubscription.streamingResultPromise.trySuccess(null)
              }

            } catch {
              case NonFatal(ex) =>
                if (remainingIterator != null) {
                  try {
                    publisher.streamEmitter.cancel(currentSubscription, remainingIterator)
                  } catch { case NonFatal(_) => () }
                }
                currentSubscription.releaseOccupiedDBSession(true)
                throw ex

            } finally {
              currentSubscription.currentIterator = remainingIterator
              currentSubscription.sync = 0
            }

            debugLogSentEvent(remainingIterator, realDemand)

            demand = currentSubscription.saveNumberOfDeliveredElementsAndReturnRemainingDemand(demand)
            realDemand = if (demand < 0) demand - Long.MinValue else demand

          } while (remainingIterator != null && realDemand > 0)

          debugLogEnd(remainingIterator)

        } catch { case NonFatal(ex) => currentSubscription.streamingResultPromise.tryFailure(ex) }

        // ------------------------------
        // debug logging

        private[this] def debugLogStart(currentIterator: StreamResultSetIterator[A], realDemand: Long): Unit = {
          if (debug) {
            log.debug((if (currentIterator != null) "Starting initial" else "Restarting") + " streaming action, realDemand = " + str(realDemand))
          }
        }

        private[this] def debugLogSentEvent(currentIterator: StreamResultSetIterator[A], realDemand: Long): Unit = {
          if (debug) {
            val message = if (currentIterator == null) {
              s"Sent up to ${str(realDemand)} elements - Stream ${if (currentSubscription.cancelled) "cancelled" else "completely delivered"}"
            } else {
              s"Sent ${str(realDemand)} elements, more available - Performing atomic state transition"
            }
            log.debug(message)
          }
        }

        private[this] def debugLogEnd(currentIterator: StreamResultSetIterator[A]): Unit = {
          if (debug) {
            if (currentIterator != null) {
              log.debug("Suspending streaming action with continuation (more data available)")
            } else {
              log.debug("Finished streaming action")
            }
          }
        }
      }

      publisher.asyncExecutor.execute(task)

    } catch {
      case NonFatal(ex) =>
        log.warn("Error scheduling synchronous streaming", ex)
        throw ex
    }
  }

  // -----------------------------------------------
  // Internal APIs visible to only threads current subscription started
  // -----------------------------------------------

  /**
   * Indicate that the specified number of elements has been delivered.
   *
   * Returns the remaining demand.
   *
   * This is an atomic operation.
   * It must only be called from the synchronous action context which performs the streaming.
   */
  private def saveNumberOfDeliveredElementsAndReturnRemainingDemand(num: Long): Long = {
    _numberOfRemainingElements.addAndGet(-num)
  }

  /**
   * Get the current demand that has not yet been marked as delivered and mark it as being in the current batch.
   * When this value is negative, the initial streaming action is still running
   * and the real demand can be computed by subtracting `Long.MinValue` from the returned value.
   */
  private def demandBatch: Long = _numberOfRemainingElements.get()

  /**
   * Returns the deferred error of current subscription if exists.
   */
  private def deferredError: Throwable = _deferredError

  /**
   * Returns the DBSession occupied by current subscription.
   */
  private def occupiedDBSession: DBSession = _occupiedDBSession

  /**
   * Whether the stream has been cancelled by the Subscriber
   */
  private def cancelled: Boolean = _cancelRequested

  private def issueQueryAndCreateNewIterator(): StreamResultSetIterator[A] = {
    makeDBSessionCursorQueryReady()

    val statementExecutor = occupiedDBSession.toStatementExecutor(sql.statement, sql.rawParameters)
    val resultSetProxy = new DBConnectionAttributesWiredResultSet(
      statementExecutor.executeQuery(),
      occupiedDBSession.connectionAttributes
    )

    new StreamResultSetIterator[A](resultSetProxy, true)(sql.extractor) {
      private[this] var closed = false
      override def close(): Unit = {
        if (!closed) {
          statementExecutor.close()
          closed = true
        }
      }
    }
  }

  private def borrowNewDBSession(): Unit = {
    if (_occupiedDBSession != null) {
      releaseOccupiedDBSession(true)
    }
    val session: DBSession = {
      val settings: DatabasePublisherSettings[A] = publisher.settings
      val db: NamedDB = NamedDB(settings.connectionPoolName, settings.settingsProvider)(settings.connectionPoolContext)
      db.autoClose(false).readOnlySession()
    }
    _occupiedDBSession = session
  }

  private def releaseOccupiedDBSession(discardErrors: Boolean): Unit = {
    try {
      _occupiedDBSession.close()
    } catch { case NonFatal(_) if discardErrors => }
    _occupiedDBSession = null
  }

  // -----------------------------------------------
  // Completely internal methods
  // -----------------------------------------------

  private[this] def makeDBSessionCursorQueryReady(): Unit = {
    occupiedDBSession
      .fetchSize(sql.fetchSize)
      .tags(sql.tags: _*)
      .queryTimeout(sql.queryTimeout)

    // setup required settings to enable cursor operations
    occupiedDBSession.connectionAttributes.driverName match {
      case Some(driver) if driver == "com.mysql.jdbc.Driver" && sql.fetchSize.exists(_ > 0) =>
        /*
         * MySQL - https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-implementation-notes.html
         *
         * StreamAction.StreamingInvoker prepares the following required settings in advance:
         *
         * - java.sql.ResultSet.TYPE_FORWARD_ONLY
         * - java.sql.ResultSet.CONCUR_READ_ONLY
         *
         * If the fetchSize is set as 0 or less, we need to forcibly change the value with the Int min value.
         */
        occupiedDBSession.fetchSize(Int.MinValue)

      case Some(driver) if driver == "org.postgresql.Driver" =>
        /*
         * PostgreSQL - https://jdbc.postgresql.org/documentation/94/query.html
         *
         * - java.sql.Connection#autocommit false
         * - java.sql.ResultSet.TYPE_FORWARD_ONLY
         */
        occupiedDBSession.conn.setAutoCommit(false)

      case _ =>
    }
  }

}
