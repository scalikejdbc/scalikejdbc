package scalikejdbc

import org.scalatest._

import scala.util.control.NonFatal

trait SyntaxProviderTestSupport extends TestSuiteMixin with SQLInterpolation { self: TestSuite =>

  implicit var session: DBSession = _

  case class Account(id: Long, name: String)
  object Account extends SQLSyntaxSupport[Account] {
    override val tableName = "qi_accounts"
  }

  abstract override protected def withFixture(test: NoArgTest): Outcome = {
    DB.autoCommit { implicit session =>
      this.session = session
      sql"create table ${Account.table} (id int not null, name varchar(256))".execute.apply()
      try {
        super.withFixture(test)
      } finally {
        try {
          sql"drop table ${Account.table}".execute.apply()
        } catch {
          case NonFatal(e) =>
        }
      }
    }

  }
}

trait SyntaxProviderSpec extends FunSpec
  with Matchers
  with SQLInterpolation
  with SyntaxProviderTestSupport
  with DBSettings

class SubQuerySQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get sub query sql syntax") {
    val a = Account.syntax("a")
    val p = SubQuery.syntax("p", a.resultName)
    p.* should be(SQLSyntax("p.i_on_a, p.n_on_a"))
    p.asterisk should be(SQLSyntax("p.*"))
    p.resultAll should be(SQLSyntax("p.i_on_a as i_on_a_on_p, p.n_on_a as n_on_a_on_p"))
  }

}

