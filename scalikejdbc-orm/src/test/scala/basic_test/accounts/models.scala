package basic_test.accounts

import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import scalikejdbc._
import scalikejdbc.orm.{
  Alias,
  CRUDMapper,
  CRUDMapperWithId,
  DataMapper,
  JoinTable,
  NoIdCRUDMapper,
  NoIdDataMapper
}
import scalikejdbc.orm.associations.Association
import scalikejdbc.orm.optimisticlock.{
  OptimisticLockWithTimestampFeature,
  OptimisticLockWithVersionFeature
}
import scalikejdbc.orm.softdeletion.{
  SoftDeleteWithBooleanFeature,
  SoftDeleteWithTimestampFeature
}
import scalikejdbc.orm.timstamps.TimestampsFeature

case class Member(
  id: Long,
  name: Option[Name] = None,
  countryId: Long,
  mentorId: Option[Long],
  companyId: Option[Long],
  createdAt: DateTime,
  country: Country,
  company: Option[Company] = None,
  mentor: Option[Member] = None,
  mentorees: Seq[Member] = Nil,
  groups: Seq[Group] = Nil,
  skills: Seq[Skill] = Nil
)

object Member extends CRUDMapper[Member] {
  override val tableName = "members"
  override val defaultAlias = createAlias("m")
  val mentorAlias = createAlias("mentor")
  val mentoreeAlias = createAlias("mentoree")

  // if you use hasOne, joined entity should be Option[Entity]
  // this code should be here
  innerJoinWithDefaults(
    Country,
    (m, c) => sqls.eq(m.countryId, c.id)
  ).byDefaultEvenIfAssociated

  // one-to-one
  val companyOpt = belongsTo[Company](Company, (m, c) => m.copy(company = c))
    .includes[Company]((ms, cs) =>
      ms.map { m =>
        cs.find(c => m.company.exists(_.id == c.id))
          .map(v => m.copy(company = Some(v)))
          .getOrElse(m)
      }
    )
    .byDefault

  val mentor =
    belongsToWithAlias[Member](
      Member -> Member.mentorAlias,
      (m, mentor) => m.copy(mentor = mentor)
    ).byDefault
  val name: Association[Member] =
    hasOne[Name](Name, (m, name) => m.copy(name = name))
      .includes[Name]((ms, ns) =>
        ms.map { m =>
          ns.find(n => m.name.exists(_.memberId == m.id))
            .map(v => m.copy(name = Some(v)))
            .getOrElse(m)
        }
      )
      .byDefault

  // groups
  hasManyThroughWithFk[Group](
    GroupMember,
    GroupMapper,
    "memberId",
    "groupId",
    (member, gs) => member.copy(groups = gs)
  ).byDefault
  // if GroupMapper is "Group", this code will work
  //hasManyThrough[Group](GroupMember, Group, (member, groups) => member.copy(groups = groups)).byDefault

  // skills
  val skillsSimpleRef = hasManyThrough[Skill](
    MemberSkill,
    Skill,
    (member, ss) => member.copy(skills = ss)
  )
  // full definition example
  val skillsVerboseRef = hasManyThrough[MemberSkill, Skill](
    through = MemberSkill -> MemberSkill.createAlias("ms2"),
    throughOn =
      (m: Alias[Member], ms: Alias[MemberSkill]) => sqls.eq(m.id, ms.memberId),
    many = Skill -> Skill.createAlias("s2"),
    on = (ms: Alias[MemberSkill], s: Alias[Skill]) => sqls.eq(ms.skillId, s.id),
    merge = (member, ss) => member.copy(skills = ss)
  )

  // mentorees
  val mentorees: Association[Member] = hasMany[Member](
    many = Member -> Member.mentoreeAlias,
    on = (m, mentorees) => sqls.eq(m.id, mentorees.mentorId),
    merge = (member, mentorees) => member.copy(mentorees = mentorees)
  ).includes[Member]((ms, mts) =>
    ms.map { m =>
      m.copy(mentorees = mts.filter(_.mentorId.exists(_ == m.id)))
    }
  ).byDefault

  override def extract(rs: WrappedResultSet, n: ResultName[Member]): Member =
    new Member(
      id = rs.long(n.id),
      countryId = rs.long(n.countryId),
      companyId = rs.longOpt(n.companyId),
      mentorId = rs.longOpt(n.mentorId),
      createdAt = rs.jodaDateTime(n.createdAt),
      country = Country(rs) // cannot use #autoConstruct in this case
    )
}

case class Name(
  memberId: Long,
  first: String,
  last: String,
  createdAt: DateTime,
  updatedAt: Option[DateTime] = None,
  member: Option[Member] = None
)

object Name
  extends CRUDMapper[Name]
  with TimestampsFeature[Name]
  with OptimisticLockWithTimestampFeature[Name] {

  override val tableName = "names"
  override val lockTimestampFieldName = "updatedAt"

  override val useAutoIncrementPrimaryKey = false
  override val primaryKeyFieldName = "memberId"

  override val defaultAlias = createAlias("nm")

  val member = belongsTo[Member](
    Member,
    (name, member) => name.copy(member = member)
  ).byDefault

  def extract(rs: WrappedResultSet, s: ResultName[Name]): Name =
    autoConstruct(rs, s, "member")
}

case class Company(
  id: Option[Long] = None,
  name: String,
  countryId: Option[Long] = None,
  country: Option[Country] = None,
  members: Seq[Member] = Nil
)

object Company
  extends CRUDMapper[Company]
  with SoftDeleteWithBooleanFeature[Company] {
  override val tableName = "companies"
  override val defaultAlias = createAlias("cmp")

  val countryOpt =
    belongsTo[Country](Country, (c, cnt) => c.copy(country = cnt)).byDefault

  val members = hasMany[Member](
    many = Member -> Member.defaultAlias,
    on = (c, ms) => sqls.eq(c.c("id"), ms.companyId),
    merge = (c, ms) => c.copy(members = ms)
  ).includes[Member](merge =
    (cs, ms) => cs.map(c => c.copy(members = ms.filter(_.companyId == c.id)))
  )

  def extract(rs: WrappedResultSet, s: ResultName[Company]): Company =
    autoConstruct(rs, s, "country", "members")
}

case class Country(id: Long, name: String)

object Country extends CRUDMapper[Country] {
  override val tableName = "countries"
  override val defaultAlias = createAlias("cnt")
  def extract(rs: WrappedResultSet, s: ResultName[Country]): Country =
    autoConstruct(rs, s)
}

case class Group(id: Long, name: String)

// using different name is ok though a little bit verbose, mapper must not be the companion.
object GroupMapper
  extends CRUDMapper[Group]
  with SoftDeleteWithTimestampFeature[Group] {
  override val tableName = "groups"
  override val defaultAlias = createAlias("g")
  def extract(rs: WrappedResultSet, s: ResultName[Group]): Group =
    autoConstruct(rs, s)

  beforeCreate((session: DBSession, namedValues: Seq[(SQLSyntax, Any)]) => {
    logger.info(s"Before creation. params: ${namedValues}")
  })
  afterCreate(
    (
      session: DBSession,
      namedValues: Seq[(SQLSyntax, Any)],
      generatedId: Option[Long]
    ) => {
      logger.info(s"Created Group's id: ${generatedId}")
    }
  )
}

case class GroupMember(groupId: Long, memberId: Long)

object GroupMember extends JoinTable[GroupMember] {
  override val tableName = "groups_members"
  override val defaultAlias = createAlias("gm")
}

case class Skill(
  id: Long,
  name: String,
  createdAt: DateTime,
  updatedAt: DateTime,
  lockVersion: Long
)

object Skill
  extends CRUDMapper[Skill]
  with TimestampsFeature[Skill]
  with OptimisticLockWithVersionFeature[Skill] {

  override val tableName = "skills"
  override val defaultAlias = createAlias("s")
  override def extract(rs: WrappedResultSet, s: ResultName[Skill]): Skill =
    autoConstruct(rs, s)
}

case class MemberSkill(memberId: Long, skillId: Long)

object MemberSkill extends JoinTable[MemberSkill] {
  override val tableName = "members_skills"
  override val defaultAlias = createAlias("ms")
}

case class ISBN(value: String)

object ISBN {
  implicit val typeBinder: TypeBinder[ISBN] = TypeBinder.string.map(ISBN.apply)
}

case class Book(
  isbn: ISBN,
  title: String,
  description: Option[String],
  isbnMaster: Option[ISBNMaster] = None
)

object Book extends CRUDMapperWithId[ISBN, Book] {
  def defaultAlias = createAlias("b")
  override def primaryKeyFieldName = "isbn"
  override def tableName = "books"

  override def useExternalIdGenerator = true
  override def generateId = ISBN(java.util.UUID.randomUUID.toString)

  override def rawValueToId(rawValue: Any): ISBN = ISBN(rawValue.toString)
  override def idToRawValue(id: ISBN): String = id.value

  belongsToWithFk[ISBNMaster](
    right = ISBNMaster,
    fk = "isbn",
    merge = (b, im) => b.copy(isbnMaster = im)
  ).byDefault

  override def extract(rs: WrappedResultSet, b: ResultName[Book]) =
    autoConstruct(rs, b, "isbnMaster")
}

case class ISBNMaster(isbn: ISBN, publisher: String, books: Seq[Book] = Nil)

object ISBNMaster extends CRUDMapperWithId[ISBN, ISBNMaster] {
  def defaultAlias = createAlias("isbnm")
  override def primaryKeyFieldName = "isbn"
  override def tableName = "isbn_master"

  override def useExternalIdGenerator = true
  override def generateId = ISBN(java.util.UUID.randomUUID.toString)

  override def rawValueToId(rawValue: Any): ISBN = ISBN(rawValue.toString)
  override def idToRawValue(id: ISBN): String = id.value

  override def extract(rs: WrappedResultSet, b: ResultName[ISBNMaster]) =
    autoConstruct(rs, b, "books")
}

case class ProductId(value: Long)
case class Product(id: ProductId, name: String, priceYen: Long)

object Product extends CRUDMapperWithId[ProductId, Product] {
  override def tableName = "products"
  override def defaultAlias = createAlias("prd")

  override def idToRawValue(id: ProductId) = id.value
  override def rawValueToId(value: Any) = ProductId(value.toString.toLong)

  override def extract(rs: WrappedResultSet, p: ResultName[Product]) =
    new Product(
      id = ProductId(rs.get(p.id)),
      name = rs.get(p.name),
      priceYen = rs.get(p.priceYen)
    )
}

// -----------------------------------
// Table is deprecated
// NOTICE: Removed since 2.0.0

case class Tag(tag: String, description: Option[TagDescription] = None)
// object Tag extends Table[Tag] {
object Tag extends DataMapper[Tag] {
  override def defaultAlias = createAlias("tag")
  override def primaryKeyFieldName = "tag"
  override def extract(rs: WrappedResultSet, n: ResultName[Tag]): Tag = new Tag(
    tag = rs.get(n.tag)
  )

  hasOneWithFk[TagDescription](
    right = TagDescription,
    fk = "tag",
    merge = (t, td) => t.copy(description = td)
  ).byDefault
}
case class TagDescription(tag: String, description: String)
// object TagDescription extends Table[TagDescription] {
object TagDescription extends DataMapper[TagDescription] {
  override def defaultAlias = createAlias("td")
  override def primaryKeyFieldName = "tag"
  override def extract(
    rs: WrappedResultSet,
    n: ResultName[TagDescription]
  ): TagDescription = autoConstruct(rs, n)
}

// -----------------------------------
// Use NoIdMapper instead of Table

case class Tag2(tag: String, description: Option[TagDescription2] = None)
object Tag2 extends NoIdDataMapper[Tag2] {
  override def tableName = "tag2"
  override def defaultAlias = createAlias("t")
  override def extract(rs: WrappedResultSet, n: ResultName[Tag2]): Tag2 =
    new Tag2(tag = rs.get(n.tag))

  hasOneWithFkAndJoinCondition[TagDescription2](
    right = TagDescription2,
    fk = "tag",
    on = sqls.eq(defaultAlias.tag, TagDescription2.defaultAlias.tag),
    merge = (t, td) => t.copy(description = td)
  ).byDefault
}
case class TagDescription2(tag: String, description: String)
object TagDescription2 extends NoIdDataMapper[TagDescription2] {
  override def tableName = "tag_description2"
  override def defaultAlias = createAlias("td")
  override def extract(
    rs: WrappedResultSet,
    n: ResultName[TagDescription2]
  ): TagDescription2 = autoConstruct(rs, n)
}

// -----------------------------------
// simple NoIdMapper example

case class LegacyAccount(
  accountCode: String,
  userId: Option[Int],
  name: Option[String]
)
object LegacyAccount extends NoIdCRUDMapper[LegacyAccount] {
  override def defaultAlias = createAlias("la")
  override def tableName = "legacy_accounts"
  override def extract(
    rs: WrappedResultSet,
    n: ResultName[LegacyAccount]
  ): LegacyAccount = autoConstruct(rs, n)
}

case class LegacyAccount2(
  accountCode: String,
  userId: Option[Int],
  name: Option[String]
)
object LegacyAccount2 extends NoIdDataMapper[LegacyAccount] {
  override def defaultAlias = createAlias("la")
  override def tableName = "legacy_accounts"
  override def extract(
    rs: WrappedResultSet,
    n: ResultName[LegacyAccount]
  ): LegacyAccount = autoConstruct(rs, n)
}

// -----------------------------------
// NoIdMapper with associations

case class Table1(num: Long, name: String, table2: Option[Table2] = None)
case class Table2(label: String, table1: Option[Table1] = None)

object Table1 extends NoIdCRUDMapper[Table1] {
  override def defaultAlias = createAlias("t1")
  override def extract(rs: WrappedResultSet, n: ResultName[Table1]): Table1 =
    autoConstruct(rs, n, "table2")

  private[this] val t2 = Table2.defaultAlias

  val table2 = hasOneWithFkAndJoinCondition[Table2](
    right = Table2,
    fk = "label",
    on = sqls.eq(defaultAlias.name, t2.label),
    merge = (t1, table2) => t1.copy(table2 = table2)
  )
}

object Table2 extends NoIdCRUDMapper[Table2] {
  override def defaultAlias = createAlias("t2")
  override def extract(rs: WrappedResultSet, n: ResultName[Table2]): Table2 =
    autoConstruct(rs, n, "table1")

  private[this] val t1 = Table1.defaultAlias

  hasOneWithFkAndJoinCondition[Table1](
    right = Table1,
    fk = "name",
    on = sqls.eq(defaultAlias.label, t1.name),
    merge = (t2, table1) => t2.copy(table1 = table1)
  ).byDefault
}
