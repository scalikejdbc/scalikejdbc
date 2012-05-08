package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import java.sql.ResultSet

class WrappedResultSetSpec extends FlatSpec with ShouldMatchers {

  behavior of "WrappedResultSet"

  it should "be available" in {
    val underlying: ResultSet = null
    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val instance = new WrappedResultSet(underlying, cursor, cursor.index)
    instance should not be null
  }

  // [NOTICE]
  // most of test cases at scalikejdbc.DBSessionSpec

}
