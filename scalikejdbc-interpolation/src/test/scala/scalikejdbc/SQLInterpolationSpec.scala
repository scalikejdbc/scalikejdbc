package scalikejdbc

import org.scalatest._
import java.time._
import org.slf4j._

import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLInterpolationSpec
  extends AnyFlatSpec
  with Matchers
  with DBSettings
  with SQLInterpolation
  with OptionValues {

  val logger: Logger = LoggerFactory.getLogger(classOf[SQLInterpolationSpec])

  behavior of "SQLInterpolation"

  it should "convert camelCase to snake_case correctly" in {
    SQLSyntaxProvider.toColumnName("_type", Map(), true) should equal("_type")
    SQLSyntaxProvider.toColumnName("type_", Map(), true) should equal("type_")
    SQLSyntaxProvider.toColumnName("firstName", Map(), true) should equal(
      "first_name"
    )
    SQLSyntaxProvider.toColumnName("SQLObject", Map(), true) should equal(
      "sql_object"
    )
    SQLSyntaxProvider.toColumnName(
      "SQLObject",
      Map("SQL" -> "s_q_l"),
      true
    ) should equal("s_q_l_object")
    SQLSyntaxProvider.toColumnName("wonderfulMyHTML", Map(), true) should equal(
      "wonderful_my_html"
    )
    SQLSyntaxProvider.toColumnName(
      "wonderfulMyHTML",
      Map("My" -> "xxx"),
      true
    ) should equal("wonderfulxxx_html")
    SQLSyntaxProvider.toColumnName(
      "wonderfulMyHTML",
      Map("wonderful" -> ""),
      true
    ) should equal("my_html")
    SQLSyntaxProvider.toColumnName("mySQLAsHTML", Map(), true) should equal(
      "my_sql_as_html"
    )

    SQLSyntaxProvider.toColumnName("firstName", Map(), false) should equal(
      "firstName"
    )
    SQLSyntaxProvider.toColumnName(
      "firstName",
      Map("first" -> "full"),
      false
    ) should equal("fullName")
  }

  object User extends SQLSyntaxSupport[User] {

    override val tableName = "users"

    // Both of columns and columnNames are OK
    // override val columns = Seq("id", "first_name", "group_id")
    override val columnNames = Seq("id", "first_name", "group_id")

    override val nameConverters = Map("uid" -> "id")
    override val delimiterForResultName = "_Z_"
    override val forceUpperCase = true

    def apply(rs: WrappedResultSet, u: ResultName[User]): User = {
      User(
        id = rs.int(u.id),
        firstName = rs.stringOpt(u.firstName),
        groupId = rs.intOpt(u.groupId)
      )
    }

    def apply(
      rs: WrappedResultSet,
      u: ResultName[User],
      g: ResultName[Group]
    ): User = {
      apply(rs, u).copy(group =
        rs.intOpt(g.id)
          .map(id => Group(id = id, websiteUrl = rs.stringOpt(g.websiteUrl)))
      )
    }
  }

  case class User(
    id: Int,
    firstName: Option[String],
    groupId: Option[Int] = None,
    group: Option[Group] = None
  )

  object Group extends SQLSyntaxSupport[Group] {
    override val tableName = "groups"
    override val columns = Seq("id", "website_url")
    def apply(rs: WrappedResultSet, g: ResultName[Group]): Group =
      Group(id = rs.int(g.id), websiteUrl = rs.stringOpt(g.field("websiteUrl")))
  }
  case class Group(
    id: Int,
    websiteUrl: Option[String],
    members: collection.Seq[User] = Nil
  )

  object GroupMember extends SQLSyntaxSupport[GroupMember] {
    override val tableName = "group_members"
    override val columns = Seq("user_id", "group_id")
  }
  // case class GroupMember(userId: Int, groupId: Int) // works!
  class GroupMember(val userId: Int, val groupId: Int)
  // class GroupMember(userId: Int, groupId: Int) // works!
  // class GroupMember(userId: Int) // compilation error

  class NotFoundEntity(val id: Long, val name: String)
  object NotFoundEntity extends SQLSyntaxSupport[NotFoundEntity]

  case class NamedDBEntity(id: Long)
  object NamedDBEntity extends SQLSyntaxSupport[NamedDBEntity] {
    override def connectionPoolName = "yetanother"
  }

  it should "throw exception if table not found" in {
    intercept[IllegalStateException](NotFoundEntity.columns)
  }

  it should "load column names from NamedDB" in {
    NamedDB("yetanother") autoCommit { implicit s =>
      try
        sql"select count(1) from named_db_entity"
          .map(_.toMap())
          .single
          .apply()
      catch {
        case e: Exception =>
          sql"create table named_db_entity(id bigint)".execute.apply()
      }
    }
    NamedDBEntity.columns.size should equal(1)
  }

  it should "be available with SQLSyntaxSupport" in {
    DB autoCommit { implicit s =>
      try {
        try sql"drop table users".execute.apply()
        catch { case e: Exception => }
        sql"create table users (id int not null, first_name varchar(256), group_id int)".execute
          .apply()

        try sql"drop table groups".execute.apply()
        catch { case e: Exception => }
        sql"create table groups (id int not null, website_url varchar(256))".execute
          .apply()

        try sql"drop table group_members".execute.apply()
        catch { case e: Exception => }
        sql"create table group_members (user_id int not null, group_id int not null)".execute
          .apply()

        Seq(
          (1, Some("foo"), None),
          (2, Some("bar"), None),
          (3, Some("baz"), Some(1))
        ) foreach { case (id, name, groupId) =>
          val c = User.column
          applyUpdate {
            insert
              .into(User)
              .columns(c.id, c.firstName, c.groupId)
              .values(id, name, groupId)
          }
        }
        sql"insert into groups values (1, ${"http://jp.scala-users.org/"})".update
          .apply()
        sql"insert into groups values (2, ${"https://www.java-users.jp/"})".update
          .apply()
        sql"insert into group_members values (1, 1)".update.apply()
        sql"insert into group_members values (2, 1)".update.apply()
        sql"insert into group_members values (1, 2)".update.apply()
        sql"insert into group_members values (2, 2)".update.apply()
        sql"insert into group_members values (3, 2)".update.apply()

        val (u, g) = (User.syntax("u"), Group.syntax)

        val user = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${User.as(u)} left join ${Group.as(g)} on ${u.groupId} = ${g.id}
            where
              ${u.id} = 3
          """.map(rs => User(rs, u.resultName, g.resultName)).single.apply().get

        user.id should equal(3)
        user.firstName should equal(Some("baz"))
        user.group.isDefined should equal(true)

        // Query Interface using toSQL
        val user2 = select
          .all(u, g)
          .from(User as u)
          .leftJoin(Group as g)
          .on(u.groupId, g.id)
          .where
          .eq(u.id, 3)
          .and
          .isNotNull(u.firstName)
          .toSQL
          .map(rs => User(rs, u.resultName, g.resultName))
          .single
          .apply()
          .get

        user2.id should equal(3)
        user2.firstName should equal(Some("baz"))
        user2.group.isDefined should equal(true)

        // Query Interface using withSQL
        val user3: User = withSQL {
          select
            .all(u, g)
            .from(User as u)
            .leftJoin(Group as g)
            .on(u.groupId, g.id)
            .where
            .eq(u.id, 3)
            .and
            .isNotNull(u.firstName)
        }.map(rs => User(rs, u.resultName, g.resultName)).single.apply().get

        user3.id should equal(3)
        user3.firstName should equal(Some("baz"))
        user3.group.isDefined should equal(true)

        // exception patterns
        {
          /* Compile error by Macro
            intercept[InvalidColumnNameException] {
              sql"""select ${u.result.id}, ${u.result.dummy}
              from ${User.as(u)} inner join ${Group.as(g)} on ${u.groupId} = ${g.id}
              where ${u.id} = 3"""
                .one(rs => User(rs, u.resultName))
                .toOne(rs => Group(rs, g.resultName))
                .map { (u, g) => u.copy(group = Option(g)) }
                .single.apply()
            }

            intercept[ResultSetExtractorException] {
              sql"""select ${u.result.id}
                from ${User.as(u)} inner join ${Group.as(g)} on ${u.groupId} = ${g.id}
                where ${u.id} = 3"""
                .one(rs => rs.int(u.resultName.foo))
                .toOne(rs => Group(rs, g.resultName))
                .map { (foo, g) => foo }
                .list.apply()
            }
           */
        }

        // foldLeft example
        {
          val gm = GroupMember.syntax
          val groupWithMembers: Option[Group] = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${GroupMember.as(gm)}
                inner join ${Group.as(g)} on ${gm.groupId} = ${g.id}
                inner join ${User.as(u)} on ${gm.userId} = ${u.id}
            where
              ${g.id} = 1
          """.foldLeft(Option.empty[Group]) { (groupOpt, rs) =>
            val newMember = User(rs, u.resultName)
            groupOpt
              .map { group =>
                if (group.members.contains(newMember)) group
                else
                  group.copy(members =
                    newMember.copy(
                      groupId = Option(group.id),
                      group = Option(group)
                    ) :: group.members.toList
                  )
              }
              .orElse {
                Some(Group(rs, g.resultName).copy(members = Seq(newMember)))
              }
          }

          groupWithMembers.isDefined should equal(true)
          groupWithMembers.get.members.size should equal(2)
        }

        {
          val gm = GroupMember.syntax
          val groupWithMembers: Option[Group] = withSQL {
            select
              .all(u, g)
              .from(GroupMember as gm)
              .innerJoin(Group as g)
              .on(gm.groupId, g.id)
              .innerJoin(User as u)
              .on(gm.userId, u.id)
              .where
              .eq(g.id, 1)
          }.foldLeft(Option.empty[Group]) { (groupOpt, rs) =>
            val newMember = User(rs, u.resultName)
            groupOpt
              .map { group =>
                if (group.members.contains(newMember)) group
                else
                  group.copy(members =
                    newMember.copy(
                      groupId = Option(group.id),
                      group = Option(group)
                    ) :: group.members.toList
                  )
              }
              .orElse {
                Some(Group(rs, g.resultName).copy(members = Seq(newMember)))
              }
          }

          groupWithMembers.isDefined should equal(true)
          groupWithMembers.get.members.size should equal(2)
        }

        // one-to-many API
        {
          val gm = GroupMember.syntax
          val groupsWithMembers: List[Group] = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${GroupMember.as(gm)}
                inner join ${Group.as(g)} on ${gm.groupId} = ${g.id}
                inner join ${User.as(u)} on ${gm.userId} = ${u.id}
            order by ${g.id}, ${u.id}
            """
            .one(rs => Group(rs, g.resultName))
            .toMany(rs => Some(User(rs, u.resultName)))
            .map { (g, us) => g.copy(members = us) }
            .list
            .apply()

          groupsWithMembers.size should equal(2)
          groupsWithMembers(0).members.size should equal(2)
          groupsWithMembers(0).members(0).id should equal(1)
          groupsWithMembers(0).members(1).id should equal(2)
          groupsWithMembers(1).members.size should equal(3)
          groupsWithMembers(1).members(0).id should equal(1)
          groupsWithMembers(1).members(1).id should equal(2)
          groupsWithMembers(1).members(2).id should equal(3)
        }
        {
          val gm = GroupMember.syntax
          val groupsWithMembers: List[Group] = withSQL {
            select
              .all(u, g)
              .from(GroupMember as gm)
              .innerJoin(Group as g)
              .on(gm.groupId, g.id)
              .innerJoin(User as u)
              .on(gm.userId, u.id)
              .orderBy(g.id, u.id)
          }.one(rs => Group(rs, g.resultName))
            .toMany(rs => Some(User(rs, u.resultName)))
            .map { (g, us) => g.copy(members = us) }
            .list
            .apply()

          groupsWithMembers.size should equal(2)
          groupsWithMembers(0).members.size should equal(2)
          groupsWithMembers(0).members(0).id should equal(1)
          groupsWithMembers(0).members(1).id should equal(2)
          groupsWithMembers(1).members.size should equal(3)
          groupsWithMembers(1).members(0).id should equal(1)
          groupsWithMembers(1).members(1).id should equal(2)
          groupsWithMembers(1).members(2).id should equal(3)
        }

        {
          val gm = GroupMember.syntax
          val groupsWithMembers: Iterable[Group] = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${GroupMember.as(gm)}
                inner join ${Group.as(g)} on ${gm.groupId} = ${g.id}
                inner join ${User.as(u)} on ${gm.userId} = ${u.id}
            order by ${g.id}, ${u.id}
            """
            .one(rs => Group(rs, g.resultName))
            .toMany(rs => Some(User(rs, u.resultName)))
            .map { (g, us) => g.copy(members = us) }
            .iterable
            .apply()

          groupsWithMembers.size should equal(2)
          groupsWithMembers.head.members.size should equal(2)
          groupsWithMembers.head.members(0).id should equal(1)
          groupsWithMembers.head.members(1).id should equal(2)
          groupsWithMembers.tail.head.members.size should equal(3)
          groupsWithMembers.tail.head.members(0).id should equal(1)
          groupsWithMembers.tail.head.members(1).id should equal(2)
          groupsWithMembers.tail.head.members(2).id should equal(3)
        }

        {
          val gm = GroupMember.syntax
          val groupWithMembers: Option[Group] = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${GroupMember.as(gm)}
                inner join ${Group.as(g)} on ${gm.groupId} = ${g.id}
                inner join ${User.as(u)} on ${gm.userId} = ${u.id}
            where ${g.id} = 1
            order by ${g.id}, ${u.id}
            """
            .one(rs => Group(rs, g.resultName))
            .toMany(rs => Some(User(rs, u.resultName)))
            .map { (g, us) => g.copy(members = us) }
            .single
            .apply()

          groupWithMembers.isDefined should be(true)
          groupWithMembers.get.members.size should equal(2)
          groupWithMembers.get.members(0).id should equal(1)
          groupWithMembers.get.members(1).id should equal(2)
        }

        {
          val userId: Option[Int] = Some(1)
          val users: List[User] = sql"""
            select
              ${u.result.*}
            from
              ${User as u}
            ${userId.map(id => sqls"where ${u.id} = ${id}") getOrElse sqls""}
            order by ${u.id}
            """
            .map(rs => User(rs, u.resultName))
            .list
            .apply()

          users.size should be(1)
          users(0).id should equal(1)
        }
        {
          val userId: Option[Int] = Some(1)
          val users: List[User] = withSQL {
            select
              .all(u)
              .from(User as u)
              .append(
                userId.map(id => sqls"where ${u.id} = ${id}") getOrElse sqls""
              )
              .orderBy(u.id)
          }.map(rs => User(rs, u.resultName)).list.apply()

          users.size should be(1)
          users(0).id should equal(1)
        }

        {
          val userId: Option[Int] = None
          val users: List[User] = sql"""
            select
              ${u.result.*}
            from
              ${User as u}
            ${userId.map(id => sqls"where ${u.id} = ${id}") getOrElse sqls""}
            order by ${u.id}
            """
            .map(rs => User(rs, u.resultName))
            .list
            .apply()

          users.size should be(3)
          users(0).id should equal(1)
          users(1).id should equal(2)
          users(2).id should equal(3)
        }

      } finally {
        try sql"drop table users".execute.apply()
        catch { case e: Exception => }
        try sql"drop table groups".execute.apply()
        catch { case e: Exception => }
        try sql"drop table group_members".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  case class Issue(id: Int, body: String, tags: collection.Seq[Tag] = Vector())
  object Issue extends SQLSyntaxSupport[Issue] {
    def apply(rs: WrappedResultSet, i: ResultName[Issue]): Issue =
      Issue(id = rs.int(i.id), body = rs.string(i.body))
  }

  case class Tag(id: Int, name: String)
  object Tag extends SQLSyntaxSupport[Tag] {
    def apply(rs: WrappedResultSet, t: ResultName[Tag]): Tag =
      Tag(id = rs.int(t.id), name = rs.string(t.name))
  }

  object IssueTag extends SQLSyntaxSupport[Nothing]

  case class IssueSummary(count: Long, sum: Long)
  object IssueSummary extends SQLSyntaxSupport[IssueSummary] {
    override val columns = Seq("count", "sum")
    def apply(is: ResultName[IssueSummary])(rs: WrappedResultSet) =
      new IssueSummary(rs.long(is.count), rs.long(is.sum))
  }

  it should "be available for empty relation" in {
    DB autoCommit { implicit s =>
      try {
        sql"create table issue (id int not null, body varchar(256) not null)".execute
          .apply()
        sql"create table tag (id int not null, name varchar(256) not null)".execute
          .apply()
        sql"create table issue_tag (issue_id int not null, tag_id int not null)".execute
          .apply()
      } catch { case e: Exception => }
    }
    try {
      DB localTx { implicit s =>
        sql"insert into issue values (1, ${"Alice"})".update.apply()
        sql"insert into issue values (2, ${"Bob"})".update.apply()
        sql"insert into issue values (3, ${"Chris"})".update.apply()
        sql"insert into issue values (4, ${"Dennis"})".update.apply()

        // insert, update, delete
        val c = Issue.column
        // withSQL { insert.into(Issue).values(5, "Eric") }.update.apply()
        applyUpdate { insert.into(Issue).values(5, "Eric") }
        applyUpdate { update(Issue).set(c.body -> "Debian").where.eq(c.id, 4) }
        applyUpdate { delete.from(Issue).where.eq(c.id, 5) }

        {
          val (i, it, t) =
            (Issue.syntax("i"), IssueTag.syntax("it"), Tag.syntax("t"))

          val issue: Option[Issue] = sql"""
              select
                ${i.result.*}, ${t.result.*}
              from
                ${Issue.as(i)}
                left join ${IssueTag.as(it)} ON ${it.issueId} = ${i.id}
                left join ${Tag.as(t)} ON ${t.id} = ${it.tagId}
              where
                ${i.id} = ${1}
            """.foldLeft(Option.empty[Issue]) { (result, rs) =>
            val tag = rs
              .intOpt(t.resultName.id)
              .map(id => Tag(id, rs.string(t.resultName.name)))
            result.map(i => i.copy(tags = i.tags ++ tag)) orElse Some(
              Issue(
                rs.int(i.resultName.id),
                rs.string(i.resultName.body),
                tag.toVector
              )
            )
          }

          issue.map(_.id) should equal(Some(1))
        }
        {
          val (i, it, t) =
            (Issue.syntax("i"), IssueTag.syntax("it"), Tag.syntax("t"))
          val issue: Option[Issue] = withSQL {
            select
              .all(i, t)
              .from(Issue as i)
              .leftJoin(IssueTag as it)
              .on(it.issueId, i.id)
              .leftJoin(Tag as t)
              .on(t.id, it.tagId)
              .where
              .eq(i.id, 1)
          }.foldLeft(Option.empty[Issue]) { (result, rs) =>
            val tag = rs
              .intOpt(t.resultName.id)
              .map(id => Tag(id, rs.string(t.resultName.name)))
            result.map(i => i.copy(tags = i.tags ++ tag)) orElse Some(
              Issue(
                rs.int(i.resultName.id),
                rs.string(i.resultName.body),
                tag.toVector
              )
            )
          }
          issue.map(_.id) should equal(Some(1))
        }

        {
          val (i, it, t) =
            (Issue.syntax("i"), IssueTag.syntax("it"), Tag.syntax("t"))

          val issue: Option[Issue] = sql"""
              select
                ${i.result.*}, ${t.result.*}
              from
                ${Issue.as(i)}
                left join ${IssueTag.as(it)} ON ${it.issueId} = ${i.id}
                left join ${Tag.as(t)} ON ${t.id} = ${it.tagId}
              where
                ${i.id} = 1
            """
            .one(rs =>
              Issue(rs.int(i.resultName.id), rs.string(i.resultName.body))
            )
            .toMany(rs =>
              rs.intOpt(t.resultName.id)
                .map(id => Tag(id, rs.string(t.resultName.name)))
            )
            .map { (i, ts) => i.copy(tags = i.tags ++ ts) }
            .single
            .apply()

          issue.map(_.id) should equal(Some(1))
        }

        {
          val (i, is) = (Issue.syntax("i"), IssueSummary.syntax("is"))
          val idCount = sqls"count(${i.resultName.id})"
          val idSum = sqls"sum(${i.resultName.id})"
          val sq = SubQuery.syntax("sq", i.resultName)
          val summary = sql"""
              select ${is.result(idCount).count}, ${is
              .result(idSum)
              .sum} from (select ${i.result.id} from ${Issue.as(i)}) ${SubQuery
              .as(sq)}
              """.map(IssueSummary(is.resultName)).single.apply().get
          summary.count should equal(4)
          summary.sum should equal(10)
        }
        {
          val (i, is) = (Issue.syntax("i"), IssueSummary.syntax("is"))
          val idCount = sqls"count(${i.resultName.id})"
          val idSum = sqls"sum(${i.resultName.id})"
          val sq = SubQuery.syntax("sq", i.resultName)
          val summary: IssueSummary = withSQL {
            select(is.result(idCount).count, is.result(idSum).sum)
              .from(select(i.result.id).from(Issue as i).as(sq))
          }.map(IssueSummary(is.resultName)).single.apply().get
          summary.count should equal(4)
          summary.sum should equal(10)
        }
      }
    } finally {
      DB.autoCommit { implicit s =>
        try sql"drop table issue".execute.apply()
        catch { case e: Exception => }
        try sql"drop table tag".execute.apply()
        catch { case e: Exception => }
        try sql"drop table issue_tag".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  object Customer extends SQLSyntaxSupport[Customer] {
    override val tableName = "customers"
    override val forceUpperCase = true

    def apply(c: ResultName[Customer])(rs: WrappedResultSet): Customer = {
      Customer(rs.int(c.id), rs.string(c.name))
    }
  }
  case class Customer(
    id: Int,
    name: String,
    groupId: Option[Int] = None,
    group: Option[CustomerGroup] = None,
    orders: collection.Seq[Order] = Nil
  )

  object CustomerGroup extends SQLSyntaxSupport[CustomerGroup] {
    override val tableName = "customer_group"
  }
  case class CustomerGroup(id: Int, name: String)

  object Product extends SQLSyntaxSupport[Product] {
    override val tableName = "products"
  }
  case class Product(id: Int, name: String)

  object Order extends SQLSyntaxSupport[Order] {
    override val tableName = "orders"
    override val columns = Seq("id", "customer_id", "product_id", "ordered_at")
  }
  case class Order(customerId: Int, productId: Int, orderedAt: LocalDateTime)

  it should "be available for sub-queries with SQLSyntaxSupport" in {
    try {
      DB autoCommit { implicit s =>
        sql"create table customers (id int not null, name varchar(256) not null, group_id int)".execute
          .apply()
        sql"create table customer_group (id int not null, name varchar(256) not null)".execute
          .apply()
        sql"create table products (id int not null, name varchar(256) not null)".execute
          .apply()
        sql"create table orders (id int not null, product_id int not null, customer_id int not null, ordered_at timestamp not null)".execute
          .apply()
      }
    } catch { case e: Exception => }
    DB localTx { implicit s =>
      try {
        sql"insert into customers values (1, ${"Alice"}, null)".update.apply()
        sql"insert into customers values (2, ${"Bob"}, 1)".update.apply()
        sql"insert into customers values (3, ${"Chris"}, 1)".update.apply()
        sql"insert into customers values (4, ${"Dennis"}, null)".update.apply()
        sql"insert into customers values (5, ${"Eric"}, null)".update.apply()
        sql"insert into customers values (6, ${"Fred"}, 1)".update.apply()
        sql"insert into customers values (7, ${"George"}, 1)".update.apply()

        sql"insert into customer_group values (1, ${"JSA"})".update.apply()

        sql"insert into products values (1, ${"Bean"})".update.apply()
        sql"insert into products values (2, ${"Milk"})".update.apply()
        sql"insert into products values (3, ${"Chocolate"})".update.apply()

        sql"insert into orders values (1, 1, 1, current_timestamp)".update
          .apply()
        sql"insert into orders values (2, 1, 2, current_timestamp)".update
          .apply()
        sql"insert into orders values (3, 2, 3, current_timestamp)".update
          .apply()
        sql"insert into orders values (4, 2, 2, current_timestamp)".update
          .apply()
        sql"insert into orders values (5, 2, 1, current_timestamp)".update
          .apply()

        {
          val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
          val sq = SubQuery.syntax("sq", c.resultName)

          val customers: List[Customer] = sql"""
            select
              ${sq.result.*}, ${cg.result.*}
            from
              (select ${c.result.*} from ${Customer.as(c)} limit 5) ${SubQuery
              .as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} > 3
            order by ${sq(c).id}
          """
            .one(Customer(sq(c).resultName))
            .toOptionalOne(rs =>
              rs.intOpt(cg.resultName.id)
                .map(id => CustomerGroup(id, rs.string(cg.resultName.name)))
            )
            .map { (c, cg) => c.copy(group = cg) }
            .list
            .apply()

          customers(0).id should equal(4)
          customers(1).id should equal(5)
        }

        {
          val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
          val sq = SubQuery.syntax("sq", c.resultName)
          val customers: List[Customer] = withSQL {
            select
              .all(sq, cg)
              .from(select.all(c).from(Customer as c).limit(5).as(sq))
              .leftJoin(CustomerGroup as cg)
              .on(sq(c).groupId, cg.id)
              .where
              .gt(sq(c).id, 3)
              .orderBy(sq(c).id)
          }.one(Customer(sq(c).resultName))
            .toOptionalOne(rs =>
              rs.intOpt(cg.resultName.id)
                .map(id => CustomerGroup(id, rs.string(cg.resultName.name)))
            )
            .map { (c, cg) => c.copy(group = cg) }
            .list
            .apply()

          customers(0).id should equal(4)
          customers(1).id should equal(5)
        }

        {
          val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
          val sq = SubQuery.syntax("sq", c.resultName)

          val customers: Iterable[Customer] = sql"""
            select
              ${sq.result.*}, ${cg.result.*}
            from
              (select ${c.result.*} from ${Customer.as(
              c
            )} order by id limit 5) ${SubQuery.as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} > 3
            order by ${sq(c).id}
          """
            .one(rs =>
              Customer(
                rs.int(sq(c).resultName.id),
                rs.string(sq(c).resultName.name)
              )
            )
            .toOptionalOne(rs =>
              rs.intOpt(cg.resultName.id)
                .map(id => CustomerGroup(id, rs.string(cg.resultName.name)))
            )
            .map { (c, cg) => c.copy(group = cg) }
            .iterable
            .apply()

          customers.map(_.id) should equal(Seq(4, 5))
          customers.head.id should equal(4)
          customers.tail.head.id should equal(5)
        }

        {
          val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
          val sq = SubQuery.syntax("sq", c.resultName)

          val customer: Option[Customer] = sql"""
            select
              ${sq.result.*}, ${cg.result.*}
            from
              (select ${c.result.*} from ${Customer.as(
              c
            )} order by id limit 5) ${SubQuery.as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} = 4
          """
            .one(rs =>
              Customer(
                rs.int(sq(c).resultName.id),
                rs.string(sq(c).resultName.name)
              )
            )
            .toOptionalOne(rs =>
              rs.intOpt(cg.resultName.id)
                .map(id => CustomerGroup(id, rs.string(cg.resultName.name)))
            )
            .map { (c, cg) => c.copy(group = cg) }
            .single
            .apply()

          customer.isDefined should be(true)
          customer.get.id should equal(4)
        }

        {
          val (c, o) = (Customer.syntax("c"), Order.syntax("o"))
          val customers = sql"""
              select
                ${c.result.*}
              from
                ${Customer.as(c)}
              where
               ${c.id} in (select ${o.result.customerId} from ${Order.as(o)})
            """
            .map(rs =>
              Customer(rs.int(c.resultName.id), rs.string(c.resultName.name))
            )
            .list
            .apply()

          customers.size should equal(3)
        }

        {
          val (c, o, p) =
            (Customer.syntax("c"), Order.syntax("o"), Product.syntax("p"))
          val x = SubQuery.syntax("x", o.resultName, p.resultName)

          val customers = sql"""
              select
                ${c.result.*}, ${x.result.*}
              from
                ${Customer.as(c)}
                inner join
                (select
                   ${o.result.*}, ${p.result.*}
                 from
                   ${Order.as(o)} inner join ${Product
              .as(p)} on ${o.productId} = ${p.id}
                ) ${SubQuery.as(x)}
                on ${c.id} = ${x(o).customerId}
              order by ${c.id}
            """
            .one(rs =>
              Customer(rs.int(c.resultName.id), rs.string(c.resultName.name))
            )
            .toMany(rs =>
              Some(
                Order(
                  rs.int(x(o).resultName.customerId),
                  rs.int(x(o).resultName.productId),
                  rs.get(x(o).resultName.orderedAt)
                )
              )
            )
            .map { (c, os) => c.copy(orders = os) }
            .list
            .apply()

          customers.size should equal(3)
        }

      } catch {
        case e: Exception =>
          e.printStackTrace
          throw e

      } finally {
        try sql"drop table customers".execute.apply()
        catch { case e: Exception => }
        try sql"drop table customer_group".execute.apply()
        catch { case e: Exception => }
        try sql"drop table products".execute.apply()
        catch { case e: Exception => }
        try sql"drop table orders".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  it should "be available with shortened names" in {
    DB localTx { implicit s =>
      try {
        sql"create table users (id int not null, first_name varchar(256), full_name varchar(256))".execute
          .apply()
        Seq((1, "Alice", "Alice Cooper"), (2, "Bob", "Bob Lee")) foreach {
          case (id, first, full) =>
            val c = UserName.column
            sql"insert into ${UserName.table} (${c.id}, ${c.first}, ${c.full}) values (${id}, ${first}, ${full})".update
              .apply()
        }

        object UserName extends SQLSyntaxSupport[UserName] {
          override val tableName = "users"
          override val columns = Seq("id", "first_name", "full_name")
          override val nameConverters =
            Map("^first$" -> "first_name", "full" -> "full_name")
        }
        case class UserName(id: Int, first: String, full: String)

        val u = UserName.syntax("u")
        val user =
          sql"select ${u.result.*} from ${UserName.as(u)} where ${u.id} = 2"
            .map { rs =>
              UserName(
                id = rs.int(u.resultName.id),
                first = rs.string(u.resultName.first),
                full = rs.string(u.resultName.full)
              )
            }
            .single
            .apply()

        user.isDefined should be(true)
        user.get.first should equal("Bob")
        user.get.full should equal("Bob Lee")

      } finally {
        try sql"drop table users".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  case class XNames(x1: String, x2: String)
  object XNames extends SQLSyntaxSupport[XNames] {
    override val columns = Seq("x1", "x2")
    def apply(xn: ResultName[XNames])(rs: WrappedResultSet) =
      new XNames(x1 = rs.string(xn.x1), x2 = rs.string(xn.x2))
  }

  it should "be available with names such as x1, x2" in {
    try {
      DB localTx { implicit s =>
        sql"create table x_names (x1 varchar(256), x2 varchar(256))".execute
          .apply()
      }
      DB localTx { implicit s =>
        val (xn, c) = (XNames.syntax("xn"), XNames.column)
        Seq(("Alice", "Alice Cooper"), ("Bob", "Bob Lee")) foreach {
          case (x1, x2) =>
            sql"insert into ${XNames.table} (${c.x1}, ${c.x2}) values (${x1}, ${x2})".update
              .apply()
        }
        val found =
          sql"select ${xn.result.*} from ${XNames as xn} where ${xn.x1} = 'Alice'"
            .map(XNames(xn.resultName))
            .single
            .apply()
        found.isDefined should be(true)
        found.get.x1 should equal("Alice")
        found.get.x2 should equal("Alice Cooper")
      }
    } finally {
      DB localTx { implicit s =>
        try sql"drop table x_names".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  case class Names(fullName: String, firstName: String, lastName: String)
  object Names extends SQLSyntaxSupport[Names] {
    override val columns = Seq("full_name", "first_name", "last_name")
    def apply(n: ResultName[Names])(rs: WrappedResultSet) = new Names(
      fullName = rs.string(n.fullName),
      firstName = rs.string(n.firstName),
      lastName = rs.string(n.lastName)
    )
  }

  it should "be available with duplicated shorten names" in {
    try {
      DB autoCommit { implicit s =>
        try sql"drop table names".execute.apply()
        catch { case e: Exception => }
        sql"create table names (full_name varchar(256), first_name varchar(256), last_name varchar(256))".execute
          .apply()
      }
      DB localTx { implicit s =>
        val (n, c) = (Names.syntax("n"), Names.column)
        Seq(
          ("Alice Cooper", "Alice", "Cooper"),
          ("Bob Lee", "Bob", "Lee")
        ) foreach { case (full, first, last) =>
          sql"insert into ${Names.table} (${c.fullName}, ${c.firstName}, ${c.lastName}) values (${full}, ${first}, ${last})".update
            .apply()
        }
        val found =
          sql"select ${n.result.*} from ${Names as n} where ${n.firstName} = 'Alice'"
            .map(Names(n.resultName))
            .single
            .apply()
        found.isDefined should be(true)
        found.get.firstName should equal("Alice")
        found.get.lastName should equal("Cooper")
        found.get.fullName should equal("Alice Cooper")

        val found2: Option[Names] = withSQL {
          select
            .all(n)
            .from(Names as n)
            .where
            .eq(n.firstName, "Alice")
            .append(sqls"order by ${n.firstName}")
        }.map(Names(n.resultName)).single.apply()
        found2.isDefined should be(true)
        found2.get.firstName should equal("Alice")
        found2.get.lastName should equal("Cooper")
        found2.get.fullName should equal("Alice Cooper")

        {
          val names = withSQL {
            select
              .all(n)
              .from(Names as n)
              .where
              .in(n.firstName, Seq("Alice", "Bob", "Chris"))
          }.map(Names(n.resultName)).list.apply()
          names.size should equal(2)
        }
        {
          val groupByResult = withSQL {
            select(n.result.firstName, sqls"count(1)")
              .from(Names as n)
              .groupBy(n.firstName)
          }.map(_.toMap()).list.apply()
          groupByResult.size should equal(2)
        }
      }
    } finally {
      DB localTx { implicit s =>
        try sql"drop table names".execute.apply()
        catch { case e: Exception => }
      }
    }
  }

  it should "return statement and parameters" in {
    {
      val (id, name) = (123, "Alice")
      val sql = sql"insert into company values (${id}, ${name})"
      sql.statement should equal("insert into company values (?, ?)")
      sql.parameters should equal(Seq(123, "Alice"))
    }

    {
      val sql = insert.into(Order).values().toSQL
      sql.statement should equal("insert into orders values ()")
    }

    {
      val sql = insert.into(Order).values(Nil).toSQL
      sql.statement should equal("insert into orders values ()")
    }

    {
      val (id, customer_id, product_id, ordered_at) =
        (11, 1, Some(1), LocalDateTime.of(2018, 4, 20, 0, 0))
      val sql =
        insert.into(Order).values(id, customer_id, product_id, ordered_at).toSQL
      sql.statement should equal("insert into orders values (?, ?, ?, ?)")
      sql.parameters should equal(
        Seq(11, 1, Some(1), LocalDateTime.of(2018, 4, 20, 0, 0))
      )
    }

    {
      val sql = insert.into(Order).multipleValues().toSQL
      sql.statement should equal("insert into orders values ()")
    }

    {
      val sql = insert.into(Order).multipleValues(Nil).toSQL
      sql.statement should equal("insert into orders values ()")
    }

    {
      val vs = Seq(11, 1, Some(1), LocalDateTime.of(2018, 4, 20, 0, 0))
      val sql = insert.into(Order).multipleValues(vs).toSQL
      sql.statement should equal("insert into orders values (?, ?, ?, ?)")
      sql.parameters should equal(
        Seq(11, 1, Some(1), LocalDateTime.of(2018, 4, 20, 0, 0))
      )
    }

    {
      val Seq(vs1, vs2) = Seq(
        Seq(11, 1, Some(1), LocalDateTime.of(2018, 4, 20, 0, 0)),
        Seq(12, 2, Some(2), LocalDateTime.of(2018, 1, 2, 3, 4))
      )
      val sql = insert.into(Order).multipleValues(vs1, vs2).toSQL
      sql.statement should equal(
        "insert into orders values (?, ?, ?, ?), (?, ?, ?, ?)"
      )
      sql.parameters should equal(
        Seq(
          11,
          1,
          Some(1),
          LocalDateTime.of(2018, 4, 20, 0, 0),
          12,
          2,
          Some(2),
          LocalDateTime.of(2018, 1, 2, 3, 4)
        )
      )
    }
  }

  it should "cache columns" in {
    SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
      .get(Order.connectionPoolName -> Order.tableName)
      .value
      .values
      .forall(_.nonEmpty) should be(true)
  }

  it should "clear loaded columns" in {
    Order.clearLoadedColumns()
    SQLSyntaxSupportFeature.SQLSyntaxSupportCachedColumns
      .get(Order.connectionPoolName -> Order.tableName)
      .value
      .values
      .forall(_.isEmpty) should be(true)

    SQLSyntaxSupport.clearLoadedColumns(ConnectionPool.DEFAULT_NAME)
    SQLSyntaxSupport.clearAllLoadedColumns()
  }

  case class FooBarBaz(firstName: String, lastName: Option[String] = None)
  object FooBarBaz extends SQLSyntaxSupport[FooBarBaz] {}

  it should "clear loaded columns after db migration" in {
    implicit val session = AutoSession

    try {
      sql"create table foo_bar_baz(first_name varchar(64) not null);".execute
        .apply()

      FooBarBaz.column.column("first_name") should equal(
        SQLSyntax("first_name")
      )
      intercept[InvalidColumnNameException] {
        FooBarBaz.column.column("last_name")
      }

      sql"drop table foo_bar_baz;".execute.apply()

      FooBarBaz.clearLoadedColumns()

      sql"create table foo_bar_baz(first_name varchar(64) not null, last_name varchar(64));".execute
        .apply()

      FooBarBaz.column.column("first_name") should equal(
        SQLSyntax("first_name")
      )
      FooBarBaz.column.column("last_name") should equal(SQLSyntax("last_name"))

      sql"drop table foo_bar_baz;".execute.apply()
      sql"create table foo_bar_baz(first_name varchar(64) not null);".execute
        .apply()

      FooBarBaz.column.column("first_name") should equal(
        SQLSyntax("first_name")
      )
      FooBarBaz.column.column("last_name") should equal(SQLSyntax("last_name"))

      FooBarBaz.clearLoadedColumns()

      FooBarBaz.column.column("first_name") should equal(
        SQLSyntax("first_name")
      )
      intercept[InvalidColumnNameException] {
        FooBarBaz.column.column("last_name")
      }

      sql"drop table foo_bar_baz;".execute.apply()
      sql"create table foo_bar_baz(first_name varchar(64) not null, last_name varchar(64));".execute
        .apply()

      SQLSyntaxSupport.clearLoadedColumns()

      FooBarBaz.column.column("first_name") should equal(
        SQLSyntax("first_name")
      )
      FooBarBaz.column.column("last_name") should equal(SQLSyntax("last_name"))
    } finally {
      try sql"drop table foo_bar_baz;".execute.apply()
      catch { case NonFatal(_) => }
    }
  }

  it should "provide fast dynamic result names" in {
    val resultName = User.syntax("u").resultName

    val millis = (1 to 10)
      .map { _ =>
        val start = System.currentTimeMillis()
        (1 to 10000) foreach { _ =>
          resultName.id.value shouldBe "I_Z_U"
          resultName.firstName.value shouldBe "FN_Z_U"
          resultName.groupId.value shouldBe "GI_Z_U"
        }
        val end = System.currentTimeMillis()
        (end - start)
      }
      .find(_ < 100L)
    millis.get should be < 100L
  }

  it should "be faster when using TrieMap instead" in {
    val results = (1 to 5).map { _ =>
      val noCacheResultMillis = {
        val start = System.currentTimeMillis()
        (1 to 100000).foreach { _ =>
          columnNoCache("first_Name")
        }
        val end = System.currentTimeMillis()
        end - start
      }
      val cacheResultMillis = {
        val start = System.currentTimeMillis()
        (1 to 100000).foreach { _ =>
          column("first_Name")
        }
        val end = System.currentTimeMillis()
        end - start
      }
      (cacheResultMillis, noCacheResultMillis)
    }
    logger.info(s"results (cached, no-cache): ${results}")
  }

  val tableAliasName = "table"
  val delimiterForResultName = "_on_"
  val columns: collection.Seq[SQLSyntax] = Seq(
    sqls"first_name",
    sqls"last_name",
    sqls"birth_date",
    sqls"company_id",
    sqls"id",
    sqls"created_at"
  )
  val cachedColumns = new TrieMap[String, SQLSyntax]()

  def columnNoCache(name: String): SQLSyntax = {
    columns
      .find(_.value.equalsIgnoreCase(name))
      .map { c =>
        SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
      }
      .getOrElse(throw new Exception)
  }
  def column(name: String): SQLSyntax =
    cachedColumns.getOrElse(name, columnNoCache(name))

}
