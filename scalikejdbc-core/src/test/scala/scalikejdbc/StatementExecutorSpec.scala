package scalikejdbc

import org.scalatest._
import java.sql.PreparedStatement

class StatementExecutorSpec extends FlatSpec with Matchers {

  behavior of "StatementExecutor"

  it should "be available" in {
    val underlying: PreparedStatement = null
    val template: String = ""
    val params: Seq[Any] = Nil
    val instance = new StatementExecutor(underlying, template, params)
    instance should not be null
  }

}
