package scalikejdbc.streams

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicLong }

import org.reactivestreams.{ Subscriber, Subscription }
import scalikejdbc._

import scala.concurrent.Promise
import scala.util.{ Failure, Success }
import scala.util.control.NonFatal

/**
 * A DatabaseSubscription represents a one-to-one lifecycle of a Subscriber subscribing to a DatabasePublisher.
 *
 * It can only be used once by a single Subscriber.
 * It is used to both signal desire for data and cancel demand (and allow resource cleanup).
 */
private[streams] class DatabaseSubscription[A](
  /**
   * DatabasePublisher in the fashion of Reactive Streams.
   */
  private[streams] val publisher: DatabasePublisher[A],
  /**
   * Subscriber in the fashion of Reactive Streams.
   */
  private[streams] val subscriber: Subscriber[_ >: A]
) extends Subscription
  with LogSupport {

  // -----------------------------------------------
  // Internal state
  // -----------------------------------------------

  /**
   * Stream ready SQL object.
   */
  private def sql: StreamReadySQL[A] = publisher.sql

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
  private[this] var _maybeOccupiedDBSession: Option[DBSession] = None

  /**
   * The state for a suspended streaming action.
   * Must be set only from a synchronous action context.
   */
  private var maybeRemainingIterator: Option[StreamResultSetIterator[A]] = None

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
  private[this] val _numberOfRemainingElements: AtomicLong = new AtomicLong(
    Long.MinValue
  )

  /**
   * Returns true if it has been cancelled by the Subscriber.
   */
  private[this] val _isCancellationAlreadyRequested: AtomicBoolean =
    new AtomicBoolean(false)

  /**
   * Whether the Subscriber has been signaled with `onComplete` or `onError`.
   */
  private[this] val _isCurrentSubscriptionFinished: AtomicBoolean =
    new AtomicBoolean(false)

  /**
   * An error that will be signaled to the Subscriber when the stream is cancelled or terminated.
   *
   * This is used for signaling demand overflow in #request(Long)
   * while guaranteeing that the #onError(Throwable) message does not overlap with an active #onNext(A) call.
   */
  private[this] var _maybeDeferredError: Option[Throwable] = None

  // -----------------------------------------------
  // Reactive Streams Subscription APIs
  // -----------------------------------------------

  /**
   * No events will be sent by a Publisher until demand is signaled via this method.
   */
  override def request(n: Long): Unit = {
    if (isCancellationAlreadyRequested) {
      if (log.isDebugEnabled) {
        log.debug(
          s"Subscription#request($n) called from subscriber: ${subscriber} after cancellation, skipped processing"
        )
      }

    } else {
      if (log.isDebugEnabled) {
        log.debug(
          s"Subscription#request($n) called from subscriber: ${subscriber}"
        )
      }

      if (n <= 0) {
        // 3. Subscription - 9
        // see: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code
        //
        // While the Subscription is not cancelled, Subscription.request(long n)
        // MUST signal onError with a java.lang.IllegalArgumentException if the argument is <= 0.
        // The cause message MUST include a reference to this rule and/or quote the full rule.
        //
        _maybeDeferredError = Some(
          new IllegalArgumentException(
            "The n of Subscription#request(long n) must not be larger than 0 (Reactive Streams spec, 3.9)"
          )
        )

        cancel()

      } else {
        // 3. Subscription - 17
        // see: https://github.com/reactive-streams/reactive-streams-jvm/blob/v1.0.0/README.md#3-subscription-code
        //
        // A Subscription MUST support an unbounded number of calls to request
        // and MUST support a demand (sum requested - sum delivered) up to 2^63-1 (java.lang.Long.MAX_VALUE).
        // A demand equal or greater than 2^63-1 (java.lang.Long.MAX_VALUE) MAY be considered by the Publisher as “effectively unbounded”[3].
        //
        if (
          isCancellationAlreadyRequested == false && _numberOfRemainingElements
            .getAndAdd(n) == 0L
        ) {
          reScheduleSynchronousStreaming()
        }
      }
    }
  }

  /**
   * Requests the Publisher to stop sending data and clean up resources.
   */
  override def cancel(): Unit = {
    if (_isCancellationAlreadyRequested.getAndSet(true)) {
      if (log.isDebugEnabled) {
        log.debug(
          s"Subscription#cancel() called from subscriber: ${subscriber} again, skipped processing"
        )
      }

    } else {
      log.info(s"Subscription#cancel() called from subscriber: ${subscriber}")

      // restart the streaming here because cancelling it requires closing the occupied database session.
      // This will also complete the result Promise and thus allow the rest of the scheduled Action to run.
      if (_numberOfRemainingElements.getAndSet(Long.MaxValue) == 0L) {
        try {
          reScheduleSynchronousStreaming()
        } catch {
          case t: Throwable =>
            log.warn("Caught an exception in Subscription#cancel()", t)
            finishAsCompletionWithoutException()
            t match {
              case _: InterruptedException => Thread.currentThread().interrupt()
              case _                       => throw t
            }
        }
      }
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
    if (_isCurrentSubscriptionFinished.getAndSet(true) == false) {
      if (log.isDebugEnabled) {
        log.debug(
          s"Subscriber#onError for subscriber: ${subscriber} called with exception: $t"
        )
      }

      try {
        subscriber.onError(t)
      } catch {
        case NonFatal(e) =>
          log.warn(
            s"Subscriber#onError for subscriber: ${subscriber} unexpectedly failed because ${e.getMessage}",
            e
          )
      }
    }
  }

  /**
   * Starts new streaming.
   */
  private[streams] def startNewStreaming(): Unit = {
    scheduleSynchronousStreaming(None)
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
  private def saveNumberOfDeliveredElementsAndReturnRemainingDemand(
    num: Long
  ): Long = {
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
  private def maybeDeferredError: Option[Throwable] = _maybeDeferredError

  /**
   * Returns the DBSession occupied by current subscription.
   */
  private def maybeOccupiedDBSession: Option[DBSession] =
    _maybeOccupiedDBSession

  /**
   * Whether the stream has been cancelled by the Subscriber
   */
  private def isCancellationAlreadyRequested: Boolean =
    _isCancellationAlreadyRequested.get()

  /**
   * Whether the current subscription is already finished.
   */
  private def isCurrentSubscriptionFinished: Boolean =
    _isCurrentSubscriptionFinished.get()

  /**
   * Issues a query and creates a new iterator to consume.
   */
  private def issueQueryAndCreateNewIterator(): StreamResultSetIterator[A] = {

    val occupiedDBSession =
      maybeOccupiedDBSession.getOrElse(occupyNewDBSession())
    val statementExecutor = new DBSessionWrapper(
      occupiedDBSession,
      sql.createDBSessionAttributesSwitcher
    ).toStatementExecutor(sql.statement, sql.rawParameters)
    val resultSet = statementExecutor.executeQuery()
    val resultSetProxy = new DBConnectionAttributesWiredResultSet(
      resultSet,
      occupiedDBSession.connectionAttributes
    )

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
  private def occupyNewDBSession(): DBSession = {
    if (log.isDebugEnabled) {
      log.debug(
        s"Acquiring a new database session for subscriber: ${subscriber}"
      )
    }

    _maybeOccupiedDBSession match {
      case Some(_) => releaseOccupiedDBSession(true)
      case _       =>
    }

    val session: DBSession = {
      implicit val cpContext = publisher.settings.connectionPoolContext
      val sessionProvider: NamedDB =
        NamedDB(publisher.settings.dbName, publisher.settings.settingsProvider)
      sessionProvider.autoClose(false).readOnlySession()
    }
    _maybeOccupiedDBSession = Some(session)
    session
  }

  /**
   * Releases the occupied database session.
   */
  private def releaseOccupiedDBSession(discardErrors: Boolean): Unit = {
    if (log.isDebugEnabled) {
      log.debug(
        s"Releasing the occupied database session for subscriber: ${subscriber}"
      )
    }

    try {
      _maybeOccupiedDBSession match {
        case Some(session) => session.close()
        case _             =>
      }
    } catch {
      case NonFatal(e) if discardErrors =>
        if (log.isDebugEnabled) {
          log.debug(
            s"Failed to close the occupied database session because ${e.getMessage}",
            e
          )
        } else {
          log.info(
            s"Failed to close the occupied database session because ${e.getMessage}, exception: ${ClassNameUtil
                .getClassName(e.getClass)}"
          )
        }
    } finally {
      _maybeOccupiedDBSession = None
    }
  }

  // -----------------------------------------------
  // Completely internal methods
  // -----------------------------------------------

  /**
   * Schedules a synchronous streaming which holds the given iterator.
   */
  private[this] def scheduleSynchronousStreaming(
    maybeIterator: Option[StreamResultSetIterator[A]]
  ): Unit = {

    val currentSubscription = this

    try {
      val task: Runnable = new Runnable() {
        def run(): Unit = {
          try {
            // must start with remaining iterator every time invoking this Runnable
            var maybeRemainingIterator: Option[StreamResultSetIterator[A]] =
              maybeIterator
            val _ = currentSubscription.sync

            maybeRemainingIterator match {
              case None => currentSubscription.occupyNewDBSession()
              case _    =>
            }

            var demand: Long = currentSubscription.demandBatch
            var realDemand: Long =
              if (demand < 0) demand - Long.MinValue else demand

            def loop(): Unit = {
              try {

                if (currentSubscription.isCancellationAlreadyRequested) {
                  // ------------------------
                  // cancelling the current subscription

                  log.info(
                    s"Cancellation from subscriber: ${currentSubscription.subscriber} detected"
                  )

                  try {
                    currentSubscription.maybeDeferredError match {
                      case Some(error) =>
                        log.info(
                          s"Responding the deferred error : ${currentSubscription.maybeDeferredError} to the cancellation"
                        )
                        throw error
                      case _ =>
                    }

                  } finally {
                    cleanUpResources()
                  }

                } else if (realDemand > 0 || maybeRemainingIterator.isEmpty) {
                  // ------------------------
                  // proceed with the remaining iterator

                  // create a new iterator if it absent.
                  val iterator: StreamResultSetIterator[A] = {
                    maybeRemainingIterator match {
                      case Some(iterator) => iterator
                      case _ =>
                        currentSubscription.issueQueryAndCreateNewIterator()
                    }
                  }
                  maybeRemainingIterator =
                    emitElementsAndReturnRemainingIterator(realDemand, iterator)
                }

                if (maybeRemainingIterator.isEmpty) {
                  log.info(
                    s"All data for subscriber: ${currentSubscription.subscriber} has been sent"
                  )
                  finishAsCompletionWithoutException()
                }

              } catch {
                case NonFatal(e) =>
                  if (log.isDebugEnabled) {
                    log.debug(
                      s"Unexpectedly failed to deal with remaining iterator because ${e.getMessage}",
                      e
                    )
                  } else {
                    log.info(
                      s"Unexpectedly failed to deal with remaining iterator because ${e.getMessage}, exception: ${ClassNameUtil
                          .getClassName(e.getClass)}"
                    )
                  }
                  cleanUpResources()
                  throw e

              } finally {
                currentSubscription.maybeRemainingIterator =
                  maybeRemainingIterator
                currentSubscription.sync = 0
              }

              demand = currentSubscription
                .saveNumberOfDeliveredElementsAndReturnRemainingDemand(demand)
              realDemand = if (demand < 0) demand - Long.MinValue else demand
            }

            loop()
            while (maybeRemainingIterator.isDefined && realDemand > 0) {
              loop()
            }

          } catch {
            case NonFatal(ex) =>
              currentSubscription.endOfStream.tryFailure(ex)
          }
        }
      }

      publisher.asyncExecutor.execute(task)

    } catch {
      case NonFatal(e) =>
        log.warn(
          s"Failed to schedule a synchronous processing because ${e.getMessage}",
          e
        )
        throw e
    }
  }

  /**
   * Restarts a suspended streaming action.
   * Must only be called from the Subscriber context.
   */
  private[this] def reScheduleSynchronousStreaming(): Unit = {
    val _ = sync
    maybeRemainingIterator match {
      case Some(remainingIterator) =>
        maybeRemainingIterator = None
        scheduleSynchronousStreaming(Some(remainingIterator))
      case _ =>
    }
  }

  /**
   * Emits a bunch of elements.
   */
  private[this] def emitElementsAndReturnRemainingIterator(
    realDemand: Long,
    iterator: StreamResultSetIterator[A]
  ): Option[StreamResultSetIterator[A]] = {

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
      case NonFatal(e) =>
        try {
          iterator.close()
        } catch { case NonFatal(_) => }
        throw e
    }

    if (log.isDebugEnabled) {
      log.debug(s"Emitted $count element${if (count > 1) "s"
        else ""} to subscriber: ${subscriber}, realDemand: ${realDemand}")
    }

    if (
      (bufferNext && iterator.hasNext)
      || (bufferNext == false && count == realDemand)
    ) {
      Some(iterator)
    } else {
      None
    }
  }

  /**
   * Cleans up the occupied resources.
   */
  private[this] def cleanUpResources(): Unit = {
    try {
      releaseOccupiedDBSession(true)
      log.info(
        s"Finished cleaning up database resources occupied for subscriber: ${subscriber}"
      )
    } catch {
      case NonFatal(e) =>
        log.warn(
          "Caught an exception while releasing the occupied database session",
          e
        )
    } finally {

      try {
        maybeRemainingIterator match {
          case Some(iterator) =>
            if (iterator != null) {
              iterator.close()
            }
            maybeRemainingIterator = None
          case _ =>
        }
      } catch {
        case NonFatal(e) =>
          log.warn(
            "Caught an exception while closing the remaining iterator",
            e
          )
      }
    }
  }

  /**
   * Finishes the current subscription as completed.
   */
  private[this] def finishAsCompletionWithoutException(): Unit = {
    try {
      cleanUpResources()
    } catch {
      case e: Throwable => throw e
    } finally {

      try {
        endOfStream.trySuccess(())
      } catch {
        case NonFatal(e) =>
          log.warn("Caught an exception while finishing the subscription", e)
      }
    }
  }

  /**
   * Finishes the stream with `onComplete` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  private[this] def onComplete(): Unit = {
    if (
      isCurrentSubscriptionFinished == false && isCancellationAlreadyRequested == false
    ) {
      if (log.isDebugEnabled) {
        log.debug(
          s"Invoking ${subscriber}#onComplete() from Subscription#onComplete()"
        )
      }
      _isCurrentSubscriptionFinished.set(true)
      try {
        subscriber.onComplete()
      } catch {
        case NonFatal(e) =>
          log.warn(
            s"Subscriber#onComplete() for subscriber: ${subscriber} unexpectedly failed because ${e.getMessage}",
            e
          )
      }
    }
  }

}
