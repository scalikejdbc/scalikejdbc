package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SubQuerySpec extends AnyFlatSpec with Matchers with SQLInterpolation {

  Class.forName("org.h2.Driver")
  ConnectionPool.add("SubQuerySpec", "jdbc:h2:mem:SubQuerySpec", "user", "pass")

  case class Account(id: Int, name: String)
  object Account extends SQLSyntaxSupport[Account] {
    override lazy val connectionPoolName: Any = "SubQuerySpec"
  }

  it should "work" in {

    NamedDB("SubQuerySpec").autoCommit { implicit session =>
      sql"create table account(id integer, name varchar(10))".execute.apply()
    }

    val a = Account.syntax("a")
    val s = SubQuery.syntax("s", a.resultName)

    s(a.resultName.id) should equal(
      SQLSyntax(value = "s.i_on_a", parameters = Nil)
    )

    try {
      s(sqls"id")
    } catch {
      case e: InvalidColumnNameException =>
        e.getMessage should equal(
          "Invalid column name. (name: id, registered names: i_on_a,n_on_a)"
        )
    }
  }

  case class Member(id: Long, groupId: Long)
  object Member extends SQLSyntaxSupport[Member] {
    override val columnNames: Seq[String] = Seq("id", "group_id")
  }

  it should "work with #989" in {
    val m = Member.syntax("m")
    val s = SubQuery.syntax("s").include(m)
    s(m).result.id.value should equal("s.i_on_m as i_on_m_on_s")
  }

}
