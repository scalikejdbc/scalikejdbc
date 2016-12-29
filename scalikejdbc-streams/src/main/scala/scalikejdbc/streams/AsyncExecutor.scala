package scalikejdbc.streams

import java.io.Closeable
import java.util.concurrent.{ ExecutorService, TimeUnit }

import scalikejdbc.LogSupport

import scala.concurrent.ExecutionContext

trait AsyncExecutor extends Closeable {
  def executionContext: ExecutionContext

  def execute(runnable: Runnable): Unit

  def close(): Unit
}

object AsyncExecutor extends LogSupport {

  def apply(ec: ExecutionContext): AsyncExecutor = {
    new AsyncExecutor with ExecutionContextPreparable {
      override val executionContext: ExecutionContext = ec

      override def execute(runnable: Runnable): Unit = prepareExecution.execute(runnable)

      override def close(): Unit = ()
    }
  }

  def apply(executorService: ExecutorService, autoClose: Boolean = false): AsyncExecutor = {
    new AsyncExecutor with ExecutionContextPreparable {
      private[this] val executor = executorService

      override lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executor)

      override def execute(runnable: Runnable): Unit = prepareExecution.execute(runnable)

      override def close(): Unit = if (autoClose) {
        executor.shutdownNow()
        if (executor.awaitTermination(30, TimeUnit.SECONDS))
          log.warn("Abandoning ThreadPoolExecutor (not yet destroyed after 30 seconds)")
      } else ()
    }
  }
}
