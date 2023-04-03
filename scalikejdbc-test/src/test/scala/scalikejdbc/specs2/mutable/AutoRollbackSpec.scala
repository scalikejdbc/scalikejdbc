package scalikejdbc.specs2.mutable

import java.sql.SQLException

import org.specs2.mutable.Specification
import scalikejdbc._
import org.joda.time.DateTime
import scalikejdbc.NamedDB
import unit._

class AutoRollbackSpec
  extends Specification
  with DBSettings
  with PreparingTables {

  sequential

  // DB

  "Specification should work without AutoRollback" in {
    MutableMember.count() must_== (0)
    MutableMember.create(0, "Dummy")
    MutableMember.count() must_== (1)
  }

  "Transactions should be committed without AutoRollback" in {
    MutableMember.count() must_== (1)
    MutableMember.delete(0)
    MutableMember.count() must_== (0)
  }

  "mutable_members table must be empty" in new AutoRollback {
    MutableMember.count() must_== (0)
  }

  "AutoRollback should roll all operations back" in new AutoRollbackWithFixture {
    // AutoRollbackWithFixture insert 2 records
    MutableMember.count() must_== (2)

    MutableMember.create(3, "Chris")
    MutableMember.count() must_== (3)
  }

  "AutoRollback should roll back if fixture() failed" in {
    // step 2 failed with lock timeout if step 1 is not rolled back
    "step 1" in {
      new AutoRollbackWithWrongFixture {} must throwA[SQLException]
    }
    "step 2" in new AutoRollbackWithFixture {
      MutableMember.count() must_== (2)
    }
  }

  "mutable_members table must be empty after a test" in new AutoRollback {
    // all insertions should be rolled back
    MutableMember.count() must_== (0)
  }

  // NamedDB

  "Specification should work without AutoRollback for NamedDB" in {
    MutableMember2.count() must_== (0)
    MutableMember2.create(0, "Dummy")
    MutableMember2.count() must_== (1)
  }

  "Transactions should be committed without AutoRollback for NamedDB" in {
    MutableMember2.count() must_== (1)
    MutableMember2.delete(0)
    MutableMember2.count() must_== (0)
  }

  "mutable_members2 table must be empty" in new DB2AutoRollback {
    MutableMember2.count() must_== (0)
  }

  "AutoRollback should roll all operations back for NamedDB" in new DB2AutoRollbackWithFixture {
    // DB2AutoRollbackWithFixture insert 2 records
    MutableMember2.count() must_== (2)

    MutableMember2.create(3, "Chris")
    MutableMember2.count() must_== (3)
  }

  "mutable_members2 table must be empty after a test" in new DB2AutoRollback {
    // all insertions should be rolled back
    MutableMember2.count() must_== (0)
  }

}

trait AutoRollbackWithFixture extends AutoRollback {
  override def fixture(implicit session: DBSession): Unit = {
    SQL("insert into mutable_members values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
    SQL("insert into mutable_members values (?, ?, ?)")
      .bind(2, "Bob", DateTime.now)
      .update
      .apply()
  }
}

trait AutoRollbackWithWrongFixture extends AutoRollback {
  override def fixture(implicit session: DBSession): Unit = {
    SQL("insert into mutable_members values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
    // Primary key conflict
    SQL("insert into mutable_members values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
  }
}

trait DB2AutoRollbackWithFixture extends AutoRollback {
  override def db() = NamedDB("db2").toDB()
  override def fixture(implicit session: DBSession): Unit = {
    SQL("insert into mutable_members2 values (?, ?, ?)")
      .bind(1, "Alice", DateTime.now)
      .update
      .apply()
    SQL("insert into mutable_members2 values (?, ?, ?)")
      .bind(2, "Bob", DateTime.now)
      .update
      .apply()
  }
}

trait DB2AutoRollback extends AutoRollback {
  override def db() = NamedDB("db2").toDB()
}
