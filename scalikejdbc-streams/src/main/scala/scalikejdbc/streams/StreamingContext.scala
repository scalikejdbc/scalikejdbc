package scalikejdbc.streams

import java.util.concurrent.atomic.AtomicLong

import org.reactivestreams.{ Subscriber, Subscription }
import scalikejdbc.{ DBSession, LogSupport, WithExtractor }

import scala.concurrent.Promise
import scala.util.control.NonFatal

class StreamingContext[A, E <: WithExtractor](
    val publisher: DatabasePublisher[A, E],
    val subscriber: Subscriber[_]
) extends Subscription with LogSupport {

  /**
   * A volatile variable to enforce the happens-before relationship (see
   * [[https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html]] and
   * [[http://gee.cs.oswego.edu/dl/jmm/cookbook.html]]) when executing something in
   * a synchronous action context. It is read when entering the context and written when leaving
   * so that all writes to non-volatile variables within the context are visible to the next
   * synchronous execution.
   */
  @volatile private[streams] var sync = 0

  // TODO: use Option
  private[streams] var currentSession: DBSession = null

  def session: DBSession = currentSession

  /** Whether the Subscriber has been signaled with `onComplete` or `onError`. */
  private[this] var finished = false

  /**
   * The total number of elements requested and not yet marked as delivered by the synchronous streaming action.
   * Whenever this value drops to 0, streaming is suspended.
   * When it is raised up from 0 in `request`, streaming is scheduled to be restarted.
   * It is initially set to `Long.MinValue` when streaming starts.
   * Any negative value above `Long.MinValue` indicates the actual demand at that point.
   * It is reset to 0 when the initial streaming ends.
   */
  private[this] val remaining = new AtomicLong(Long.MinValue)

  /**
   * An error that will be signaled to the Subscriber when the stream is cancelled or terminated.
   * This is used for signaling demand overflow in `request()`
   * while guaranteeing that the `onError` message does not overlap with an active `onNext` call.
   */
  private[streams] var deferredError: Throwable = null

  /**
   * The state for a suspended streaming action.
   * Must be set only from a synchronous action context.
   */
  private[this] var currentIterator: CloseableIterator[A] = null

  private[streams] def replaceCurrentIterator(newIterator: CloseableIterator[A]): Unit = {
    currentIterator = newIterator
  }

  /** The streaming action which may need to be continued with the suspended state */
  private[streams] var emitter: StreamingEmitter[A, E] = null

  @volatile private[this] var cancelRequested = false

  /** The Promise to complete when streaming has finished. */
  val streamingResultPromise: Promise[Null] = Promise[Null]()

  /**
   * Indicate that the specified number of elements has been delivered. Returns the remaining demand.
   * This is an atomic operation.
   * It must only be called from the synchronous action context which performs the streaming.
   */
  def delivered(num: Long): Long = remaining.addAndGet(-num)

  /**
   * Get the current demand that has not yet been marked as delivered and mark it as being in the current batch.
   * When this value is negative, the initial streaming action is still running
   * and the real demand can be computed by subtracting `Long.MinValue` from the returned value.
   */
  def demandBatch: Long = remaining.get()

  /** Whether the stream has been cancelled by the Subscriber */
  def cancelled: Boolean = cancelRequested

  def emit(v: Any): Unit = subscriber.asInstanceOf[Subscriber[Any]].onNext(v)

  /**
   * Finish the stream with `onComplete` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  def tryOnComplete(): Unit = if (!finished && !cancelRequested) {
    if (log.isDebugEnabled) log.debug("Signaling onComplete()")
    finished = true
    try subscriber.onComplete() catch {
      case NonFatal(ex) => log.warn("Subscriber.onComplete failed unexpectedly", ex)
    }
  }

  /**
   * Finish the stream with `onError` if it is not finished yet.
   * May only be called from a synchronous action context.
   */
  def tryOnError(t: Throwable): Unit = if (!finished) {
    if (log.isDebugEnabled) log.debug(s"Signaling onError($t)")
    finished = true
    try subscriber.onError(t) catch {
      case NonFatal(ex) => log.warn("Subscriber.onError failed unexpectedly", ex)
    }
  }

  /**
   * Restart a suspended streaming action.
   * Must only be called from the Subscriber context.
   */
  def restartStreaming(): Unit = {
    val _ = sync
    val iteratorToConsume = currentIterator
    if (iteratorToConsume != null) {
      currentIterator = null
      if (log.isDebugEnabled) {
        log.debug("Scheduling stream continuation after transition from demand = 0")
      }

      publisher.scheduleSynchronousStreaming(
        emitter,
        this,
        iteratorToConsume
      )

    } else {
      if (log.isDebugEnabled) {
        log.debug("Saw transition from demand = 0, but no stream continuation available")
      }
    }
  }

  ////////////////////////////////////////////////////////////////////////// Subscription methods

  def request(l: Long): Unit = if (!cancelRequested) {
    if (l <= 0) {
      deferredError = new IllegalArgumentException("Requested count must not be <= 0 (see Reactive Streams spec, 3.9)")
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
      if (!cancelRequested && remaining.getAndAdd(l) == 0L) restartStreaming()
    }
  }

  def cancel(): Unit = if (!cancelRequested) {
    cancelRequested = true
    // Restart streaming because cancelling requires closing the result set and the session from
    // within a synchronous action context. This will also complete the result Promise and thus
    // allow the rest of the scheduled Action to run.
    if (remaining.getAndSet(Long.MaxValue) == 0L) restartStreaming()
  }
}