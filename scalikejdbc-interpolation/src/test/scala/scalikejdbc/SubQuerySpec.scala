package scalikejdbc

import org.scalatest._

class SubQuerySpec extends FlatSpec with Matchers with SQLInterpolation {

  Class.forName("org.h2.Driver")
  ConnectionPool.add('SubQuerySpec, "jdbc:h2:mem:SubQuerySpec", "user", "pass")

  case class Account(id: Int, name: String)
  object Account extends SQLSyntaxSupport[Account] {
    override lazy val connectionPoolName = 'SubQuerySpec
  }

  it should "work" in {

    NamedDB('SubQuerySpec).autoCommit { implicit session =>
      sql"create table account(id integer, name varchar(10))".execute.apply()
    }

    val a = Account.syntax("a")
    val s = SubQuery.syntax("s", a.resultName)

    s(a.resultName.id) should equal(SQLSyntax(value = "s.i_on_a", parameters = List()))

    try {
      s(sqls"id")
    } catch {
      case e: InvalidColumnNameException =>
        e.getMessage should equal("Invalid column name. (name: id, registered names: i_on_a,n_on_a)")
    }
  }

}
