package scalikejdbc

import org.scalatest._

class SQLInterpolationStringSuite extends FlatSpec with Matchers {

  import scalikejdbc.interpolation.Implicits._

  behavior of "SQLInterpolationString"

  it should "be equalization of changing parameter among Set" in {
    import scala.collection.mutable
    val s1 = sqls"s in (${Set(1, 2, 3)})"
    val s2 = sqls"s in (${mutable.Set(1, 2, 3)}"
    s1.parameters should equal(s2.parameters)
  }

  it should "strip margin by stripMargin" in {
    sql"""SELECT
         |${1}
         |""".stripMargin.statement should equal("SELECT\n?\n")
  }

  it should "strip margin specifying marginChar by stripMargin" in {
    sql"""SELECT
         /${1}
         /""".stripMargin('/').statement should equal("SELECT\n?\n")
  }

}
