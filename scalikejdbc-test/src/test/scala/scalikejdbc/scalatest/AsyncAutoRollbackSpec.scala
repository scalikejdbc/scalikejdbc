package scalikejdbc.scalatest

import scalikejdbc._
import org.joda.time.DateTime
import scalikejdbc.NamedDB
import unit._

import scala.concurrent.Future
import org.scalatest.flatspec.FixtureAsyncFlatSpec
import org.scalatest.matchers.should.Matchers

trait AsyncFlatSpecWithCommonTraits
  extends FixtureAsyncFlatSpec
  with Matchers
  with DBSettings
  with PreparingTables

class AsyncAutoRollbackSpec
  extends AsyncFlatSpecWithCommonTraits
  with AsyncAutoRollback {

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

  behavior of "AsyncAutoRollbackFixture"

  it should "be prepared and be able to create a new record" in {
    implicit session =>
      Future {
        ScalaTestMember.count() should equal(2)
        ScalaTestMember.create(3, "Chris")
        ScalaTestMember.count() should equal(3)
      }
  }

  it should "be rolled back" in { implicit session =>
    Future {
      ScalaTestMember.count() should equal(2)
    }
  }

}

class NamedAsyncAutoRollbackSpec
  extends AsyncFlatSpecWithCommonTraits
  with AsyncAutoRollback {

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

  behavior of "Named AsyncAutoRollbackFixture"

  it should "be prepared and be able to create a new record for NamedDB" in {
    implicit session =>
      Future {
        ScalaTestMember2.count() should equal(2)
        ScalaTestMember2.create(3, "Chris")
        ScalaTestMember2.count() should equal(3)
      }
  }

  it should "be rolled back" in { implicit session =>
    Future {
      ScalaTestMember2.count() should equal(2)
    }
  }

}

class AsyncAutoRollbackWithNoArgTestFixtureSpec
  extends AsyncFlatSpecWithCommonTraits
  with AsyncAutoRollback
  with AsyncBufferMixin {

  override def db() = NamedDB("db2").toDB()

  behavior of "AsyncAutoRollback with NoArgTestFixture"

  it should "call withFixture(NoArgTest)" in { implicit session =>
    Future {
      // "test" is appended in BufferMixin
      assert(buffer.contains("test"))
    }
  }
}
