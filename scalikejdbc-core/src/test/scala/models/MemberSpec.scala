package models

import scalikejdbc._
import java.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MemberSpec extends AnyFlatSpec with Matchers with Settings {

  behavior of "Active Record like model"

  it should "be available" in {

    DB autoCommit { implicit session =>
      try {
        SQL("drop table MEMBER").execute.apply()
      } catch {
        case e: Exception =>
      }
      try {
        SQL("""
            create table MEMBER (
              id bigint primary key,
              name varchar(30) not null,
              description varchar(1000),
              birthday date,
              created_at timestamp not null
            )
            """).execute.apply()
      } catch {
        case e: Exception =>
      }
    }

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
      DB localTx { implicit session =>
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
    } catch { case e: Exception => }
    Member.findBy("name = /*'name*/''", "name" -> "Rollback").size should equal(
      0
    )

    // execute SQL directly
    MemberSQLTemplate.find().foreach(m => Member.delete(m))
    MemberSQLTemplate.create().id should equal(123)
    MemberSQLTemplate.find().isDefined should be(true)
    MemberSQLTemplate.countAll() should equal(1)
    MemberSQLTemplate.findAll().size should equal(1)
    MemberSQLTemplate.save().name should equal("xxx")
    MemberSQLTemplate.delete()
    MemberSQLTemplate.find().isDefined should be(false)

  }

  it should "be available with NamedDB" in {

    NamedDB("named") autoCommit { implicit session =>
      try {
        SQL("drop table NAMED_MEMBER").execute.apply()
      } catch {
        case e: Exception =>
      }
      SQL("""
            create table NAMED_MEMBER (
              id bigint primary key,
              name varchar(30) not null,
              description varchar(1000),
              birthday date,
              created_at timestamp not null
            )
          """).execute.apply()
    }

    // use model
    val alice = NamedMember.create(
      id = 1,
      name = "Alice",
      description = Option("Alice's Adventures in Wonderland"),
      birthday = Option(LocalDate.of(1980, 1, 2)),
      createdAt = LocalDateTime.now
    )
    NamedMember.find(alice.id).get.id should equal(alice.id)
    intercept[IllegalStateException] {
      NamedMember
        .findBy("name like /*:nameMatch*/'Bob%'", "nameMatch" -> "Alice%")
        .size should be > 0
    }
    NamedMember
      .findBy("name like /*'nameMatch*/'Bob%'", "nameMatch" -> "Alice%")
      .size should be > 0
    val newAlice = alice.copy(name = "ALICE").save()
    NamedMember
      .findBy("name = /*'name*/'Alice'", "name" -> "ALICE")
      .size should equal(1)
    newAlice.destroy()

    try {
      NamedDB("named") localTx { implicit session =>
        NamedMember.create(
          id = 999,
          name = "Rollback",
          description = Option("rollback test"),
          birthday = Option(LocalDate.of(1980, 1, 2)),
          createdAt = LocalDateTime.now
        )
        NamedMember
          .findBy("name = /*'name*/''", "name" -> "Rollback")
          .size should equal(1)
        throw new RuntimeException
      }
    } catch { case e: Exception => }
    NamedMember
      .findBy("name = /*'name*/''", "name" -> "Rollback")
      .size should equal(0)

  }
}
