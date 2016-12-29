package scalikejdbc.streams

import java.util.concurrent.atomic.AtomicLong

import org.reactivestreams.{ Subscriber, Subscription }
import scalikejdbc._

import scala.concurrent.Promise
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

class StreamingDB(
    executor: StreamingDB.Executor,
    name: Any,
    connectionPoolContext: DB.CPContext,
    settings: SettingsProvider,
    bufferNext: Boolean = true
) extends LogSupport { self =>
  type StreamingActionContext = StreamingContext

  def stream[A, E <: WithExtractor](sql: StreamingSQL[A, E]): DatabasePublisher[A] = {
    val action = createAction(sql)
    createPublisher(action)
  }

  protected[this] def createAction[A, E <: WithExtractor](sql: StreamingSQL[A, E]): StreamingAction[A, E] = {
    new StreamingAction[A, E] {
      override def createInvoker(): StreamingInvoker[A, E] = {
        new StreamingInvoker[A, E] {
          override protected[this] def streamingSql: StreamingSQL[A, E] = sql
        }
      }
    }
  }

  protected[this] def createPublisher[A, E <: WithExtractor](action: StreamingAction[A, E]): DatabasePublisher[A] = {
    new DatabasePublisher[A] {
      override def subscribe(s: Subscriber[_ >: A]): Unit = {
        if (s eq null) {
          throw new NullPointerException("given a null Subscriber in subscribe. (see Reactive Streams spec, 1.9)")
        }
        val context = new StreamingContext(s, self, bufferNext)
        val subscribed = try { s.onSubscribe(context); true } catch {
          case NonFatal(ex) =>
            log.warn("Subscriber.onSubscribe failed unexpectedly", ex)
            false
        }
        if (subscribed) {
          try {
            context.streamingAction = action
            scheduleSynchronousStreaming(context.streamingAction, context)(null)
            context.streamingResultPromise.future.onComplete {
              case Success(_) => context.tryOnComplete()
              case Failure(t) => context.tryOnError(t)
            }(executor.executionContext)
          } catch { case NonFatal(ex) => context.tryOnError(ex) }
        }
      }
    }
  }

  protected[this] def acquireSession(context: StreamingContext): Unit = {
    context.currentSession = StreamingDB.connect(name, connectionPoolContext, settings).readOnlySession()
  }

  protected[this] def releaseSession(context: StreamingContext, discardErrors: Boolean): Unit = {
    try context.currentSession.close() catch { case NonFatal(_) if discardErrors => }
    context.currentSession = null
  }

  protected[StreamingDB] def scheduleSynchronousStreaming(a: StreamingAction[_, _ <: WithExtractor], ctx: StreamingActionContext)(initialState: a.State): Unit = try {
    executor.execute(new Runnable {
      private[this] def str(l: Long) = if (l != Long.MaxValue) l else "Inf"

      def run(): Unit = try {
        val debug = log.isDebugEnabled
        var state = initialState
        val _ = ctx.sync
        if (state eq null) acquireSession(ctx)
        var demand = ctx.demandBatch
        var realDemand = if (demand < 0) demand - Long.MinValue else demand
        do {
          try {
            if (debug)
              log.debug((if (state eq null) "Starting initial" else "Restarting") + " streaming action, realDemand = " + str(realDemand))
            if (ctx.cancelled) {
              if (ctx.deferredError ne null) throw ctx.deferredError
              if (state ne null) { // streaming cancelled before finishing
                val oldState = state
                state = null
                a.cancelStream(ctx, oldState)
              }
            } else if (realDemand > 0 || (state eq null)) {
              val oldState = state
              state = null
              state = a.emitStream(ctx, realDemand, oldState)
            }
            if (state eq null) { // streaming finished and cleaned up
              releaseSession(ctx, true)
              ctx.streamingResultPromise.trySuccess(null)
            }
          } catch {
            case NonFatal(ex) =>
              if (state ne null) try a.cancelStream(ctx, state) catch { case NonFatal(_) => () }
              releaseSession(ctx, true)
              throw ex
          } finally {
            ctx.streamState = state
            ctx.sync = 0
          }
          if (debug) {
            if (state eq null) log.debug(s"Sent up to ${str(realDemand)} elements - Stream " + (if (ctx.cancelled) "cancelled" else "completely delivered"))
            else log.debug(s"Sent ${str(realDemand)} elements, more available - Performing atomic state transition")
          }
          demand = ctx.delivered(demand)
          realDemand = if (demand < 0) demand - Long.MinValue else demand
        } while ((state ne null) && realDemand > 0)
        if (debug) {
          if (state ne null) log.debug("Suspending streaming action with continuation (more data available)")
          else log.debug("Finished streaming action")
        }
      } catch { case NonFatal(ex) => ctx.streamingResultPromise.tryFailure(ex) }
    })
  } catch {
    case NonFatal(ex) =>
      log.warn("Error scheduling synchronous streaming", ex)
      throw ex
  }

  class StreamingContext(subscriber: Subscriber[_], database: StreamingDB, val bufferNext: Boolean) extends Subscription with LogSupport {
    /**
     * A volatile variable to enforce the happens-before relationship (see
     * [[https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html]] and
     * [[http://gee.cs.oswego.edu/dl/jmm/cookbook.html]]) when executing something in
     * a synchronous action context. It is read when entering the context and written when leaving
     * so that all writes to non-volatile variables within the context are visible to the next
     * synchronous execution.
     */
    @volatile private[StreamingDB] var sync = 0

    private[StreamingDB] var currentSession: DBSession = null

    def session: DBSession = currentSession

    /** Whether the Subscriber has been signaled with `onComplete` or `onError`. */
    private[this] var finished = false

    /**
     * The total number of elements requested and not yet marked as delivered by the synchronous
     * streaming action. Whenever this value drops to 0, streaming is suspended. When it is raised
     * up from 0 in `request`, streaming is scheduled to be restarted. It is initially set to
     * `Long.MinValue` when streaming starts. Any negative value above `Long.MinValue` indicates
     * the actual demand at that point. It is reset to 0 when the initial streaming ends.
     */
    private[this] val remaining = new AtomicLong(Long.MinValue)

    /**
     * An error that will be signaled to the Subscriber when the stream is cancelled or
     * terminated. This is used for signaling demand overflow in `request()` while guaranteeing
     * that the `onError` message does not overlap with an active `onNext` call.
     */
    private[StreamingDB] var deferredError: Throwable = null

    /**
     * The state for a suspended streaming action. Must only be set from a synchronous action
     * context.
     */
    private[StreamingDB] var streamState: AnyRef = null

    /** The streaming action which may need to be continued with the suspended state */
    private[StreamingDB] var streamingAction: StreamingAction[_, _ <: WithExtractor] = null

    @volatile private[this] var cancelRequested = false

    /** The Promise to complete when streaming has finished. */
    val streamingResultPromise: Promise[Null] = Promise[Null]()

    /**
     * Indicate that the specified number of elements has been delivered. Returns the remaining
     * demand. This is an atomic operation. It must only be called from the synchronous action
     * context which performs the streaming.
     */
    def delivered(num: Long): Long = remaining.addAndGet(-num)

    /**
     * Get the current demand that has not yet been marked as delivered and mark it as being in
     * the current batch. When this value is negative, the initial streaming action is still
     * running and the real demand can be computed by subtracting `Long.MinValue` from the
     * returned value.
     */
    def demandBatch: Long = remaining.get()

    /** Whether the stream has been cancelled by the Subscriber */
    def cancelled: Boolean = cancelRequested

    def emit(v: Any): Unit = subscriber.asInstanceOf[Subscriber[Any]].onNext(v)

    /**
     * Finish the stream with `onComplete` if it is not finished yet. May only be called from a
     * synchronous action context.
     */
    def tryOnComplete(): Unit = if (!finished && !cancelRequested) {
      if (log.isDebugEnabled) log.debug("Signaling onComplete()")
      finished = true
      try subscriber.onComplete() catch {
        case NonFatal(ex) => log.warn("Subscriber.onComplete failed unexpectedly", ex)
      }
    }

    /**
     * Finish the stream with `onError` if it is not finished yet. May only be called from a
     * synchronous action context.
     */
    def tryOnError(t: Throwable): Unit = if (!finished) {
      if (log.isDebugEnabled) log.debug(s"Signaling onError($t)")
      finished = true
      try subscriber.onError(t) catch {
        case NonFatal(ex) => log.warn("Subscriber.onError failed unexpectedly", ex)
      }
    }

    /** Restart a suspended streaming action. Must only be called from the Subscriber context. */
    def restartStreaming(): Unit = {
      val _ = sync
      val s = streamState
      if (s ne null) {
        streamState = null
        if (log.isDebugEnabled) log.debug("Scheduling stream continuation after transition from demand = 0")
        val a = streamingAction
        database.scheduleSynchronousStreaming(a, this.asInstanceOf[database.StreamingActionContext])(s.asInstanceOf[a.State])
      } else {
        if (log.isDebugEnabled) log.debug("Saw transition from demand = 0, but no stream continuation available")
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
}

object StreamingDB {
  type Executor = AsyncExecutor

  // same as https://github.com/scalikejdbc/scalikejdbc/blob/2.5.0/scalikejdbc-core/src/main/scala/scalikejdbc/DB.scala#L151-L157
  private[this] def connectionPool(dbName: Any, context: DB.CPContext): ConnectionPool = Option(context match {
    case DB.NoCPContext => ConnectionPool(dbName)
    case _: MultipleConnectionPoolContext => context.get(dbName)
    case _ => throw new IllegalStateException(ErrorMessage.UNKNOWN_CONNECTION_POOL_CONTEXT)
  }) getOrElse {
    throw new IllegalStateException(ErrorMessage.CONNECTION_POOL_IS_NOT_YET_INITIALIZED)
  }

  protected def connect(dbName: Any, connectionPoolContext: DB.CPContext, settings: SettingsProvider): DBConnection = {
    val pool = connectionPool(dbName, connectionPoolContext)
    DB(pool.borrow(), pool.connectionAttributes, settings).autoClose(false)
  }

  def apply(dbName: Any, executor: Executor)(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): StreamingDB = {
    new StreamingDB(executor, dbName, context, settingsProvider)
  }
}

