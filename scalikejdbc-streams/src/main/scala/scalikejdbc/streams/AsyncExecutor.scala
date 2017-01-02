package scalikejdbc.streams

import scalikejdbc.LogSupport

import scala.concurrent.ExecutionContext

/**
 * Executes asynchronous operations.
 *
 * This class properly closes a closeable internal state if exists.
 */
private[streams] trait AsyncExecutor {

  /**
   * Returns current ExecutionContext.
   */
  def executionContext: ExecutionContext

  /**
   * Runs an asynchronous operation.
   */
  def execute(runnable: Runnable): Unit

}

object AsyncExecutor extends LogSupport {

  /**
   * Returns AsyncExecutor built from ExecutionContext.
   */
  def apply(ec: ExecutionContext): AsyncExecutor = {
    new AsyncExecutorBuiltWithExecutionContext(ec)
  }

  private class AsyncExecutorBuiltWithExecutionContext(
      override val executionContext: ExecutionContext
  ) extends AsyncExecutor with ExecutionContextPreparable {

    override def execute(runnable: Runnable): Unit = {
      preparedExecutionContext().execute(runnable)
    }
  }

}
