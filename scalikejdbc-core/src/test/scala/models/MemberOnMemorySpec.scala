package models

import scalikejdbc._
import java.time._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MemberOnMemorySpec extends AnyFlatSpec with Matchers {

  Class.forName("org.h2.Driver")
  ConnectionPool.add("MemberSpec", "jdbc:h2:mem:MemberSpec", "", "")

  behavior of "Testing on memory"

  it should "be available" in {

    NamedDB("MemberSpec") autoCommit { implicit session =>
      try {
        SQL("drop table member").execute.apply()
      } catch {
        case e: Exception =>
      }
      SQL("""
            create table member (
              id bigint primary key,
              name varchar(30) not null,
              description varchar(1000),
              birthday date,
              created_at timestamp not null
            )
             """).execute.apply()
    }

    NamedDB("MemberSpec") localTx { implicit session =>
      // use model
      val alice = Member.create(
        id = 1,
        name = "Alice",
        description = Option("Alice's Adventures in Wonderland"),
        birthday = Option(LocalDate.of(1980, 1, 2)),
        createdAt = LocalDateTime.now
      )
      Member.find(alice.id).get.id should equal(alice.id)
      intercept[IllegalStateException] {
        Member
          .findBy("name like /*:nameMatch*/'Bob%'", "nameMatch" -> "Alice%")
          .size should be > 0
      }
      Member
        .findBy("name like /*'nameMatch*/'Bob%'", "nameMatch" -> "Alice%")
        .size should be > 0
      val newAlice = alice.copy(name = "ALICE").save()
      Member
        .findBy("name = /*'name*/'Alice'", "name" -> "ALICE")
        .size should equal(1)
      newAlice.destroy()

      try {
        NamedDB("MemberSpec") localTx { implicit session =>
          Member.create(
            id = 999,
            name = "Rollback",
            description = Option("rollback test"),
            birthday = Option(LocalDate.of(1980, 1, 2)),
            createdAt = LocalDateTime.now
          )
          Member
            .findBy("name = /*'name*/''", "name" -> "Rollback")
            .size should equal(1)
          throw new RuntimeException
        }
      } catch {
        case e: Exception =>
      }
      Member
        .findBy("name = /*'name*/''", "name" -> "Rollback")
        .size should equal(0)
    }
  }

}
