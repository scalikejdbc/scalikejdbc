package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

import scalikejdbc.SQLInterpolation._

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLInterpolation"

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

  it should "convert camelCase to snake_case correctly" in {
    SQLSyntaxProvider.toSnakeCase("firstName") should equal("first_name")
    SQLSyntaxProvider.toSnakeCase("SQLObject") should equal("sql_object")
    SQLSyntaxProvider.toSnakeCase("SQLObject", Map("SQL" -> "s_q_l")) should equal("s_q_l_object")
    SQLSyntaxProvider.toSnakeCase("wonderfulMyHTML") should equal("wonderful_my_html")
    SQLSyntaxProvider.toSnakeCase("wonderfulMyHTML", Map("My" -> "xxx")) should equal("wonderfulxxx_html")
  }

  object User extends SQLSyntaxSupport[User] {

    override def tableName = "users"
    override def columns = Seq("id", "first_name", "group_id")
    override def delimiterForResultName = "_Z_"
    override def forceUpperCase = true

    def apply(rs: WrappedResultSet, u: ResultName[User]): User = {
      User(id = rs.int(u.id), name = rs.stringOpt(u.firstName), groupId = rs.intOpt(u.groupId))
    }

    def apply(rs: WrappedResultSet, u: ResultName[User], g: ResultName[Group]): User = {
      apply(rs, u).copy(group = rs.intOpt(g.id).map(id => Group(id = id, websiteUrl = rs.stringOpt(g.websiteUrl))))
    }
  }

  case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

  object Group extends SQLSyntaxSupport[Group] {
    override def tableName = "groups"
    override def columns = Seq("id", "website_url")
    def apply(rs: WrappedResultSet, g: ResultName[Group]): Group = Group(id = rs.int(g.id), websiteUrl = rs.stringOpt(g.field("websiteUrl")))
  }
  case class Group(id: Int, websiteUrl: Option[String], members: List[User] = Nil)

  object GroupMember extends SQLSyntaxSupport[GroupMember] {
    override def tableName = "group_members"
    override def columns = Seq("user_id", "group_id")
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
          sql"insert into groups values (${1}, ${"http://jp.scala-users.org/"})".update.apply()
          sql"insert into groups values (${2}, ${"http://http://www.java-users.jp/"})".update.apply()
          sql"insert into group_members values (${1}, ${1})".update.apply()
          sql"insert into group_members values (${2}, ${1})".update.apply()
          sql"insert into group_members values (${1}, ${2})".update.apply()
          sql"insert into group_members values (${2}, ${2})".update.apply()
          sql"insert into group_members values (${3}, ${2})".update.apply()

          val (u, g) = (User.syntax("u"), Group.syntax)

          val user = sql"""
            select 
              ${u.result.*}, ${g.result.*}
            from 
              ${User.as(u)} left join ${Group.as(g)} on ${u.groupId} = ${g.id}
            where 
              ${u.id} = ${3}
          """.map(rs => User(rs, u.resultName, g.resultName)).single.apply().get

          user.id should equal(3)
          user.name should equal(Some("baz"))
          user.group.isDefined should equal(true)

          intercept[IllegalArgumentException] {
            sql"select ${u.result.*} from ${User.as(u)} where ${u.id} = ${3}".map { rs => u.result.dummy }.single.apply()
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
            where
              ${g.id} = ${1}
          """.foldLeft(Option.empty[Group]) { (groupOpt, rs) =>
              val newMember = User(rs, u.resultName)
              groupOpt.map { group =>
                if (group.members.contains(newMember)) group
                else group.copy(members = newMember.copy(groupId = Option(group.id), group = Option(group)) :: group.members)
              }.orElse {
                Some(Group(rs, g.resultName).copy(members = List(newMember)))
              }
            }

            groupWithMembers.isDefined should equal(true)
            groupWithMembers.get.members.size should equal(2)
          }

          {
            val gm = GroupMember.syntax
            val groupsWithMembers: List[Group] = sql"""
            select
              ${u.result.*}, ${g.result.*}
            from
              ${GroupMember.as(gm)}
                inner join ${Group.as(g)} on ${gm.groupId} = ${g.id}
                inner join ${User.as(u)} on ${gm.userId} = ${u.id}
            order by ${g.id}
            """
              .one(rs => Group(rs, g.resultName))
              .toMany(rs => rs.intOpt(u.resultName.id).map(_ => User(rs, u.resultName)))
              .map { (g, us) => g.copy(members = us) }
              .list.apply()

            groupsWithMembers.size should equal(2)
            groupsWithMembers(0).members.size should equal(2)
            groupsWithMembers(1).members.size should equal(3)
          }

        } finally {
          sql"drop table users".execute.apply()
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
