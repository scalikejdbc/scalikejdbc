package scalikejdbc.streams

import org.reactivestreams.Subscriber
import scalikejdbc.{ WithExtractor, _ }

import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

class StreamingDB[A, E <: WithExtractor](
    executor: AsyncExecutor,
    name: Any,
    connectionPoolContext: DB.CPContext,
    settings: SettingsProvider,
    bufferNext: Boolean = true
) extends LogSupport { self =>

  def stream(sql: StreamingSQL[A, E]): DatabasePublisher[A] = {
    val action = new StreamingAction[A, E](sql)
    createPublisher(action)
  }

  private[streams] def scheduleSynchronousStreaming(
    action: StreamingAction[A, E],
    ctx: StreamingContext[A, E]
  )(initialState: CloseableIterator[A]): Unit = try {
    executor.execute(new Runnable {
      private[this] def str(l: Long) = if (l != Long.MaxValue) l else "Inf"
      private[this] val debug = log.isDebugEnabled

      def run(): Unit = try {
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
                action.cancelStream(ctx, oldState)
              }
            } else if (realDemand > 0 || (state eq null)) {
              val oldState = state
              state = null
              state = action.emitStream(ctx, realDemand, oldState)
            }
            if (state eq null) { // streaming finished and cleaned up
              releaseSession(ctx, true)
              ctx.streamingResultPromise.trySuccess(null)
            }
          } catch {
            case NonFatal(ex) =>
              if (state ne null) try action.cancelStream(ctx, state) catch { case NonFatal(_) => () }
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

  private[this] def createPublisher(
    action: StreamingAction[A, E]
  ): DatabasePublisher[A] = {
    new DatabasePublisher[A] {
      override def subscribe(subscriber: Subscriber[_ >: A]): Unit = {
        if (subscriber eq null) {
          throw new NullPointerException("given a null Subscriber in subscribe. (see Reactive Streams spec, 1.9)")
        }
        val context = new StreamingContext[A, E](subscriber, self, bufferNext)
        val subscribed = try { subscriber.onSubscribe(context); true } catch {
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

  private[this] def acquireSession(context: StreamingContext[A, E]): Unit = {
    val session = NamedDB(name, settings)(connectionPoolContext).autoClose(false).readOnlySession()
    context.currentSession = session
  }

  private[this] def releaseSession(context: StreamingContext[A, E], discardErrors: Boolean): Unit = {
    try {
      context.currentSession.close()
    } catch { case NonFatal(_) if discardErrors => }
    context.currentSession = null
  }
}

object StreamingDB {

  def apply[A, E <: WithExtractor](
    dbName: Any,
    executor: AsyncExecutor
  )(implicit
    context: DB.CPContext = DB.NoCPContext,
    settingsProvider: SettingsProvider = SettingsProvider.default): StreamingDB[A, E] = {
    new StreamingDB(executor, dbName, context, settingsProvider)
  }

}

