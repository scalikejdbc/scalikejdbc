package foo

import scalikejdbc._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AutoNamedValuesSpec extends AnyFlatSpec with Matchers with DBSettings {

  case class Issue(id: Long, firstName: String, groupId: Long)

  val IssueTable = SQLSyntaxSupportFactory[Issue]()

  class Organization(val id: Long, val websiteUrl: String)

  object Organization extends SQLSyntaxSupport[Organization] {
    def apply(s: SyntaxProvider[Organization])(
      rs: WrappedResultSet
    ): Organization = autoConstruct(rs, s)

    def apply(r: ResultName[Organization])(rs: WrappedResultSet): Organization =
      autoConstruct(rs, r)
  }

  case class Person(
    id: Long,
    name: String,
    organizationId: Option[Long],
    organization: Option[Organization] = None,
    groupId: Long = 0
  )

  object Person extends SQLSyntaxSupport[Person] {
    def apply(s: SyntaxProvider[Person])(rs: WrappedResultSet): Person =
      autoConstruct(rs, s, "organization")

    override lazy val columns = autoColumns[Person]("organization")
  }

  behavior of "autoNamedValues"

  it should "execute" in {
    DB autoCommit { implicit s =>
      try {
        try sql"drop table issue".execute.apply()
        catch { case ignore: Exception => }
        sql"create table issue (id int not null, first_name varchar(256), group_id int)".execute
          .apply()

        try sql"drop table organization".execute.apply()
        catch { case ignore: Exception => }
        sql"create table organization (id int not null, website_url varchar(256))".execute
          .apply()

        try sql"drop table person".execute.apply()
        catch { case ignore: Exception => }
        sql"create table person(id int not null, name varchar(256) not null, organization_id bigint, group_id bigint)".execute
          .apply()

        val issue1 = Issue(1L, "ユーザ1", 1L)
        val issue2 = Issue(2L, "ユーザ2", 1L)
        val issue3 = Issue(3L, "ユーザ3", 2L)
        val org1 = new Organization(1L, "http://jp.scala-users.org/")
        val org2 = new Organization(2L, "https://www.java-users.jp/")

        val (ic, oc, pc) =
          (IssueTable.column, Organization.column, Person.column)

        Seq(
          insert.into(IssueTable).namedValues(autoNamedValues(issue1, ic)),
          insert.into(IssueTable).namedValues(autoNamedValues(issue2, ic)),
          insert.into(IssueTable).namedValues(autoNamedValues(issue3, ic)),
          insert.into(Organization).namedValues(autoNamedValues(org1, oc)),
          insert.into(Organization).namedValues(autoNamedValues(org2, oc)),
          insert
            .into(Person)
            .columns(pc.id, pc.name, pc.organizationId, pc.groupId)
            .values(1L, "person1", Some(1L), 1L),
          insert
            .into(Person)
            .columns(pc.id, pc.name, pc.organizationId, pc.groupId)
            .values(2L, "person2", None, 1L)
        ).foreach(sql => applyUpdate(sql))

        val (i, o, p) =
          (IssueTable.syntax("i"), Organization.syntax("o"), Person.syntax("p"))

        val i1 = withSQL { select.from(IssueTable as i).where.eq(i.id, 1) }
          .map(IssueTable(i))
          .single
          .apply()
        i1 should equal(Some(issue1))
        val i2 = withSQL { select.from(IssueTable as i).where.eq(i.id, 2) }
          .map(IssueTable(i.resultName))
          .single
          .apply()
        i2 should equal(Some(issue2))

        val o1 = withSQL { select.from(Organization as o).where.eq(o.id, 1) }
          .map(Organization(o))
          .single
          .apply()
        o1.map(_.id) should equal(Some(org1.id))
        o1.map(_.websiteUrl) should equal(Some(org1.websiteUrl))
        val o2 = withSQL { select.from(Organization as o).where.eq(o.id, 2) }
          .map(Organization(o.resultName))
          .single
          .apply()
        o2.map(_.id) should equal(Some(org2.id))
        o2.map(_.websiteUrl) should equal(Some(org2.websiteUrl))

        val p1 = withSQL {
          select
            .from(Person as p)
            .leftJoin(Organization as o)
            .on(p.organizationId, o.id)
            .where
            .eq(p.id, 1)
        }.map(Person(p)).single.apply()
        p1.flatMap(_.organizationId) should equal(Some(1L))

      } finally {
        try sql"drop table issue".execute.apply()
        catch { case ignore: Exception => }
        try sql"drop table organization".execute.apply()
        catch { case ignore: Exception => }
        try sql"drop table person".execute.apply()
        catch { case ignore: Exception => }
      }
    }
  }

  it should "throw a compiler error if wrong column passed" in {
    val org1 = new Organization(1L, "http://jp.scala-users.org/")
    val pc = Person.column
    """autoNamedValues[Organization](org1, pc)""" shouldNot compile
  }
}
