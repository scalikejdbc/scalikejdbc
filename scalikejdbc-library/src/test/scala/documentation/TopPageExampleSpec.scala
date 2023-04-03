package documentation

import java.time.LocalDateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TopPageExampleSpec extends AnyFlatSpec with Matchers {

  behavior of "SQLInterpolation"

  // import scalikejdbc._, SQLInterpolation._
  import scalikejdbc._

  // defines entity object and extractor
  case class Member(id: Long, name: Option[String], createdAt: LocalDateTime)
  object Member extends SQLSyntaxSupport[Member] {
    override val tableName = "members"
    def apply(rs: WrappedResultSet) =
      new Member(rs.get("id"), rs.get("name"), rs.get("created_at"))
  }

  it should "work" in {

    // initialize JDBC driver & connection pool
    Class.forName("org.h2.Driver")
    ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

    // ad-hoc session provider on the REPL
    implicit val session = AutoSession

    // table creation, you can run DDL by using #execute as same as JDBC
    sql"""
create table members (
  id serial not null primary key,
  name varchar(64),
  created_at timestamp not null
)
""".execute.apply()

    // insert initial data
    Seq("Alice", "Bob", "Chris") foreach { name =>
      sql"insert into members (name, created_at) values (${name}, current_timestamp)".update
        .apply()
    }

    // for now, retrieves all data as Map value
    val entities: List[Map[String, Any]] =
      sql"select * from members".map(_.toMap()).list.apply()

    // find all members
    val members: List[Member] =
      sql"select * from members".map(rs => Member(rs)).list.apply()

    entities.size should equal(3)
    members.size should equal(3)
  }

}
