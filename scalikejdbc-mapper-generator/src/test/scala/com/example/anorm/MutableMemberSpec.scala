package com.example.anorm

import scalikejdbc.specs2.mutable.{ AutoRollback => AutoRollback_ }
import org.specs2.mutable._
import org.joda.time._
import scalikejdbc.{ SQL, DBSession }

class MutableMemberSpec extends Specification with com.example.DBSettings {

  trait AutoRollback extends AutoRollback_ {
    override def fixture(implicit session: DBSession) {
      SQL("insert into member (id, name, created_at) values (?, ?, ?)")
        .bind(123, "123", DateTime.now).update.apply()
    }
  }

  "Member" should {
    "find by primary keys" in new AutoRollback {
      val maybeFound = Member.find(123)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollback {
      val allResults = Member.findAll()
      allResults.size should be_>(0)
    }
    "count all records" in new AutoRollback {
      val count = Member.countAll()
      count should be_>(0L)
    }
    "find by where clauses" in new AutoRollback {
      val results = Member.findAllBy("ID = {id}", 'id -> 123)
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Member.countBy("ID = {id}", 'id -> 123)
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Member.create(name = "MyString", createdAt = DateTime.now)
      created should not beNull
    }
    "update a record" in new AutoRollback {
      val entity = Member.findAll().head
      val updated = Member.update(entity.copy(name = "Updated"))
      updated should not equalTo (entity)
    }
    "delete a record" in new AutoRollback {
      Member.find(123).map { entity =>
        Member.delete(entity)
      }
      val shouldBeNone = Member.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
