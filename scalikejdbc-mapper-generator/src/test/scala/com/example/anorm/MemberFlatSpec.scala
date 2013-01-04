package com.example.anorm

import org.scalatest._
import org.joda.time._
import scalikejdbc.scalatest.AutoRollback
import scalikejdbc._

class MemberFlatSpec extends fixture.FlatSpec with ShouldMatchers with AutoRollback
    with com.example.DBSettings {

  override def fixture(implicit session: DBSession) {
    SQL("insert into member (id, name, created_at) values (?, ?, ?)")
      .bind(123, "123", DateTime.now).update.apply()
  }

  behavior of "Member"

  it should "find by primary keys" in { implicit session =>
    val maybeFound = Member.find(123)
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in { implicit session =>
    val allResults = Member.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in { implicit session =>
    val count = Member.countAll()
    count should be > (0L)
  }
  it should "find by where clauses" in { implicit session =>
    val results = Member.findAllBy("ID = {id}", 'id -> 123)
    results.size should be > (0)
  }
  it should "count by where clauses" in { implicit session =>
    val count = Member.countBy("ID = {id}", 'id -> 123)
    count should be > (0L)
  }
  it should "create new record" in { implicit session =>
    val created = Member.create(name = "MyString", createdAt = DateTime.now)
    created should not be (null)
  }
  it should "update a record" in { implicit session =>
    val entity = Member.findAll().head
    val updated = Member.update(entity.copy(name = "Updated"))
    updated should not equal (entity)
  }
  it should "delete a record" in { implicit session =>
    Member.find(123).map { entity =>
      Member.delete(entity)
    }
    val shouldBeNone = Member.find(123)
    shouldBeNone.isDefined should be(false)
  }

}

