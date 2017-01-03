package scalikejdbc.streams

import java.util.concurrent.atomic.AtomicLong

import org.reactivestreams.{ Subscriber, Subscription }
import scalikejdbc.{ DBConnectionAttributesWiredResultSet, DBSession, LogSupport, NamedDB }

import scala.concurrent.Promise
import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * A subscription of DatabasePublisher
 */
private[streams] class DatabaseSubscription[A](
    /**
     * DatabasePublisher in the fashion of Reactive Streams.
     */
    val publisher: DatabasePublisher[A],

    /**
     * Subscriber in the fashion of Reactive Streams.
     */
    val subscriber: Subscriber[_ >: A]
) extends Subscription with LogSupport {

  // -----------------------------------------------
  // Internal state
  // -----------------------------------------------

  /**
   * Stream ready SQL object.
   */
  private lazy val sql: StreamReadySQL[A] = publisher.sql

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
   * The Promise to complete when streaming has finished.
   */
  private val endOfStream: Promise[Unit] = Promise[Unit]()

  /**
   * The total number of elements requested and not yet marked as delivered by the synchronous streaming action.
   *
   * Whenever the value drops to 0, streaming is suspended.
   * The value is initially set to `Long.MinValue` when the streaming starts.
   * When the value is raised up from 0 in #request(Long), the streaming is scheduled to be restarted.
   * Any negative value of more than `Long.MinValue` indicates the actual demand at that point.
   * It is reset to 0 when the initial streaming ends.
   */
  private[this] val _numberOfRemainingElements: AtomicLong = new AtomicLong(Long.MinValue)

  /**
   * Returns true if it has been cancelled by the Subscriber.
   */
  @volatile
  private[this] var _cancelRequested: Boolean = false

  /**
   * Whether the Subscriber has been signaled with `onComplete` or `onError`.
   */
  private[this] var _isFinished: Boolean = false

  /**
   * An error that will be signaled to the Subscriber when the stream is cancelled or terminated.
   *
   * This is used for signaling demand overflow in #request(Long)
   * while guaranteeing that the #onError(Throwable) message does not overlap with an active #onNext(A) call.
   */
  private[this] var _deferredError: Throwable = null

  // -----------------------------------------------
  // Reactive Streams Subscription APIs
  // -----------------------------------------------

  /**
   * No events will be sent by a Publisher until demand is signaled via this method.
   */
  override def request(n: Long): Unit = {
    if (_cancelRequested) {
      if (log.isDebugEnabled) {
        log.debug(s"Subscription#request($n) called from subscriber: ${subscriber} after cancellation, skipped processing")
      }

    } else {
      if (log.isDebugEnabled) {
        log.debug(s"Subscription#request($n) called from subscriber: ${subscriber}")
      }

      if (n <= 0) {
        // 3. Subscription - 9
        // see: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code
        //
        // While the Subscription is not cancelled, Subscription.request(long n)
        // MUST signal onError with a java.lang.IllegalArgumentException if the argument is <= 0.
        // The cause message MUST include a reference to this rule and/or quote the full rule.
        //
        _deferredError = new IllegalArgumentException("The n of Subscription#request(long n) must not be larger than 0 (Reactive Streams spec, 3.9)")

        cancel()

      } else {
        // 3. Subscription - 17
        // see: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code
        //
        // A Subscription MUST support an unbounded number of calls to request
        // and MUST support a demand (sum requested - sum delivered) up to 2^63-1 (java.lang.Long.MAX_VALUE).
        // A demand equal or greater than 2^63-1 (java.lang.Long.MAX_VALUE) MAY be considered by the Publisher as “effectively unbounded”[3].
        //
        if (_cancelRequested == false && _numberOfRemainingElements.getAndAdd(n) == 0L) {
          reScheduleSynchronousStreaming()
        }
      }
    }
  }

  /**
   * Requests the Publisher to stop sending data and clean up resources.
   */
  override def cancel(): Unit = if (!_cancelRequested) {
    if (cancelled == false) {
      log.info(s"Subscription#cancel() called from subscriber: ${subscriber}")

      _cancelRequested = true

      // restart the streaming here because cancelling it requires closing the occupied database session.
      // This will also complete the result Promise and thus allow the rest of the scheduled Action to run.
      if (_numberOfRemainingElements.getAndSet(Long.MaxValue) == 0L) {
        reScheduleSynchronousStreaming()
      }

    } else {
      log.info(s"Subscription#cancel() called from subscriber: ${subscriber} again, skipped processing")
    }
  }

  // -----------------------------------------------
  // scalikejdbc-streams internal APIs
  // -----------------------------------------------

  /**
   * Prepares the completion handler of the current streaming process.
   */
  private[streams] def prepareCompletionHandler(): Unit = {
    implicit val ec = publisher.asyncExecutor.executionContext
    endOfStream.future.onComplete {
      case Success(_) => onComplete()
      case Failure(t) => onError(t)
    }
  }

  /**
   * Finishes the streaming when some error happens.
   */
  private[streams] def onError(t: Throwable): Unit = {
    if (!_isFinished) {
      if (log.isDebugEnabled) {
        log.debug(s"Subscriber#onError for subscriber: ${subscriber} called with exception: $t")
      }
      _isFinished = true

      try {
        subscriber.onError(t)
      } catch {
        case NonFatal(e) =>
          log.warn(s"Subscriber#onError for subscriber: ${subscriber} unexpectedly failed because ${e.getMessage}", e)
      }
    }
  }

  /**
   * Starts new streaming.
   */
  private[streams] def startNewStreaming(): Unit = {
    scheduleSynchronousStreaming(null)
  }

  // -----------------------------------------------
  // Internal APIs
  // visible to only threads current subscription started
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

  /**
   * Issues a query and creates a new iterator to consume.
   */
  private def issueQueryAndCreateNewIterator(): StreamResultSetIterator[A] = {
    makeDBSessionCursorQueryReady()

    val statementExecutor = occupiedDBSession.toStatementExecutor(sql.statement, sql.rawParameters)
    val resultSet = statementExecutor.executeQuery()
    val resultSetProxy = new DBConnectionAttributesWiredResultSet(resultSet, occupiedDBSession.connectionAttributes)

    new StreamResultSetIterator[A](resultSetProxy, sql.extractor) {
      private[this] var closed = false
      override def close(): Unit = {
        if (!closed) {
          statementExecutor.close()
          closed = true
        }
      }
    }
  }

  /**
   * Borrows a new database session and returns it.
   */
  private def borrowNewDBSession(): Unit = {
    if (log.isDebugEnabled) {
      log.debug(s"Acquiring a new database session for subscriber: ${subscriber}")
    }

    if (_occupiedDBSession != null) {
      releaseOccupiedDBSession(true)
    }
    val session: DBSession = {
      val settings: DatabasePublisherSettings[A] = publisher.settings
      val db: NamedDB = NamedDB(settings.dbName, settings.settingsProvider)(settings.connectionPoolContext)
      db.autoClose(false).readOnlySession()
    }
    _occupiedDBSession = session
  }

  /**
   * Releases the occupied database session.
   */
  private def releaseOccupiedDBSession(discardErrors: Boolean): Unit = {
    if (log.isDebugEnabled) {
      log.debug(s"Releasing the occupied database session for subscriber: ${subscriber}")
    }

    try {
      _occupiedDBSession.close()
    } catch {
      case NonFatal(e) if discardErrors =>
        if (log.isDebugEnabled) {
          log.debug(s"Failed to close the occupied database session because ${e.getMessage}", e)
        } else {
          log.info(s"Failed to close the occupied database session because ${e.getMessage}, exception: ${e.getClass.getCanonicalName}")
        }
    }
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

  /**
   * Schedules a synchronous streaming which holds the given iterator.
   */
  private[this] def scheduleSynchronousStreaming(iterator: StreamResultSetIterator[A]): Unit = {

    val currentSubscription = this

    try {
      val task: Runnable = new Runnable() {
        def run(): Unit = {
          try {
            // must start with remaining iterator every time invoking this Runnable
            var remainingIterator = iterator
            val _ = currentSubscription.sync

            if (remainingIterator == null) {
              // need to start new session for this
              currentSubscription.borrowNewDBSession()
            }

            var demand: Long = currentSubscription.demandBatch
            var realDemand: Long = if (demand < 0) demand - Long.MinValue else demand

            do {
              try {

                if (currentSubscription.cancelled) {
                  // ------------------------
                  // cancelled the streaming
                  log.info(s"Cancellation from subscriber: ${currentSubscription.subscriber} detected")

                  if (currentSubscription.deferredError != null) {
                    throw currentSubscription.deferredError
                  }

                  if (remainingIterator != null) {
                    log.info(s"Responding the deferred error : ${currentSubscription.deferredError} to the cancellation")
                    // the streaming was cancelled before it finished
                    val iteratorToConsume = remainingIterator
                    remainingIterator = null
                    closeIterator(iteratorToConsume)
                  }

                } else if (realDemand > 0 || remainingIterator == null) {
                  // ------------------------
                  // proceed with the remaining iterator

                  // create a new iterator if it absent.
                  val iteratorToConsume: StreamResultSetIterator[A] = {
                    if (remainingIterator != null) remainingIterator
                    else currentSubscription.issueQueryAndCreateNewIterator()
                  }
                  remainingIterator = null
                  remainingIterator = emitDemandedElementsAndReturnRemainingIterator(realDemand, iteratorToConsume)
                }

                if (remainingIterator == null) {
                  // finishing and cleaning up the streaming
                  currentSubscription.releaseOccupiedDBSession(true)
                  currentSubscription.endOfStream.trySuccess(())
                }

              } catch {
                case NonFatal(e) =>
                  if (log.isDebugEnabled) {
                    log.debug(s"Unexpectedly failed to deal with remaining iterator because ${e.getMessage}", e)
                  } else {
                    log.info(s"Unexpectedly failed to deal with remaining iterator because ${e.getMessage}, exception: ${e.getClass.getCanonicalName}")
                  }

                  if (remainingIterator != null) {
                    try {
                      closeIterator(remainingIterator)
                    } catch {
                      case NonFatal(_) => ()
                    }
                  }
                  currentSubscription.releaseOccupiedDBSession(true)
                  throw e

              } finally {
                currentSubscription.currentIterator = remainingIterator
                currentSubscription.sync = 0
              }

              demand = currentSubscription.saveNumberOfDeliveredElementsAndReturnRemainingDemand(demand)
              realDemand = if (demand < 0) demand - Long.MinValue else demand

            } while (remainingIterator != null && realDemand > 0)

          } catch {
            case NonFatal(ex) =>
              currentSubscription.endOfStream.tryFailure(ex)
          }
        }
      }

      publisher.asyncExecutor.execute(task)

    } catch {
      case NonFatal(e) =>
        log.warn(s"Failed to schedule a synchronous processing because ${e.getMessage}", e)
        throw e
    }
  }

  /**
   * Restarts a suspended streaming action.
   * Must only be called from the Subscriber context.
   */
  private[this] def reScheduleSynchronousStreaming(): Unit = {
    val _ = sync
    val iteratorToConsume = currentIterator
    if (iteratorToConsume != null) {
      currentIterator = null
      scheduleSynchronousStreaming(iteratorToConsume)
    }
  }

  /**
   * Emits a bunch of elements.
   */
  private[this] def emitDemandedElementsAndReturnRemainingIterator(
    realDemand: Long,
    iterator: StreamResultSetIterator[A]
  ): StreamResultSetIterator[A] = {
    val bufferNext = publisher.settings.bufferNext
    var count = 0L

    try {
      while ({
        if (bufferNext) iterator.hasNext && count < realDemand
        else count < realDemand && iterator.hasNext
      }) {
        count += 1
        subscriber.onNext(iterator.next())
      }
    } catch {
      case NonFatal(ex) =>
        try {
          iterator.close()
        } catch { case NonFatal(_) => () }
        throw ex
    }

    if (log.isDebugEnabled) {
      log.debug(s"Emitted $count element${if (count > 1) "s" else ""} to subscriber: ${subscriber}, realDemand: ${realDemand}")
    }

    if ((bufferNext && iterator.hasNext) || (!bufferNext && count == realDemand)) iterator
    else null
  }

  /**
   * Cancels a given iterator.
   */
  private[this] def closeIterator(iterator: StreamResultSetIterator[A]): Unit = {
    if (iterator != null) {
      iterator.close()
    }
  }

  /**
   * Finishes the stream with `onComplete` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  private[this] def onComplete(): Unit = {
    if (_isFinished == false && _cancelRequested == false) {
      if (log.isDebugEnabled) {
        log.debug(s"Invoking ${subscriber}#onComplete() from Subscription#onComplete()")
      }

      _isFinished = true
      try {
        subscriber.onComplete()
      } catch {
        case NonFatal(e) =>
          log.warn(s"Subscriber#onComplete() for subscriber: ${subscriber} unexpectedly failed because ${e.getMessage}", e)
      }
    }
  }

}
