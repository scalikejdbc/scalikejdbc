package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQL_AttributesSpec
  extends AnyFlatSpec
  with Matchers
  with DBSettings
  with SQLInterpolation {

  behavior of "SQL"

  case class Company(id: Long, members: collection.Seq[Member] = Seq.empty)
  case class Member(id: Long, companyId: Long, name: Option[String])

  object Company extends SQLSyntaxSupport[Company] {
    override val columns: Seq[String] = Seq("id")
    def apply(rs: WrappedResultSet, u: ResultName[Company]): Company = Company(
      rs.get(u.id)
    )
  }
  object Member extends SQLSyntaxSupport[Member] {
    override val columns: Seq[String] = Seq("id", "company_id", "name")
    def apply(rs: WrappedResultSet, u: ResultName[Member]): Member = {
      Member(rs.get(u.id), rs.get(u.companyId), rs.get(u.name))
    }
  }

  it should "contain queryTimeout, fetchSize and tags when using one-to-x APIs" in {
    val (c, m) = (Company.syntax("c"), Member.syntax("m"))

    val query: OneToManySQLToOption[Company, Member, NoExtractor, Nothing] = {
      withSQL {
        select
          .from[Company](Company as c)
          .leftJoin(Member as m)
          .on(c.id, m.companyId)
          .where
          .eq(c.id, 123)
      }.queryTimeout(5)
        .fetchSize(10)
        .tags("foo", "bar")
        .one(rs => Company(rs, c.resultName))
        .toMany(rs =>
          rs.longOpt(m.resultName.id).map(_ => Member(rs, m.resultName))
        )
        .single
    }

    query.queryTimeout should equal(Some(5))
    query.fetchSize should equal(Some(10))
    query.tags should equal(Seq("foo", "bar"))
  }

}
