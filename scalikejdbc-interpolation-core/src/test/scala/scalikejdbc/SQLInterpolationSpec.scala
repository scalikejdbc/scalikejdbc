package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.joda.time._

class SQLInterpolationSpec extends FlatSpec with Matchers {

  // TODO NPE?
  /*
  import scalikejdbc.interpolation._
  import scalikejdbc.interpolation.Implicits._

  behavior of "SQLInterpolation"

  val props = new java.util.Properties
  using(new java.io.FileInputStream("scalikejdbc-library/src/test/resources/jdbc.properties")) { in => props.load(in) }
  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)
  val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
  ConnectionPool.singleton(url, user, password, poolSettings)

  case class Group(id: Int, websiteUrl: Option[String], members: Seq[User] = Nil)
  case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

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
          val user: User = sql"select * from users where id = ${id}".map {
            rs => User(id = rs.int("id"), name = rs.stringOpt("name"))
          }.single.apply().get
          user.id should equal(2)
          user.name should be(None)
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
          val sorting = sqls"desc"
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

*/

}
