package basic_test.accounts

import org.joda.time.DateTime
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ Tag => _ }
import scalikejdbc._
import scalikejdbc.orm.Pagination
import scalikejdbc.orm.JodaTimeImplicits._
import scalikejdbc.orm.exception.OptimisticLockException
import scalikejdbc.orm.strongparameters.{
  ParamType,
  PermittedStrongParameters,
  StrongParameters
}
import util.{ DBSeeds, LightFactoryGirl }

class AccountDatabaseSpec extends AnyFunSpec with Matchers with DBSeeds {

  Class.forName("org.h2.Driver")
  ConnectionPool.singleton(
    "jdbc:h2:mem:skinny-mapper-test;MODE=PostgreSQL",
    "sa",
    "sa"
  )

  addSeedSQL(
    sql"""
create table members (
  id bigint auto_increment primary key not null,
  country_id bigint not null,
  company_id bigint,
  mentor_id bigint,
  created_at timestamp not null
)
""",
    sql"""
create table members2 (
  id bigint auto_increment primary key not null,
  country_id bigint not null,
  company_id bigint,
  mentor_id bigint,
  created_at timestamp not null
)
""",
    sql"""
create table names (
  member_id bigint primary key not null,
  first varchar(64) not null,
  last varchar(64) not null,
  created_at timestamp not null,
  updated_at timestamp
)
""",
    sql"""
create table countries (
  id bigint auto_increment primary key not null,
  name varchar(255) not null
)
""",
    sql"""
create table companies (
  id bigint auto_increment primary key not null,
  name varchar(255) not null,
  country_id bigint,
  is_deleted boolean default false not null
)
""",
    sql"""
create table groups (
  id bigint auto_increment primary key not null,
  name varchar(255) not null,
  deleted_at timestamp
)
""",
    sql"""
create table groups_members (
  group_id bigint not null,
  member_id bigint not null
)
""",
    sql"""
create table skills (
  id bigint auto_increment primary key not null,
  name varchar(255) not null,
  created_at timestamp not null,
  updated_at timestamp,
  -- lock_version bigint not null default 1
  lock_version bigint not null
)
""",
    sql"""
create table members_skills (
  member_id bigint not null,
  skill_id bigint not null
)
""",
    sql"""
create table books (
  isbn varchar(64) not null,
  title varchar(255) not null,
  description varchar(1024)
)
""",
    sql"""
create table isbn_master (
  isbn varchar(64) not null,
  publisher varchar(255) not null
)
""",
    sql"""
create table products (
  id bigint auto_increment primary key not null,
  name varchar(255) not null,
  price_yen bigint not null
)
""",
    sql"""
create table tag (
  tag varchar(255) primary key not null
)
""",
    sql"""
create table tag_description (
  tag varchar(255) primary key not null,
  description varchar(1024) not null
)
""",
    sql"""
create table tag2 (
  tag varchar(255) primary key not null
)
""",
    sql"""
create table tag_description2 (
  tag varchar(255) primary key not null,
  description varchar(1024) not null
)
""",
    sql"""
create table legacy_accounts (
  account_code varchar(12) not null,
  user_id int,
  name varchar(255)
)
""",
    sql"""
create table table1 (
  num int not null,
  name varchar(128) not null
)
""",
    sql"""
create table table2 (
  label varchar(128) not null
)
"""
  )

  runIfFailed(sql"select * from members")

  def fixture(implicit session: DBSession): Unit = {

    // #createWithNamedValues
    val countryId1 = Country.createWithAttributes("name" -> "Japan")
    val countryId2 = Country.createWithAttributes("name" -> "China")

    val groupId1 =
      GroupMapper.createWithAttributes("name" -> "Scala Users Group")
    val groupId2 = GroupMapper.createWithAttributes("name" -> "Java Group")

    val companyId = Company.createWithAttributes(
      "name" -> "Typesafe",
      "countryId" -> countryId1
    )

    Member.withColumns { m =>
      // Member doesn't use TimestampsFeature
      val alice = Member.createWithNamedValues(
        m.countryId -> countryId1,
        m.companyId -> companyId,
        m.createdAt -> DateTime.now
      )
      val bob = Member.createWithNamedValues(
        m.countryId -> countryId2,
        m.mentorId -> alice,
        m.createdAt -> DateTime.now
      )
      val chris = Member.createWithNamedValues(
        m.countryId -> countryId1,
        m.mentorId -> alice,
        m.createdAt -> DateTime.now
      )

      Name.createWithAttributes(
        "memberId" -> alice,
        "first" -> "Alice",
        "last" -> "Cooper"
      )
      Name.createWithAttributes(
        "memberId" -> bob,
        "first" -> "Bob",
        "last" -> "Marley"
      )
      Name.createWithAttributes(
        "memberId" -> chris,
        "first" -> "Chris",
        "last" -> "Birchall"
      )

      GroupMember.createWithAttributes(
        "memberId" -> alice,
        "groupId" -> groupId1
      )
      GroupMember.createWithAttributes("memberId" -> bob, "groupId" -> groupId1)
      GroupMember.createWithAttributes("memberId" -> bob, "groupId" -> groupId2)

      val skillId = Skill.createWithAttributes("name" -> "Programming")
      Skill.updateById(skillId).withAttributes("name" -> "Web development")
      MemberSkill.createWithAttributes(
        "memberId" -> alice,
        "skillId" -> skillId
      )
    }
  }

  def db = NamedDB("")

  def permit(params: StrongParameters): PermittedStrongParameters = {
    params.permit(
      "countryId" -> ParamType.Int,
      "mentorId" -> ParamType.Int,
      "createdAt" -> ParamType.DateTime
    )
  }

  describe("The features") {
    it("should work") {
      DB.localTx { implicit s =>
        fixture(using s)

        Member.connectionPool should not be (null)
        Member.connectionPoolName should equal(ConnectionPool.DEFAULT_NAME)

        {
          // dynamic table
          val createdAt = DateTime.now
          val createdId = Member.withColumns { m =>
            Member
              .withTableName("members2")
              .createWithNamedValues(
                m.countryId -> Country.limit(1).apply().head.id,
                m.companyId -> Company.limit(1).apply().head.id,
                m.createdAt -> createdAt
              )
          }
          Member
            .withTableName("members2")
            .where(
              "id" -> createdId,
              "createdAt" -> createdAt
            )
            .apply()
            .exists(_.id == createdId) should be(true)
          Member
            .where(
              "id" -> createdId,
              "createdAt" -> createdAt
            )
            .apply()
            .exists(_.id == createdId) should be(false)
        }

        {
          // count
          Member.count() should be > (0L)
        }

        {
          // count(String)
          Member.count("countryId", false) should equal(3L)
          Member.count("countryId", true) should equal(2L)
        }

        {
          // distinctCount
          Member.distinctCount() should equal(3L)
          Member.distinctCount("countryId") should equal(2L)
        }

        // TODO
//        {
//          // countBy
//          val countryId = Country.where("name" -> "Japan").apply().head.id
//          Member.withAlias(s =>
//            Member.countBy(sqls.eq(s.countryId, countryId))
//          ) should be > (0L)
//        }

        {
          // #countBy(SQLSyntax) with associations (byDefault)
          val n = Name.defaultAlias
          val mn = Member.mentorAlias
          val aliceId = Member
            .findAllBy(sqls.eq(n.first, "Alice").and.eq(n.last, "Cooper"))
            .apply(0)
            .id

          Member.countBy(sqls.eq(mn.id, aliceId)) should be > (0L)
        }

        // TODO
//        {
//          // #countBy(SQLSyntax) with associations
//          val skillName = Skill.limit(1).offset(0).apply().map(_.name).head
//          val s = Skill.defaultAlias
//          Member
//            .joins[Long](Member.skillsSimpleRef)
//            .countBy(sqls.eq(s.name, skillName)) should be > (0L)
//        }

        {
          // #belongsTo, #hasOne
          Member.withAlias { m =>
            val members = Member.findAll()
            members.size should be > (0)

            val alice = members.filter(_.name.get.first == "Alice").head
            alice.mentor.isDefined should be(false)
            alice.country.name should equal("Japan")

            val bob = members.filter(_.name.get.first == "Bob").head
            bob.mentor.isDefined should be(true)
            bob.country.name should equal("China")
          }
        }

        {
          // #hasMany
          Member.withAlias { m =>
            val members = Member.findAll()
            val membersByIds = Member.where("id" -> members.map(_.id)).apply()
            membersByIds.size should equal(members.size)
            Member.where("id" -> members.map(_.id)).count() should equal(
              members.size
            )

            val withGroups = members.filter(_.name.get.first == "Bob").head
            withGroups.groups.size should equal(2)
            val withoutGroups =
              members.filter(_.name.get.first == "Chris").head
            withoutGroups.groups.size should equal(0)

            val alice = members.filter(_.name.get.first == "Alice").head
            alice.mentorees.size should equal(2)
            val bob = members.filter(_.name.get.first == "Bob").head
            bob.mentorees.size should equal(0)

            {
              // skills should be empty
              val withSkills =
                members.filter(_.name.get.first == "Alice").head
              withSkills.skills.size should equal(0)
              val withoutSkills =
                members.filter(_.name.get.first == "Chris").head
              withoutSkills.skills.size should equal(0)
            }

            // simple hasManyThrough definition
            {
              val membersWithSkills =
                Member.joins(Member.skillsSimpleRef).findAll()
              val withSkills =
                membersWithSkills.filter(_.name.get.first == "Alice").head
              withSkills.skills.size should equal(1)
              val withoutSkills =
                membersWithSkills.filter(_.name.get.first == "Chris").head
              withoutSkills.skills.size should equal(0)
            }

            {
              val membersWithSkills =
                Member
                  .joins(Member.skillsSimpleRef)
                  .where("id" -> Member.findAll().map(_.id))
                  .apply()
              val withSkills =
                membersWithSkills.filter(_.name.get.first == "Alice").head
              withSkills.skills.size should equal(1)
              val withoutSkills =
                membersWithSkills.filter(_.name.get.first == "Chris").head
              withoutSkills.skills.size should equal(0)
            }

            // verbose hasManyThrough definition
            {
              val membersWithSkills =
                Member.joins(Member.skillsVerboseRef).findAll()
              val withSkills =
                membersWithSkills.filter(_.name.get.first == "Alice").head
              withSkills.skills.size should equal(1)
              val withoutSkills =
                membersWithSkills.filter(_.name.get.first == "Chris").head
              withoutSkills.skills.size should equal(0)
            }

            {
              val membersWithSkills =
                Member
                  .joins(Member.skillsVerboseRef)
                  .where("id" -> Member.findAll().map(_.id))
                  .apply()
              val withSkills =
                membersWithSkills.filter(_.name.get.first == "Alice").head
              withSkills.skills.size should equal(1)
              val withoutSkills =
                membersWithSkills.filter(_.name.get.first == "Chris").head
              withoutSkills.skills.size should equal(0)
            }

          }
        }

        {
          val l = LegacyAccount.defaultAlias
          val c = LegacyAccount.column

          {
            val before = LegacyAccount.count()
            val before2 = LegacyAccount2.count()
            LegacyAccount.createWithAttributes(
              "accountCode" -> "foo",
              "userId" -> None,
              "name" -> "Alice"
            )
            val after = LegacyAccount.count()
            val after2 = LegacyAccount2.count()
            after should equal(before + 1)
            after2 should equal(before2 + 1)
            before should equal(before2)
            after should equal(after2)
          }

          LegacyAccount.createWithAttributes(
            "accountCode" -> "sera",
            "userId" -> Some(123),
            "name" -> "Sera"
          )
          LegacyAccount.createWithAttributes(
            "accountCode" -> "kaz",
            "userId" -> Some(333),
            "name" -> "Kaz"
          )

          val accounts = LegacyAccount.findAll()
          val accounts2 = LegacyAccount2.findAll()
          accounts.size should equal(accounts2.size)
          accounts.size should equal(3)

          {
            LegacyAccount
              .findBy(sqls.eq(l.name, "Alice"))
              .get
              .accountCode should equal("foo")
            LegacyAccount2
              .findBy(sqls.eq(l.name, "Alice"))
              .get
              .accountCode should equal("foo")
            LegacyAccount
              .updateBy(sqls.eq(c.name, "Alice"))
              .withAttributes("accountCode" -> "bar")
            LegacyAccount
              .findBy(sqls.eq(l.name, "Alice"))
              .get
              .accountCode should equal("bar")
            LegacyAccount2
              .findBy(sqls.eq(l.name, "Alice"))
              .get
              .accountCode should equal("bar")
          }

          {
            LegacyAccount.deleteBy(
              sqls.eq(c.accountCode, "sera").and.eq(c.userId, 123)
            )
            LegacyAccount.count() should equal(2)
            LegacyAccount2.count() should equal(2)

            LegacyAccount.deleteAll()
            LegacyAccount.count() should equal(0)
            LegacyAccount2.count() should equal(0)
          }
        }

        {
          // joins
          Member.joins(Member.companyOpt).connectionPool should not be (null)
          Member.joins(Member.companyOpt).connectionPoolName should equal(
            ConnectionPool.DEFAULT_NAME
          )
        }

        {
          // auto session
          Member.autoSession should equal(AutoSession)
          Member.joins(Member.companyOpt).autoSession should equal(AutoSession)
        }

        {
          // dynamic table
          Member.withTableName("legacy_members") should not be (null)
          Member
            .joins(Member.companyOpt)
            .withTableName("legacy_members") shouldNot be(null)
        }

        {
          // findById
          val memberIds = Member.findAll().map(_.id)
          val member = Member.findById(memberIds.head)
          member.isDefined should be(true)

          val nonExistingId = (1 to 10000).find(i => !memberIds.contains(i)).get
          val none = Member.findById(nonExistingId)
          none.isDefined should be(false)
        }

        // It should behave like `ActiveRecord::Base.find_by(...)`
        {
          // findBy
          val m = Member.defaultAlias
          val mentor = Member.findAll().head
          val now = DateTime.now

          val member1Id = Member.createWithAttributes(
            "countryId" -> mentor.countryId,
            "companyId" -> mentor.companyId,
            "mentorId" -> mentor.id,
            "createdAt" -> now
          )
          val member2Id = Member.createWithAttributes(
            "countryId" -> mentor.countryId,
            "companyId" -> mentor.companyId,
            "mentorId" -> mentor.id,
            "createdAt" -> now
          )

          val member = Member.findBy(
            sqls
              .eq(m.countryId, mentor.countryId)
              .and
              .eq(m.companyId, mentor.companyId)
              .and
              .eq(m.mentorId, mentor.id)
          )

          // depends on default ordering
          member.get.id should (
            equal(member1Id) or
              equal(member2Id)
          )
        }

        {
          // nested belongsTo relations
          val m = Member.defaultAlias

          val member = Member
            .findAllByWithLimitOffset(sqls.isNotNull(m.companyId), 1, 0)
            .head
          member.company.get.country.isDefined should be(false)

          val member2 = Member
            .findAllByWithPagination(
              sqls.isNotNull(m.companyId),
              Pagination.page(1).per(1)
            )
            .head
          member2.company.get.country.isDefined should be(false)

          val memberWithCountry =
            Member
              .includes(Member.companyOpt)
              .findAllByWithLimitOffset(sqls.isNotNull(m.companyId), 1, 0)
              .head
          memberWithCountry.company.get.country.isDefined should be(true)
        }

        {
          // nested hasMany relations
          val company = Company
            .joins(Company.members)
            .findAll()
            .find(_.members.size > 0)
            .head
          company.members.head.name.isDefined should be(false)

          val includedCompany = Company
            .includes(Company.members)
            .findAll()
            .find(_.members.size > 0)
            .head
          includedCompany.members.head.name.isDefined should be(true)
        }

        {
          // empty string is often passed from controllers
          Member.createWithPermittedAttributes(
            permit(
              StrongParameters(
                Map(
                  "countryId" -> Country.findAll().head.id,
                  "mentorId" -> "",
                  "createdAt" -> DateTime.now
                )
              )
            )
          )

          Member.createWithPermittedAttributes(
            permit(
              StrongParameters(
                Map(
                  "countryId" -> Country.findAll().head.id,
                  "mentorId" -> null,
                  "createdAt" -> DateTime.now
                )
              )
            )
          )

          Member.createWithPermittedAttributes(
            permit(
              StrongParameters(
                Map(
                  "countryId" -> Country.findAll().head.id,
                  "mentorId" -> None,
                  "createdAt" -> DateTime.now
                )
              )
            )
          )
        }

        {
          val (t1, t2) = (Table1.defaultAlias, Table2.defaultAlias)

          Table1.createWithAttributes("num" -> 1, "name" -> "Java")
          Table1.createWithAttributes("num" -> 2, "name" -> "Scala")

          Table2.createWithAttributes("label" -> "Java")

          {
            val java = Table2.findBy(sqls.eq(t2.label, "Java"))
            java.isDefined should equal(true)
            java.get.table1 should equal(Some(Table1(1, "Java")))
          }

          {
            val java =
              Table1.joins(Table1.table2).findBy(sqls.eq(t1.name, "Java"))
            java.isDefined should equal(true)
            java.get.table2 should equal(Some(Table2("Java")))
          }
        }

        {
          // Timestamps
          val id1 = Skill.createWithAttributes("name" -> "Scala")
          val id2 = Skill.createWithNamedValues(Skill.column.name -> "Java")

          Skill.where("id" -> Seq(id1, id2)).apply().foreach { skill =>
            skill.createdAt should not be (null)
            skill.updatedAt should not be (null)
          }

          Thread.sleep(100L)

          Skill.updateById(id1).withAttributes("name" -> "Scala Programming")
          Skill
            .updateById(id2)
            .withNamedValues(Skill.column.name -> "Java Programming")

          Skill.where("id" -> Seq(id1, id2)).apply().foreach { skill =>
            skill.updatedAt should not equal (skill.createdAt)
          }
        }

        {
          // Optimistic Lock
          val skill = LightFactoryGirl(Skill).create()

          // with optimistic lock
          Skill
            .updateByIdAndVersion(skill.id, skill.lockVersion)
            .withAttributes("name" -> "Java Programming")
          intercept[OptimisticLockException] {
            Skill
              .updateByIdAndVersion(skill.id, skill.lockVersion)
              .withAttributes("name" -> "Ruby Programming")
          }
          // without lock
          Skill
            .updateById(skill.id)
            .withAttributes("name" -> "Ruby Programming")
        }

        {
          // findAll
          Member.findAll().size should be > (0)
          val m = Member.defaultAlias
          Member
            .findAll(orderings = Seq(sqls"${m.id}, ${m.createdAt} desc"))
            .size should be > (0)
        }

        // http://api.rubyonrails.org/classes/ActiveRecord/Calculations.html
        {
          val id = Product.createWithAttributes(
            "name" -> "How to learn Scala",
            "priceYen" -> 1230
          )
          Product.createWithAttributes(
            "name" -> "How to learn Scala 2",
            "priceYen" -> 1800
          )

          val p = Product.defaultAlias

          Product.count() should equal(2)
          Product.where(sqls.eq(p.id, id.value)).count() should equal(1)

          Product
            .where(sqls.isNotNull(p.priceYen))
            .sum("priceYen") should equal(3030)
          Product.sum("priceYen") should equal(3030)

          // NOTICE: H2 and others returns value without decimal part.
          // https://hibernate.atlassian.net/browse/HHH-5173
          Product.average("priceYen") should equal(1515)
          Product.average("priceYen", Some(2)) should equal(1515)
          Product.minimum("priceYen") should equal(1230)
          Product.maximum("priceYen") should equal(1800)

          Product.avg("priceYen") should equal(1515)
          Product.avg("priceYen", Some(3)) should equal(1515)
          Product.min("priceYen") should equal(1230)
          Product.max("priceYen") should equal(1800)

          Product
            .where(sqls.isNotNull(p.priceYen))
            .average("priceYen") should equal(1515)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .average("priceYen", Some(2)) should equal(1515)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .minimum("priceYen") should equal(1230)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .maximum("priceYen") should equal(1800)

          Product
            .where(sqls.isNotNull(p.priceYen))
            .avg("priceYen") should equal(1515)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .avg("priceYen", Some(3)) should equal(1515)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .min("priceYen") should equal(1230)
          Product
            .where(sqls.isNotNull(p.priceYen))
            .max("priceYen") should equal(1800)
        }

        {
          // findAllBy(SQLSyntax, Int, Int)
          val countryId = Country.findAll().map(_.id).head
          Member.withAlias { m =>
            Member
              .findAllBy(sqls.eq(m.countryId, countryId))
              .size should be > (0)
            Member
              .findAllByWithLimitOffset(sqls.eq(m.countryId, countryId), 1, 0)
              .size should equal(1)
            Member
              .findAllByWithPagination(
                sqls.eq(m.countryId, countryId),
                Pagination.page(1).per(1)
              )
              .size should equal(1)

            val ordering = sqls"${m.id}, ${m.createdAt} desc"
            Member
              .findAllBy(sqls.eq(m.countryId, countryId), Seq(ordering))
              .size should be > (0)
            Member
              .findAllByWithLimitOffset(
                sqls.eq(m.countryId, countryId),
                1,
                0,
                Seq(ordering)
              )
              .size should equal(1)
            Member
              .findAllByWithPagination(
                sqls.eq(m.countryId, countryId),
                Pagination.page(1).per(1),
                Seq(ordering)
              )
              .size should equal(1)
            // Removed since 2.0.0
            // Member.findAllByPaging(sqls.eq(m.countryId, countryId), 1, 0).size should equal(1)
            // Member.findAllByPaging(sqls.eq(m.countryId, countryId), 1, 0, Seq(ordering)).size should equal(1)
          }
        }

        {
          // #updateById(Long)
          val countryId = Country.limit(1).offset(0).apply().map(_.id).head
          val memberId = Member.createWithAttributes(
            "countryId" -> countryId,
            "createdAt" -> DateTime.now
          )
          val mentorId = Member.limit(1).offset(0).apply().head.id
          Member.updateById(memberId).withAttributes("mentorId" -> mentorId)
          val updated = Member.findById(memberId)
          updated.get.mentorId should equal(Some(mentorId))
        }

        {
          // #deleteById(Long)
          val countryId = Country.limit(1).offset(0).apply().map(_.id).head
          val memberId = Member.createWithAttributes(
            "countryId" -> countryId,
            "createdAt" -> DateTime.now
          )
          Member.deleteById(memberId)
        }

        {
          // #createWithPermittedAttributes
          val minutes = java.util.TimeZone.getDefault.getRawOffset / 1000 / 60
          val prefix = if (minutes >= 0) "+" else "-"
          val timeZone = prefix + "%02d:%02d".format(
            (math.abs(minutes) / 60),
            (math.abs(minutes) % 60)
          )
          val minus2hours = minutes - 120
          val minus2hoursPrefix = if (minus2hours >= 0) "+" else "-"
          val minus2hoursTimeZone = minus2hoursPrefix + "%02d:%02d".format(
            (math.abs(minus2hours) / 60),
            (math.abs(minus2hours) % 60)
          )
          Seq(
            s"2013-01-02T03:04:05${timeZone}",
            s"2013-01-02T01:04:05${minus2hoursTimeZone}",
            s"2013/01/02T03:04:05${timeZone}",
            s"2013-01-02 03:04:05${timeZone}",
            s"2013-01-02T03:04:05",
            s"2013-01-02 03:04:05",
            s"2013/1/2 3:4:5",
            s"2013-1-2 03:4:05",
            s"2013-01-02 03-04-05"
          ) foreach { createdAt =>
            val params = StrongParameters(
              Map("name" -> "Java Programming", "createdAt" -> createdAt)
            )
            val id = Skill.createWithPermittedAttributes(
              params.permit(
                "name" -> ParamType.String,
                "createdAt" -> ParamType.DateTime
              )
            )
            val created = Skill.findById(id)
            created.get.createdAt should equal(
              new DateTime(2013, 1, 2, 3, 4, 5)
            )
          }

          {
            // throw exception for invalid datetime format
            Seq(
              "2013-a-b 03-04-05",
              "2013-01-02 0c:04:05"
            ) foreach { createdAt =>
              intercept[Exception] {
                Skill.createWithPermittedAttributes(
                  StrongParameters(
                    Map("name" -> "Java Programming", "createdAt" -> createdAt)
                  )
                    .permit(
                      "name" -> ParamType.String,
                      "createdAt" -> ParamType.DateTime
                    )
                )
              }
            }
          }

          {
            // querying
            val allMembers = Member.findAll()

            val c = Country.defaultAlias
            val japan = Country
              .where("name" -> "Japan")
              .orderBy(c.id.desc)
              .limit(1000)
              .offset(0)
              .apply()
              .head
            val expected = allMembers.filter(_.countryId == japan.id)

            val m = Member.defaultAlias
            val actual = Member
              .where(sqls.eq(m.countryId, japan.id))
              .limit(1000)
              .offset(0)
              .apply()
            actual should equal(expected)
          }

          {
            // #orderBy in Querying APIs
            val id1 = Skill.createWithAttributes("name" -> "Skill_B")
            val id2 = Skill.createWithAttributes("name" -> "Skill_A")
            val id3 = Skill.createWithAttributes("name" -> "Skill_B")
            val s = Skill.defaultAlias
            val ids = Skill
              .where("id" -> Seq(id1, id2, id3))
              .orderBy(s.name.asc, s.id.desc)
              .apply()
              .map(_.id)
            ids should equal(Seq(id2, id3, id1))
          }

          {
            // #paginate in Querying APIs
            Seq("America", "Russia", "Korea", "India", "Brazil").foreach {
              name =>
                Country.createWithAttributes("name" -> name)
            }
            val res1 = Country.limit(3).offset(3).apply().map(_.id)
            val res2 =
              Country.paginate(Pagination.page(2).per(3)).apply().map(_.id)
            res1 should equal(res2)
          }

          {
            // withId
            val book3 =
              LightFactoryGirl(Book).create(
                "isbn" -> ISBN("aaaa-bbbb-cccc"),
                "title" -> "Play3 in Action"
              )
            book3.isbn should equal(ISBN("aaaa-bbbb-cccc"))
            book3.title should equal("Play3 in Action")

            val isbn: ISBN =
              Book.createWithAttributes("title" -> "ScalikeJDBC Cookbook")
            ISBNMaster.createWithAttributes(
              "isbn" -> isbn,
              "publisher" -> "O'Reilly"
            )
            LightFactoryGirl(ISBNMaster)
              .withVariables("isbn" -> java.util.UUID.randomUUID)
              .create()

            val newBook = Book.findById(isbn).get
            newBook.title should equal("ScalikeJDBC Cookbook")
            newBook.isbnMaster.map(_.publisher) should equal(Some("O'Reilly"))

            Book.createWithAttributes("title" -> "Skinny Framework in Action")
            Book.findAll().size should equal(3)

            Book
              .updateById(isbn)
              .withAttributes("title" -> "ScalikeJDBC Cookbook 2")
            Book.findById(isbn).map(_.title) should equal(
              Some("ScalikeJDBC Cookbook 2")
            )

            Book.deleteById(isbn)
            Book.findById(isbn) should equal(None)
            Book.count() should equal(2)
          }

          {
            val skill = LightFactoryGirl(Skill).create()

            // with optimistic lock
            Skill.deleteByIdAndVersion(skill.id, skill.lockVersion)
            intercept[OptimisticLockException] {
              Skill.deleteByIdAndVersion(skill.id, skill.lockVersion)
            }
            // without lock
            Skill.deleteById(skill.id)
          }

          {
            // update with lock timestamp
            val member = LightFactoryGirl(Member)
              .withVariables(
                "countryId" -> LightFactoryGirl(Country, "countryyy")
                  .create()
                  .id
              )
              .create(
                "companyId" -> LightFactoryGirl(Company).create().id,
                "createdAt" -> DateTime.now
              )
            val name = LightFactoryGirl(Name).create("memberId" -> member.id)

            // with optimistic lock
            Name
              .updateByIdAndTimestamp(name.memberId, name.updatedAt)
              .withAttributes("first" -> "Kaz")
            /* TODO: this test w/ Scala 3 + Windows OS somehow fails
            intercept[OptimisticLockException] {
              Name
                .updateByIdAndTimestamp(name.memberId, name.updatedAt)
                .withAttributes("first" -> "Kaz")
            }
             */
            // without lock
            Name.updateById(name.memberId).withAttributes("first" -> "Kaz")
          }

          {
            // delete with lock timestamp
            val member = LightFactoryGirl(Member)
              .withVariables(
                "countryId" -> LightFactoryGirl(Country, "countryyy")
                  .create()
                  .id
              )
              .create(
                "companyId" -> LightFactoryGirl(Company).create().id,
                "createdAt" -> DateTime.now
              )
            val name = LightFactoryGirl(Name).create("memberId" -> member.id)

            // with optimistic lock
            Name.deleteByIdAndOptionalTimestamp(name.memberId, name.updatedAt)
            intercept[OptimisticLockException] {
              Name.deleteByIdAndOptionalTimestamp(name.memberId, name.updatedAt)
            }
            // without lock
            Name.deleteById(name.memberId)
          }

          {
            // deal with typed auto-increment value
            // using typed auto-increment value
            val productId: ProductId =
              Product.createWithAttributes(
                "name" -> "How to learn Scala",
                "priceYen" -> 2000
              )
            Product.findById(productId).map(_.name) should equal(
              Some("How to learn Scala")
            )

            Product.deleteById(productId)
            Product.findById(productId) should equal(None)

            // since h2 database 1.4.197, setting a specific value for an auto-generated column is no longer allowed.
            val productId2: ProductId = Product.createWithAttributes(
              /*'id -> ProductId(777), */
              "name" -> "How to learn Ruby",
              "priceYen" -> 1800
            )
            Product.findById(productId2).map(_.name) should equal(
              Some("How to learn Ruby")
            )

            Product.updateById(productId2).withAttributes("priceYen" -> 1950)
            Product.findById(productId2).map(_.priceYen) should equal(
              Some(1950)
            )
          }

          {
            // No Id

            val (td, c) = (TagDescription2.defaultAlias, TagDescription2.column)
            val td1 = insert
              .into(TagDescription2)
              .namedValues(
                c.tag -> "Scala",
                c.description -> "Programming Language"
              )
              .toSQL
              .update
              .apply()
            val td2 = insert
              .into(TagDescription2)
              .namedValues(
                c.tag -> "ScalikeJDBC",
                c.description -> "Database Access Library"
              )
              .toSQL
              .update
              .apply()
            (td1, td2) should equal((1, 1))

            TagDescription2
              .where(sqls.eq(td.tag, "Scala"))
              .apply()
              .head
              .description should equal("Programming Language")
            val tds = TagDescription2.limit(10).apply()
            tds.size should equal(2)

            val (t, tc) = (Tag2.defaultAlias, Tag2.column)
            val t1 = insert
              .into(Tag2)
              .namedValues(tc.tag -> "Scala")
              .toSQL
              .update
              .apply()
            t1 should equal(1)

            val scalaTag = Tag2.where(sqls.eq(t.tag, "Scala")).apply().head
            scalaTag.tag should equal("Scala")
            scalaTag.description should equal(
              Some(TagDescription2("Scala", "Programming Language"))
            )
          }

          {
            // deleteAll
            Product.deleteAll()
            MemberSkill.deleteAll()
          }
        }
      }
    }
  }
}
