package document

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class READMESpec extends AnyFunSpec with Matchers {

  import scalikejdbc.*
  import scalikejdbc.orm.*
  import scalikejdbc.orm.timstamps.TimestampsFeature
  import java.time.ZonedDateTime

  case class Email(
    id: Long,
    memberId: Long,
    address: String,
  )
  object Email extends CRUDMapper[Email] {
    override lazy val tableName = "member_email"
    lazy val defaultAlias = createAlias("me")
    def extract(rs: WrappedResultSet, e: ResultName[Email]): Email =
      autoConstruct(rs, e)
  }

  case class Member(
    id: Long,
    name: Option[String],
    createdAt: ZonedDateTime,
    updatedAt: Option[ZonedDateTime],
    email: Option[Email] = None,
  )
  object Member extends CRUDMapper[Member] with TimestampsFeature[Member] {
    lazy val defaultAlias = createAlias("m")
    def extract(rs: WrappedResultSet, n: ResultName[Member]): Member =
      autoConstruct(rs, n, "email")

    val email = hasOne[Email](Email, (m, e) => m.copy(email = e))
  }

  describe("README") {
    it("should work as expected") {
      // Start: Document code
      // ### Database connection ###
      Class.forName("org.h2.Driver")
      ConnectionPool.singleton("jdbc:h2:mem:hello;MODE=PostgreSQL", "user", "pass")
      implicit val session = AutoSession

      // ### Create tables ###
      sql"""create table member (
         id serial not null primary key,
         name varchar(64),
         created_at timestamp not null,
         updated_at timestamp
        )""".execute.apply()
      sql"""create table member_email (
          id serial not null primary key,
          member_id int not null,
          address varchar(256) not null
        )""".execute.apply()

      val m = Member.column

      // ### Insert rows ###
      val ids = Seq("Alice", "Bob", "Chris") map { name =>
        // insert into member (name, created_at, updated_at) values ('Alice', '2024-05-11 14:52:27.13', '2024-05-11 14:52:27.13');
        Member.createWithNamedValues(m.name -> name)
      }

      // ### Find all rows ###
      // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m order by m.id;
      val allMembers1: Seq[Member] = Member.findAll()
      // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where m.id in (1, 2, 3);
      val allMembers2: Seq[Member] = Member.findAllByIds(ids*)

      // ### Run queries with where conditions ###
      // Quick way but less type-safety
      // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where m.name = 'Alice' order by m.id;
      val member1: Seq[Member] = Member.where("name" -> "Alice").apply()
      // Types-safe query builder
      // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where name = 'Alice' order by m.id;
      val member2: Seq[Member] =
        Member.where(sqls.eq(m.name, "Alice")).apply()

      val memberId = member2.head.id

      // ### Run join queries ###
      val e = Email.column
      // insert into member_email (member_id, address) values (1, 'a@example.com');
      Email.createWithNamedValues(
        e.memberId -> memberId,
        e.address -> "a@example.com"
      )

      // Note that member3.email exists while it does not in member1,2
      // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m , me.id as i_on_me, me.member_id as mi_on_me, me.address as a_on_me from member m left join member_email me on m.id = me.member_id where name = 'Alice' order by m.id;
      val member3 =
        Member.joins(Member.email).where(sqls.eq(m.name, "Alice")).apply()

      // ### Update/delete rows ###
      // update member set updated_at = '2024-05-11 14:52:27.188', name = 'Ace' where id = 1;
      Member.updateById(memberId).withAttributes("name" -> "Ace")
      // delete from member where id = 1;
      Member.deleteById(memberId)
      // End: Document code
    }
  }
}
