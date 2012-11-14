package com.example.anorm

/*
import org.specs2.mutable._
import org.joda.time._

class MemberSpec extends Specification {

  "Member" should {
    "find by primary keys" in {
      val foundByPrimaryKeys = Member.find(123)
      foundByPrimaryKeys.isDefined should beTrue
    }
    "find all records" in {
      val all = Member.findAll()
      all.size should be_>(0)
    }
    "count all records" in {
      val count = Member.countAll()
      count should be_>(0L)
    }
    "find by where clauses" in {
      val results = Member.findAllBy("ID = {id}", 'id -> 123)
      results.size should be_>(0)
    }
    "count by where clauses" in {
      val count = Member.countBy("ID = {id}", 'id -> 123)
      count should be_>(0L)
    }
    "create new record" in {
      val created = Member.create(name = "MyString", createdAt = DateTime.now)
      created should not beNull
    }
    "save a record" in {
      val entity = Member.findAll().head
      val saved = Member.save(entity)
      saved should not equalTo (entity)
    }
    "delete a record" in {
      val entity = Member.findAll().head
      Member.delete(entity)
      val shouldBeNone = Member.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
*/

/*
import org.specs2._
import org.joda.time._

class MemberSpec extends Specification {
  def is =

    "The 'Member' model should" ^
      "find by primary keys" ! findByPrimaryKeys ^
      "find all records" ! findAll ^
      "count all records" ! countAll ^
      "find by where clauses" ! findAllBy ^
      "count by where clauses" ! countBy ^
      "create new record" ! create ^
      "save a record" ! save ^
      "delete a record" ! delete ^
      end

  def findByPrimaryKeys = {
    val maybeFound = Member.find(123)
    maybeFound.isDefined should beTrue
  }
  def findAll = {
    val allResults = Member.findAll()
    allResults.size should be_>(0)
  }
  def countAll = {
    val count = Member.countAll()
    count should be_>(0L)
  }
  def findAllBy = {
    val results = Member.findAllBy("ID = {id}", 'id -> 123)
    results.size should be_>(0)
  }
  def countBy = {
    val count = Member.countBy("ID = {id}", 'id -> 123)
    count should be_>(0L)
  }
  def create = {
    val created = Member.create(name = "MyString", createdAt = DateTime.now)
    created should not beNull
  }
  def save = {
    val entity = Member.findAll().head
    val saved = Member.save(entity)
    saved should not equalTo (entity)
  }
  def delete = {
    val entity = Member.findAll().head
    Member.delete(entity)
    val shouldBeNone = Member.find(123)
    shouldBeNone.isDefined should beFalse
  }

}
*/

/*
import org.scalatest._
import org.joda.time._

class MemberSpec extends FlatSpec with ShouldMatchers {

  behavior of "Member"

  it should "find by primary keys" in {
    val maybeFound = Member.find(123)
    maybeFound.isDefined should be(true)
  }
  it should "find all records" in {
    val allResults = Member.findAll()
    allResults.size should be > (0)
  }
  it should "count all records" in {
    val count = Member.countAll()
    count should be > (0L)
  }
  it should "find by where clauses" in {
    val results = Member.findAllBy("ID = {id}", 'id -> 123)
    results.size should be > (0)
  }
  it should "count by where clauses" in {
    val count = Member.countBy("ID = {id}", 'id -> 123)
    count should be > (0L)
  }
  it should "create new record" in {
    val created = Member.create(name = "MyString", createdAt = DateTime.now)
    created should not be (null)
  }
  it should "save a record" in {
    val entity = Member.findAll().head
    val saved = Member.save(entity)
    saved should not eq (entity)
  }
  it should "delete a record" in {
    val entity = Member.findAll().head
    Member.delete(entity)
    val shouldBeNone = Member.find(123)
    shouldBeNone.isDefined should be(false)
  }

}
*/

