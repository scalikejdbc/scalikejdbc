package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

import org.joda.time._
import scalikejdbc.SQLInterpolation._

class HibernateSQLFormatter extends SQLFormatter {
  private val formatter = new org.hibernate.engine.jdbc.internal.BasicFormatterImpl()
  def format(sql: String) = formatter.format(sql)
}

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLInterpolation"

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

  GlobalSettings.sqlFormatter = SQLFormatterSettings("scalikejdbc.HibernateSQLFormatter")

  it should "convert camelCase to snake_case correctly" in {
    SQLSyntaxProvider.toSnakeCase("firstName") should equal("first_name")
    SQLSyntaxProvider.toSnakeCase("SQLObject") should equal("sql_object")
    SQLSyntaxProvider.toSnakeCase("SQLObject", Map("SQL" -> "s_q_l")) should equal("s_q_l_object")
    SQLSyntaxProvider.toSnakeCase("wonderfulMyHTML") should equal("wonderful_my_html")
    SQLSyntaxProvider.toSnakeCase("wonderfulMyHTML", Map("My" -> "xxx")) should equal("wonderfulxxx_html")
  }

  object User extends SQLSyntaxSupport[User] {

    override val tableName = "users"
    override val columns = Seq("id", "first_name", "group_id")
    override val nameConverters = Map("uid" -> "id")
    override val delimiterForResultName = "_Z_"
    override val forceUpperCase = true

    def apply(rs: WrappedResultSet, u: ResultName[User]): User = {
      User(id = rs.int(u.id), name = rs.stringOpt(u.firstName), groupId = rs.intOpt(u.groupId))
    }

    def apply(rs: WrappedResultSet, u: ResultName[User], g: ResultName[Group]): User = {
      apply(rs, u).copy(group = rs.intOpt(g.id).map(id => Group(id = id, websiteUrl = rs.stringOpt(g.websiteUrl))))
    }
  }

  case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

  object Group extends SQLSyntaxSupport[Group] {
    override val tableName = "groups"
    override val columns = Seq("id", "website_url")
    def apply(rs: WrappedResultSet, g: ResultName[Group]): Group = Group(id = rs.int(g.id), websiteUrl = rs.stringOpt(g.field("websiteUrl")))
  }
  case class Group(id: Int, websiteUrl: Option[String], members: Seq[User] = Nil)

  object GroupMember extends SQLSyntaxSupport[GroupMember] {
    override val tableName = "group_members"
    override val columns = Seq("user_id", "group_id")
  }
  case class GroupMember(userId: Int, groupId: Int)

  it should "be available with SQLSyntaxSupport" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, first_name varchar(256), group_id int)".execute.apply()
          sql"create table groups (id int not null, website_url varchar(256))".execute.apply()
          sql"create table group_members (user_id int not null, group_id int not null)".execute.apply()

          Seq((1, Some("foo"), None), (2, Some("bar"), None), (3, Some("baz"), Some(1))) foreach {
            case (id, name, groupId) =>
              sql"insert into users values (${id}, ${name}, ${groupId})".update.apply()
          }
          sql"insert into groups values (1, ${"http://jp.scala-users.org/"})".update.apply()
          sql"insert into groups values (2, ${"http://http://www.java-users.jp/"})".update.apply()
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
          user.name should equal(Some("baz"))
          user.group.isDefined should equal(true)

          // exception patterns
          {
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
              groupOpt.map { group =>
                if (group.members.contains(newMember)) group
                else group.copy(members = newMember.copy(groupId = Option(group.id), group = Option(group)) :: group.members.toList)
              }.orElse {
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
              .list.apply()

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
            val groupsWithMembers: Traversable[Group] = sql"""
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
              .traversable.apply()

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
              .single.apply()

            groupWithMembers.isDefined should be(true)
            groupWithMembers.get.members.size should equal(2)
            groupWithMembers.get.members(0).id should equal(1)
            groupWithMembers.get.members(1).id should equal(2)
          }

        } finally {
          sql"drop table users".execute.apply()
          sql"drop table groups".execute.apply()
          sql"drop table group_members".execute.apply()
        }
    }
  }

  case class Issue(id: Int, body: String, tags: Seq[Tag] = Vector())

  object Issue extends SQLSyntaxSupport[Issue] {
    override val tableName = "issues"
    override val columns = Seq("id", "body")
    def apply(rs: WrappedResultSet, i: ResultName[Issue]): Issue = Issue(
      id = rs.int(i.id),
      body = rs.string(i.body)
    )
  }

  case class Tag(id: Int, name: String)

  object Tag extends SQLSyntaxSupport[Tag] {
    override val tableName = "tags"
    override val columns = Seq("id", "name")
    def apply(rs: WrappedResultSet, t: ResultName[Tag]): Tag = Tag(
      id = rs.int(t.id),
      name = rs.string(t.name)
    )
  }

  object IssueTag extends SQLSyntaxSupport[Nothing] {
    override val tableName = "issue_tag"
    override val columns = Seq("issue_id", "tag_id")
  }

  it should "be available for empty relation" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table issues (id int not null, body varchar(256) not null)".execute.apply()
          sql"create table tags (id int not null, name varchar(256) not null)".execute.apply()
          sql"create table issue_tag (issue_id int not null, tag_id int not null)".execute.apply()

          sql"insert into issues values (1, ${"Alice"})".update.apply()
          sql"insert into issues values (2, ${"Bob"})".update.apply()
          sql"insert into issues values (3, ${"Chris"})".update.apply()
          sql"insert into issues values (4, ${"Dennis"})".update.apply()

          {
            val (i, it, t) = (Issue.syntax("i"), IssueTag.syntax("it"), Tag.syntax("t"))

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
              val tag = rs.intOpt(t.resultName.id).map(id => Tag(id, rs.string(t.resultName.name)))
              result.map(i => i.copy(tags = i.tags ++ tag)) orElse Some(Issue(rs.int(i.resultName.id), rs.string(i.resultName.body), tag.to[Vector]))
            }

            issue.map(i => i.id) should equal(Some(1))
          }

          {
            val (i, it, t) = (Issue.syntax("i"), IssueTag.syntax("it"), Tag.syntax("t"))

            val issue: Option[Issue] = sql"""
              select
                ${i.result.*}, ${t.result.*}
              from
                ${Issue.as(i)}
                left join ${IssueTag.as(it)} ON ${it.issueId} = ${i.id}
                left join ${Tag.as(t)} ON ${t.id} = ${it.tagId}
              where
                ${i.id} = ${1}
            """
              .one(rs => Issue(rs.int(i.resultName.id), rs.string(i.resultName.body)))
              .toMany(rs => rs.intOpt(t.resultName.id).map(id => Tag(id, rs.string(t.resultName.name))))
              .map { (i, ts) => i.copy(tags = i.tags ++ ts) }
              .single
              .apply()

            issue.map(i => i.id) should equal(Some(1))
          }

        } finally {
          sql"drop table issues".execute.apply()
          sql"drop table tags".execute.apply()
          sql"drop table issue_tag".execute.apply()
        }
    }
  }

  object Customer extends SQLSyntaxSupport[Customer] {
    override val tableName = "customers"
    override val forceUpperCase = true
  }
  case class Customer(id: Int, name: String, groupId: Option[Int] = None, group: Option[CustomerGroup] = None, orders: Seq[Order] = Nil)

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
  case class Order(customerId: Int, productId: Int, orderedAt: DateTime)

  it should "be available for sub-queries with SQLSyntaxSupport" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table customers (id int not null, name varchar(256) not null, group_id int)".execute.apply()
          sql"create table customer_group (id int not null, name varchar(256) not null)".execute.apply()
          sql"create table products (id int not null, name varchar(256) not null)".execute.apply()
          sql"create table orders (id int not null, product_id int not null, customer_id int not null, ordered_at timestamp not null)".execute.apply()

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

          sql"insert into orders values (1, 1, 1, current_timestamp)".update.apply()
          sql"insert into orders values (2, 1, 2, current_timestamp)".update.apply()
          sql"insert into orders values (3, 2, 3, current_timestamp)".update.apply()
          sql"insert into orders values (4, 2, 2, current_timestamp)".update.apply()
          sql"insert into orders values (5, 2, 1, current_timestamp)".update.apply()

          {
            val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
            val sq = SubQuery.syntax("sq", c.resultName)

            val customers: List[Customer] = sql"""
            select
              ${sq.result.*}, ${cg.result.*}
            from
              (select ${c.result.*} from ${Customer.as(c)} order by id limit 5) ${SubQuery.as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} > 3
            order by ${sq(c).id}
          """
              .one(rs => Customer(rs.int(sq(c).resultName.id), rs.string(sq(c).resultName.name)))
              .toOptionalOne(rs => rs.intOpt(cg.resultName.id).map(id => CustomerGroup(id, rs.string(cg.resultName.name))))
              .map { (c, cg) => c.copy(group = Some(cg)) }
              .list
              .apply()

            customers.map(u => u.id) should equal(Seq(4, 5))
            customers(0).id should equal(4)
            customers(1).id should equal(5)
          }

          {
            val (c, cg) = (Customer.syntax("c"), CustomerGroup.syntax("cg"))
            val sq = SubQuery.syntax("sq", c.resultName)

            val customers: Traversable[Customer] = sql"""
            select
              ${sq.result.*}, ${cg.result.*}
            from
              (select ${c.result.*} from ${Customer.as(c)} order by id limit 5) ${SubQuery.as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} > 3
            order by ${sq(c).id}
          """
              .one(rs => Customer(rs.int(sq(c).resultName.id), rs.string(sq(c).resultName.name)))
              .toOptionalOne(rs => rs.intOpt(cg.resultName.id).map(id => CustomerGroup(id, rs.string(cg.resultName.name))))
              .map { (c, cg) => c.copy(group = Some(cg)) }
              .traversable
              .apply()

            customers.map(u => u.id) should equal(Seq(4, 5))
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
              (select ${c.result.*} from ${Customer.as(c)} order by id limit 5) ${SubQuery.as(sq)}
                left join ${CustomerGroup.as(cg)} on ${sq(c).groupId} = ${cg.id}
            where
              ${sq(c).id} = 4
          """
              .one(rs => Customer(rs.int(sq(c).resultName.id), rs.string(sq(c).resultName.name)))
              .toOptionalOne(rs => rs.intOpt(cg.resultName.id).map(id => CustomerGroup(id, rs.string(cg.resultName.name))))
              .map { (c, cg) => c.copy(group = Some(cg)) }
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
              .map(rs => Customer(rs.int(c.resultName.id), rs.string(c.resultName.name))).list.apply()

            customers.size should equal(3)
          }

          {
            val (c, o, p) = (Customer.syntax("c"), Order.syntax("o"), Product.syntax("p"))
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
                   ${Order.as(o)} inner join ${Product.as(p)} on ${o.productId} = ${p.id}
                ) ${SubQuery.as(x)}
                on ${c.id} = ${x(o).customerId}
              order by ${c.id}
            """
              .one(rs => Customer(rs.int(c.resultName.id), rs.string(c.resultName.name)))
              .toMany(rs => Some(Order(rs.int(x(o).resultName.id), rs.int(x(o).resultName.productId), rs.timestamp(x(o).resultName.orderedAt).toDateTime)))
              .map { (c, os) => c.copy(orders = os) }.list.apply()

            customers.size should equal(3)
          }

        } finally {
          sql"drop table customers".execute.apply()
          sql"drop table customer_group".execute.apply()
          sql"drop table products".execute.apply()
          sql"drop table orders".execute.apply()
        }
    }
  }

  it should "be available with here document values" in {
    DB localTx {
      implicit s =>
        try {
          sql"""create table users (id int, name varchar(256))""".execute.apply()

          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"""insert into users values (${id}, ${name})""".update.apply()
          }

          val id = 3
          val user = sql"""select * from users where id = ${id}""".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.single.apply()
          user.isDefined should equal(true)
        } finally {
          sql"""drop table users""".execute.apply()
        }
    }
  }

  it should "be available with option values" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, name varchar(256))".execute.apply()

          Seq((1, Some("foo")), (2, None)) foreach {
            case (id, name) =>
              sql"insert into users values (${id}, ${name})".update.apply()
          }

          val id = 2
          val user = sql"select * from users where id = ${id}".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.single.apply()
          user.isDefined should equal(true)
          user.get.id should equal(2)
          user.get.name should be(None)
        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }

  it should "be available with shortened names" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, first_name varchar(256), full_name varchar(256))".execute.apply()
          Seq((1, "Alice", "Aclice Cooper"), (2, "Bob", "Bob Lee")) foreach {
            case (id, first, full) =>
              sql"insert into users values (${id}, ${first}, ${full})".update.apply()
          }

          object UserName extends SQLSyntaxSupport[UserName] {
            override val tableName = "users"
            override val columns = Seq("id", "first_name", "full_name")
          }
          case class UserName(id: Int, first: String, full: String)

          val u = UserName.syntax("u")
          val user = sql"select ${u.result.*} from ${UserName.as(u)} where ${u.id} = 2".map {
            rs =>
              UserName(id = rs.int(u.resultName.id),
                first = rs.string(u.resultName.firstName),
                full = rs.string(u.resultName.fullName))
          }.single.apply()

          user.isDefined should be(true)
          user.get.first should equal("Bob")
          user.get.full should equal("Bob Lee")

        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }

  it should "be available with the IN statement" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, name varchar(256))".execute.apply()
          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"insert into users values (${id}, ${name})".update.apply()
          }

          val ids = List(1, 2, 4) ::: (100 until 200).toList
          val users = sql"select * from users where id in (${ids})".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.list.apply()
          users.size should equal(2)
          users.map(_.name) should equal(Seq(Some("foo"), Some("bar")))
        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }

  it should "be available with sql syntax" in {
    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, name varchar(256))".execute.apply()
          Seq((1, "foo"), (2, "bar"), (3, "baz")) foreach {
            case (id, name) =>
              sql"insert into users values (${id}, ${name})".update.apply()
          }

          val ids = List(1, 2, 4) ::: (100 until 200).toList
          val sorting = SQLSyntax("DESC")
          val users = sql"select * from users where id in (${ids}) order by id ${sorting}".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.list.apply()
          users.size should equal(2)
          users.map(_.name) should equal(Seq(Some("bar"), Some("foo")))
        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }

}
