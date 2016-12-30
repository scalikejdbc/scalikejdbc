package scalikejdbc.streams

import java.io.Closeable
import java.util.concurrent.{ ExecutorService, TimeUnit }

import scalikejdbc.LogSupport

import scala.concurrent.ExecutionContext

/**
 * Closeable executor which runs asynchronous operations.
 */
trait AsyncExecutor extends Closeable {

  def executionContext: ExecutionContext

  def execute(runnable: Runnable): Unit

  def close(): Unit

}

object AsyncExecutor extends LogSupport {

  def apply(ec: ExecutionContext): AsyncExecutor = {
    new AsyncExecutorBuiltWithExecutionContext(ec)
  }

  def apply(executorService: ExecutorService, autoClose: Boolean = false): AsyncExecutor = {
    new AsyncExecutorBuiltWithExecutorService(executorService, autoClose)
  }

  private class AsyncExecutorBuiltWithExecutionContext(
    override val executionContext: ExecutionContext
  ) extends AsyncExecutor
      with ExecutionContextPreparable {

    override def execute(runnable: Runnable): Unit = preparedExecutionContext().execute(runnable)

    override def close(): Unit = ()

  }

  private class AsyncExecutorBuiltWithExecutorService(
    executorService: ExecutorService,
    autoClose: Boolean = false
  ) extends AsyncExecutor
      with ExecutionContextPreparable
      with LogSupport {

    override lazy val executionContext: ExecutionContext = ExecutionContext.fromExecutorService(executorService)

    override def execute(runnable: Runnable): Unit = preparedExecutionContext().execute(runnable)

    override def close(): Unit = {
      if (autoClose) {
        executorService.shutdownNow()
        if (executorService.awaitTermination(30, TimeUnit.SECONDS) == false) {
          log.warn("Failed to terminate ExecutorService after waiting 30 seconds")
        }
      } else {
        ()
      }
    }
  }

}
