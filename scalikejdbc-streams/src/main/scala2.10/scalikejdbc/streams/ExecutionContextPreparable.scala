package scalikejdbc.streams

import scala.concurrent.ExecutionContext

trait ExecutionContextPreparable {

  def executionContext: ExecutionContext

  def preparedExecutionContext(): ExecutionContext = executionContext.prepare()

}