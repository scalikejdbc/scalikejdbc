package scalikejdbc

import java.{ util => ju }

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLInterpolationStringSuite extends AnyFlatSpec with Matchers {

  import scalikejdbc.interpolation.Implicits._

  behavior of "SQLInterpolationString"

  it should "be equalization of changing parameter among Set" in {
    import scala.collection.mutable
    val s1 = sqls"s in (${Set(1, 2, 3)})"
    val s2 = sqls"s in (${mutable.Set(1, 2, 3)}"
    s1.parameters should equal(s2.parameters)
  }

  it should "interpolate java.util.Set" in {
    val set: ju.Set[Int] = new ju.HashSet()
    set.add(1)
    set.add(2)
    set.add(3)

    val s = sqls"s in ($set)"
    s.value should equal("s in (?, ?, ?)")
    s.parameters.toSet should equal(Set(1, 2, 3))
  }

  it should "interpolate java.util.List" in {
    val list: ju.List[Int] = new ju.ArrayList()
    list.add(1)
    list.add(2)
    list.add(3)

    val s = sqls"s in ($list)"
    s.value should equal("s in (?, ?, ?)")
    s.parameters should equal(List(1, 2, 3))
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
