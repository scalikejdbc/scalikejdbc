package scalikejdbc.interpolation

import org.scalatest._
import org.scalatest.matchers._

class SQLSyntaxSpec extends FlatSpec with Matchers {

  import Implicits._

  behavior of "SQLSyntax"

  it should "be available" in {
    SQLSyntax("where") should not be (null)
  }

  it should "has methods to append" in {
    {
      val s = SQLSyntax.eq(sqls"id", 123)
      s.value should equal(" id = ?")
      s.parameters should equal(Seq(123))
    }
    {
      val s = SQLSyntax.eq(sqls"id", 123).and.ne(sqls"name", "Alice")
      s.value should equal(" id = ? and name <> ?")
      s.parameters should equal(Seq(123, "Alice"))
    }
  }

}
