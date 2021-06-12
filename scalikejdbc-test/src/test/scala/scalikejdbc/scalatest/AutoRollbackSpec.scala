package scalikejdbc.scalatest

import scalikejdbc._
import org.joda.time.DateTime
import scalikejdbc.NamedDB
import unit._
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.matchers.should.Matchers

trait FlatSpecWithCommonTraits
  extends FixtureAnyFlatSpec
  with Matchers
  with DBSettings
  with PreparingTables

class AutoRollbackSpec extends FlatSpecWithCommonTraits with AutoRollback {

  override def fixture(implicit session: DBSession): Unit = {
    SQL("insert into ScalaTest_members values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
    SQL("insert into ScalaTest_members values (?, ?, ?)")
      .bind(2, "Bob", DateTime.now)
      .update
      .apply()
  }

  behavior of "AutoRollbackFixture"

  it should "be prepared and be able to create a new record" in {
    implicit session =>
      ScalaTestMember.count() should equal(2)
      ScalaTestMember.create(3, "Chris")
      ScalaTestMember.count() should equal(3)
  }

  it should "be rolled back" in { implicit session =>
    ScalaTestMember.count() should equal(2)
  }

}

class NamedAutoRollbackSpec extends FlatSpecWithCommonTraits with AutoRollback {

  override def db() = NamedDB("db2").toDB()

  override def fixture(implicit session: DBSession): Unit = {
    SQL("insert into scalatest_members2 values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
    SQL("insert into scalatest_members2 values (?, ?, ?)")
      .bind(2, "Bob", DateTime.now)
      .update
      .apply()
  }

  behavior of "Named AutoRollbackFixture"

  it should "be prepared and be able to create a new record for NamedDB" in {
    implicit session =>
      ScalaTestMember2.count() should equal(2)
      ScalaTestMember2.create(3, "Chris")
      ScalaTestMember2.count() should equal(3)
  }

  it should "be rolled back" in { implicit session =>
    ScalaTestMember2.count() should equal(2)
  }

}

class AutoRollbackWithNoArgTestFixtureSpec
  extends FlatSpecWithCommonTraits
  with AutoRollback
  with BufferMixin {

  override def db() = NamedDB("db2").toDB()

  behavior of "AutoRollback with NoArgTestFixture"

  it should "call withFixture(NoArgTest)" in { implicit session =>
    // "test" is appended in BufferMixin
    assert(buffer.contains("test"))
  }
}
