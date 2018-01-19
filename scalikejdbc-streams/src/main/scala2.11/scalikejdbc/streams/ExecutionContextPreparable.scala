package scalikejdbc.streams

import scala.concurrent.ExecutionContext

private[streams] trait ExecutionContextPreparable {

  def executionContext: ExecutionContext

  def preparedExecutionContext(): ExecutionContext = executionContext.prepare()

}
