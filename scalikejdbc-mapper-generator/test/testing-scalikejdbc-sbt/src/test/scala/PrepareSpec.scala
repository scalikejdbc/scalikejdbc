import org.scalatest._
import org.scalatest.matchers._

import scalikejdbc._
import com.example.models.Member

class PrepareSpec extends FlatSpec with ShouldMatchers {

  it should "work fine" in {
    Class.forName("org.hsqldb.jdbc.JDBCDriver")
    ConnectionPool.singleton("jdbc:hsqldb:file:db/test", "sa", "")
    DB autoCommit { implicit session =>
      try {
        SQL("select count(1) from member").map(rs => rs).list.apply()
      } catch {
        case e =>
          e.printStackTrace()
          SQL("""
          create table member (
            id bigint generated always as identity,
            name varchar(30) not null,
            description varchar(1000),
            birthday date,
            created_at timestamp not null,
            primary key(id)
          )
          """).execute.apply()
          SQL("insert into member (name, description, birthday, created_at) values (?, ?, ?, ?)")
            .bind("Andy", null, null, new java.util.Date).update.apply()
      }
    }
    DB readOnly { implicit session =>
      SQL("select * from member").map(rs => rs.long("id")).list.apply().foreach { id => println(id) }
    }
    Member.countAll() should equal(1L)
    Member.findBy("id = {id}", 'id -> 2).size should equal(0)
    Thread.sleep(1000)
  }

}
