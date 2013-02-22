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

  object User extends SQLSyntaxSupport {
    override def tableName = "users"
    override def columns = Seq("id", "first_name", "group_id")
    override def forceUpperCase = true
  }

  case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

  object Group extends SQLSyntaxSupport {
    override def tableName = "groups"
    override def columns = Seq("id", "website_url")
  }
  case class Group(id: Int, websiteUrl: Option[String])

  it should "be available with SQLSyntaxSupport" in {
   DB localTx {
      implicit s =>
        try {
          sql"create table users (id int not null, first_name varchar(256), group_id int)".execute.apply()
          sql"create table groups (id int not null, website_url varchar(256))".execute.apply()

          Seq((1, Some("foo"), None),(2, Some("bar"), None), (3, Some("baz") ,Some(1))) foreach { case (id, name, groupId) =>
            sql"insert into users values (${id}, ${name}, ${groupId})".update.apply()
          }
          sql"insert into groups values (${1}, ${"http://www.example.com"})".update.apply()

          val id = 3
          val u = User.syntax("u")
          val g = Group.syntax
          // User.as(g) compile error!
          val user = sql"""
            select ${u.result.*}, ${g.result.*} 
            from ${User.as(u)} left join ${Group.as(g)} on ${u.groupId} = ${g.id}
            where ${u.id} = ${id}
            """.map { rs => 
              val (user, group) = (u.result.names, g.result.names)
              User(
                id = rs.int(user.id), 
                name = rs.stringOpt(user.firstName), 
                groupId = rs.intOpt(user.groupId),
                group = rs.intOpt(group.id).map(id => Group(id = id, websiteUrl = rs.stringOpt(group.websiteUrl))
              )
            )
          }.single.apply()
          user.isDefined should equal(true)
          user.get.id should equal(3)
          user.get.name should equal(Some("baz"))

          user.get.group.isDefined should equal(true)

          intercept[IllegalArgumentException] {
            val user = sql"select ${u.result.*} from ${User.as(u)} where ${u.id} = ${id}".map { rs => u.result.dummy  }.single.apply()
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

          Seq((1, "foo"),(2, "bar"), (3, "baz")) foreach { case (id, name) =>
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

          Seq((1, Some("foo")),(2, None)) foreach { case (id, name) =>
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
          Seq((1, "foo"),(2, "bar"), (3, "baz")) foreach { case (id, name) =>
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
          Seq((1, "foo"),(2, "bar"), (3, "baz")) foreach { case (id, name) =>
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
