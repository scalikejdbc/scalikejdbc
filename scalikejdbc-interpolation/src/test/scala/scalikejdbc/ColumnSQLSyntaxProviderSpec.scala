package scalikejdbc

import org.scalatest._

import scala.util.control.NonFatal
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

trait SyntaxProviderTestSupport extends TestSuiteMixin with SQLInterpolation {
  self: TestSuite =>

  implicit var session: DBSession = _

  case class Account(id: Long, name: String)
  object Account extends SQLSyntaxSupport[Account] {
    override val tableName = "qi_accounts"
  }

  abstract override protected def withFixture(test: NoArgTest): Outcome = {
    DB.autoCommit { implicit session =>
      this.session = session
      sql"create table ${Account.table} (id int not null, name varchar(256))".execute
        .apply()
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

trait SyntaxProviderSpec
  extends AnyFunSpec
  with Matchers
  with SQLInterpolation
  with SyntaxProviderTestSupport
  with DBSettings

class ColumnSQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get column sql syntax") {
    val p = ColumnSQLSyntaxProvider[Account.type, Account](Account)
    p.* should be(SQLSyntax("id, name"))
    p.name should be(SQLSyntax("name"))
  }

}

class QuerySQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get query sql syntax") {
    val p: QuerySQLSyntaxProvider[SQLSyntaxSupport[Account], Account] =
      Account.syntax("a")
    p.columns should be(Seq(SQLSyntax("id"), SQLSyntax("name")))
    p.* should be(SQLSyntax("a.id, a.name"))
    p.column("name") should be(SQLSyntax("a.name"))
    p.resultAll should be(SQLSyntax("a.id as i_on_a, a.name as n_on_a"))
    p.resultName.* should be(SQLSyntax("i_on_a, n_on_a"))
  }

}

class ResultSQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get result sql syntax") {
    val p: ResultSQLSyntaxProvider[SQLSyntaxSupport[Account], Account] =
      Account.syntax("a").result
    p.columns should be(Seq(SQLSyntax("id"), SQLSyntax("name")))
    p.* should be(SQLSyntax("a.id as i_on_a, a.name as n_on_a"))
    p.column("name") should be(SQLSyntax("a.name as n_on_a"))
  }

}

class PartialResultSQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get partial result sql syntax") {
    val p: PartialResultSQLSyntaxProvider[SQLSyntaxSupport[Account], Account] =
      Account.syntax("a").result.apply(sqls"a.name")
    p.name should be(SQLSyntax("a.name as n_on_a"))
  }

}

class BasicResultNameSQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get basic result name sql syntax") {
    val p
      : BasicResultNameSQLSyntaxProvider[SQLSyntaxSupport[Account], Account] =
      Account.syntax("a").resultName
    p.columns should be(Seq(SQLSyntax("id"), SQLSyntax("name")))
    p.* should be(SQLSyntax("i_on_a, n_on_a"))
    p.column("name") should be(SQLSyntax("n_on_a"))
  }

}

class SubQuerySQLSyntaxProviderSpec extends SyntaxProviderSpec {

  it("should get sub query sql syntax") {
    val a = Account.syntax("a")
    val p = SubQuery.syntax("p", a.resultName)
    p.* should be(SQLSyntax("p.i_on_a, p.n_on_a"))
    p.asterisk should be(SQLSyntax("p.*"))
    p.resultAll should be(
      SQLSyntax("p.i_on_a as i_on_a_on_p, p.n_on_a as n_on_a_on_p")
    )
  }

}
