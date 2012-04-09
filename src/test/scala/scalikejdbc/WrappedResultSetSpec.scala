package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql.ResultSet
import java.util.Calendar
import collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class WrappedResultSetSpec extends FlatSpec with ShouldMatchers {

  behavior of "WrappedResultSet"

  it should "be available" in {
    val underlying: ResultSet = null
    val instance = new WrappedResultSet(underlying)
    instance should not be null
  }

}
