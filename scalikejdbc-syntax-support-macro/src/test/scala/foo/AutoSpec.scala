package foo

import org.scalatest._
import scalikejdbc._

class AutoSpec extends FlatSpec with Matchers with DBSettings {

  case class Issue(id: Long, firstName: String, groupId: Long)
  val IssueTable = SQLSyntaxSupportFactory[Issue]()

  class Organization(val id: Long, val websiteUrl: String)
  object Organization extends SQLSyntaxSupport[Organization] {
    def apply(s: SyntaxProvider[Organization])(rs: WrappedResultSet): Organization = autoConstruct(rs, s)
    def apply(r: ResultName[Organization])(rs: WrappedResultSet): Organization = autoConstruct(rs, r)
  }

  case class Person(id: Long, name: String, organizationId: Option[Long], organization: Option[Organization] = None, gorupId: Long = 0)
  object Person extends SQLSyntaxSupport[Person] {
    def apply(s: SyntaxProvider[Person])(rs: WrappedResultSet): Person = autoConstruct(rs, s, "organization")
  }

  behavior of "autoConstruct"

  it should "execute" in {
    DB autoCommit { implicit s =>
      try {
        try sql"drop table issue".execute.apply() catch { case ignore: Exception => }
        sql"create table issue (id int not null, first_name varchar(256), group_id int)".execute.apply()

        try sql"drop table organization".execute.apply() catch { case ignore: Exception => }
        sql"create table organization (id int not null, website_url varchar(256))".execute.apply()

        val issue1 = Issue(1L, "ユーザ1", 1L)
        val issue2 = Issue(2L, "ユーザ2", 1L)
        val issue3 = Issue(3L, "ユーザ3", 2L)
        val org1 = new Organization(1L, "http://jp.scala-users.org/")
        val org2 = new Organization(2L, "http://http://www.java-users.jp/")

        val (ic, oc) = (IssueTable.column, Organization.column)

        Seq(
          insert.into(IssueTable).columns(ic.id, ic.firstName, ic.groupId).values(issue1.id, issue1.firstName, issue1.groupId),
          insert.into(IssueTable).columns(ic.id, ic.firstName, ic.groupId).values(issue2.id, issue2.firstName, issue2.groupId),
          insert.into(IssueTable).columns(ic.id, ic.firstName, ic.groupId).values(issue3.id, issue3.firstName, issue3.groupId),
          insert.into(Organization).columns(oc.id, oc.websiteUrl).values(org1.id, org1.websiteUrl),
          insert.into(Organization).columns(oc.id, oc.websiteUrl).values(org2.id, org2.websiteUrl)
        ).foreach(sql => applyUpdate(sql))

        val (i, o) = (IssueTable.syntax("i"), Organization.syntax("o"))

        val i1 = withSQL { select.from(IssueTable as i).where.eq(i.id, 1) }.map(IssueTable(i)).single().apply()
        i1 should equal(Some(issue1))
        val i2 = withSQL { select.from(IssueTable as i).where.eq(i.id, 2) }.map(IssueTable(i.resultName)).single().apply()
        i2 should equal(Some(issue2))

        val o1 = withSQL { select.from(Organization as o).where.eq(o.id, 1) }.map(Organization(o)).single().apply()
        o1.map(_.id) should equal(Some(org1.id))
        o1.map(_.websiteUrl) should equal(Some(org1.websiteUrl))
        val o2 = withSQL { select.from(Organization as o).where.eq(o.id, 2) }.map(Organization(o.resultName)).single().apply()
        o2.map(_.id) should equal(Some(org2.id))
        o2.map(_.websiteUrl) should equal(Some(org2.websiteUrl))

      } finally {
        try sql"drop table issue".execute.apply() catch { case ignore: Exception => }
        try sql"drop table organization".execute.apply() catch { case ignore: Exception => }
      }
    }
  }

  it should "throw a compiler error if unknown field name is decleared in excludes" in {
    """autoConstruct[Organization](rs, s, "organization", "test")""" shouldNot compile
    """autoConstruct[Organization](rs, s, s"organization")""" shouldNot compile
  }

}
