package scalikejdbc.streams

import scala.concurrent.ExecutionContext

/**
 * ExecutionContext#prepare has been marked as deprecated since Scala 2.12.
 * But we still need the method for older Scala versions.
 * This trait is needed to keep the backward compatibility for older Scala.
 *
 * see also [[https://scala-lang.org/api/2.12.20/scala/concurrent/ExecutionContext.html]]
 */
private[streams] trait ExecutionContextPreparable {

  def executionContext: ExecutionContext

  def preparedExecutionContext(): ExecutionContext = executionContext

}
