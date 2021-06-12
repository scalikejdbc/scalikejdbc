package scalikejdbc.specs2

import org.specs2.Specification
import scalikejdbc._
import org.joda.time.DateTime
import unit._

class AutoRollbackSpec
  extends Specification
  with DBSettings
  with PreparingTables {

  def is =
    args(sequential = true) ^
      "Specification should work without AutoRollback" ! withoutAutoRollback ^
      "Transactions should be committed without AutoRollback" ! shouldBeCommittedWithoutAutoRollback ^
      "members table must be empty" ! autoRollback().beforeTest ^
      "AutoRollback should roll all operations back" ! autoRollbackWithFixture().shouldBeRolledBack ^
      "members table must be empty after a test" ! autoRollback().afterTest ^
      "Specification should work without AutoRollback for NamedDB" ! db2WithoutAutoRollback ^
      "Transactions should be committed without AutoRollback for NamedDB" ! db2ShouldBeCommittedWithoutAutoRollback ^
      "members2 table must be empty" ! db2AutoRollback().beforeTest ^
      "AutoRollback should roll all operations back for NamedDB" ! db2AutoRollbackWithFixture().test ^
      "members2 table must be empty after a test" ! db2AutoRollback().afterTest ^ end

  def withoutAutoRollback = {
    Member.count() must_== (0)
    Member.create(0, "Dummy")
    Member.count() must_== (1)
  }

  def shouldBeCommittedWithoutAutoRollback = {
    Member.count() must_== (1)
    Member.delete(0)
    Member.count() must_== (0)
  }

  case class autoRollback() extends AutoRollback {
    def beforeTest = this {
      Member.count() must_== (0)
    }
    def afterTest = this {
      // all insertions should be rolled back
      Member.count() must_== (0)
    }
  }

  case class autoRollbackWithFixture() extends AutoRollback {
    override def fixture(implicit session: DBSession): Unit = {
      SQL("insert into members values (?, ?, ?)")
        .bind(1, "Alice", DateTime.now)
        .update
        .apply()
      SQL("insert into members values (?, ?, ?)")
        .bind(2, "Bob", DateTime.now)
        .update
        .apply()
    }

    def shouldBeRolledBack = this {
      // MemberAutoRollbackWithFixture insert 2 records
      Member.count() must_== (2)
      Member.create(3, "Chris")
      Member.count() must_== (3)
    }
  }

  // NamedDB

  def db2WithoutAutoRollback = {
    Member2.count() must_== (0)
    Member2.create(0, "Dummy")
    Member2.count() must_== (1)
  }

  def db2ShouldBeCommittedWithoutAutoRollback = {
    Member2.count() must_== (1)
    Member2.delete(0)
    Member2.count() must_== (0)
  }

  case class db2AutoRollback() extends AutoRollback {
    override def db() = NamedDB("db2").toDB()

    def beforeTest = this {
      Member2.count() must_== (0)
    }

    def afterTest = this {
      // all insertions should be rolled back
      Member2.count() must_== (0)
    }
  }

  case class db2AutoRollbackWithFixture() extends AutoRollback {
    override def db() = NamedDB("db2").toDB()

    override def fixture(implicit session: DBSession): Unit = {
      SQL("insert into members2 values (?, ?, ?)")
        .bind(1, "Alice", DateTime.now)
        .update
        .apply()
      SQL("insert into members2 values (?, ?, ?)")
        .bind(2, "Bob", DateTime.now)
        .update
        .apply()
    }

    def test = this {
      // db2AutoRollbackWithFixture insert 2 records
      Member2.count() must_== (2)
      Member2.create(3, "Chris")
      Member2.count() must_== (3)
    }
  }

}
