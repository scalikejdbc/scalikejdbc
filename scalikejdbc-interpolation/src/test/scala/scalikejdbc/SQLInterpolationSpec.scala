package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

import scalikejdbc.SQLInterpolation._

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLInterpolation"

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

  case class User(id: Int, name: String)

  object User extends SQLSyntaxSupport {
    val tableName = "users"
    val columns = Seq("id", "name")
  }

  case class Contract(id: Int, userId: Int, value: String)

  object Contract extends SQLSyntaxSupport {
    val tableName = "contracts"
    val columns = Seq("id", "user_id", "value")
  }

  it should "be available" in {
   DB localTx {
      implicit s =>
        try {
          sql"create table users (id int, name varchar(256))".execute.apply()

          Seq((1, "foo"),(2, "bar"), (3, "baz")) foreach { case (id, name) =>
            sql"insert into users values (${id}, ${name})".update.apply()
          }

          val id = 3
          val u = User.syntax
          val user = sql"select ${u.result.*} from ${User.as(u)} where ${u.id} = ${id}".map {
            rs => User(id = rs.int(u.result.id), name = rs.string(u.result.name))
          }.single.apply()
          user.isDefined should equal(true)
          user.get.id should equal(3)
          user.get.name should equal("baz")
        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }

  it should "be able to join" in {
   DB localTx {
      implicit s =>
        try {
          sql"create table users (id int, name varchar(256))".execute.apply()

          Seq((1, "foo"),(2, "bar"), (3, "baz")) foreach { case (id, name) =>
            sql"insert into users values (${id}, ${name})".update.apply()
          }

          sql"create table contracts (id int, user_id int, value varchar(256))".execute.apply()

          Seq((1, 1, "xxx"),(2, 1, "yyy"), (3, 2, "zzz")) foreach { case (id, userId, value) =>
            sql"insert into contracts values (${id}, ${userId}, ${value})".update.apply()
          }

          val id = 1
          val u = User.syntax("u")
          val c = Contract.syntax("c")
          // sql"""${User.as(c)}""" compile error!
          val contracts = sql"""
            select
              ${c.result.*}
            from
              ${User.as(u)},
              ${Contract.as(c)}
            where
              ${c.id} = ${u.id}
              and ${u.id} = ${id}
            order by
              ${c.id}
          """.map {
            rs => Contract(id = rs.int(c.result.id), userId = rs.int(c.result.userId), value = rs.string(c.result.value))
          }.list.apply()
          contracts.size should equal(2)
          contracts(0).id should equal(1)
          contracts(1).id should equal(2)
        } finally {
          sql"drop table users".execute.apply()
          sql"drop table contrancts".execute.apply()
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
            rs => User(id = rs.int("id"), name = rs.string("name"))
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
            rs => User(id = rs.int("id"), name = rs.string("name"))
          }.single.apply()
          user.isDefined should equal(true)
          user.get.id should equal(2)
          user.get.name should be(null)
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
            rs => User(id = rs.int("id"), name = rs.string("name"))
          }.list.apply()
          users.size should equal(2)
          users.map(_.name) should equal(Seq("foo", "bar"))
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
            rs => User(id = rs.int("id"), name = rs.string("name"))
          }.list.apply()
          users.size should equal(2)
          users.map(_.name) should equal(Seq("bar", "foo"))
        } finally {
          sql"drop table users".execute.apply()
        }
    }
  }
}
