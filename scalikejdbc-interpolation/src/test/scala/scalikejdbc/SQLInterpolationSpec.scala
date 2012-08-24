package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class SQLInterpolationSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLInterpolation"

  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

  case class User(id: Int, name: String)

  it should "be available" in {

    import scalikejdbc.SQLInterpolation._

    DB localTx {
      implicit s =>
        try {
          sql"create table users (id int, name varchar(256));".execute.apply()
          sql"insert into users values (${1}, ${"foo"})".update.apply()
          sql"insert into users values (${2}, ${"bar"})".update.apply()
          sql"insert into users values (${3}, ${"baz"})".update.apply()
          val id = 3
          val user = sql"select * from users where id = ${id}".map {
            rs => User(id = rs.int("id"), name = rs.string("name"))
          }.single.apply()
          user.isDefined should equal(true)
        } finally {
          sql"drop table users;".execute.apply()
        }
    }
  }

}
