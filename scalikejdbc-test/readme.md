# ScalikeJDBC Testing Support

### project/Build.scala

With [ScalaTest](http://scalatest.org/):

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"      % "[1.4,)",
  "com.github.seratch" %% "scalikejdbc-test" % "[1.4,)" % "test",
  "org.scalatest"      %% "scalatest"        % "[1.8,)" % "test"
)
```

or with [specs2](http://specs2.org/):

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"      % "[1.4,)",
  "com.github.seratch" %% "scalikejdbc-test" % "[1.4,)"  % "test",
  "org.specs2"         %% "specs2"           % "[1.12,)" % "test"
)
```

### ScalaTest

`AutoRollback` trait provides automatic rollback after each test and data fixture.

```scala
import scalikejdbc._
import scalikejdbc.scalatest.AutoRollback
import org.joda.time.DateTime
import org.scalatest.fixture.FlatSpec

class AutoRollbackSpec extends FlatSpec with AutoRollback {

  // override def db = NamedDB('anotherdb).toDB

  override def fixture(implicit session: DBSession) {
    SQL("insert into members values (?, ?, ?)").bind(1, "Alice", DateTime.now).update.apply()
    SQL("insert into members values (?, ?, ?)").bind(2, "Bob", DateTime.now).update.apply()
  }

  behavior of "Members"

  it should "create a new record" in { implicit session =>
    val before = Member.count() 
    Member.create(3, "Chris")
    Member.count() should equal(before + 1)
  }

}
```

### specs2

`AutoRollback` trait provides automatic rollback after each test and data fixture.

- unit

```scala
import scalikejdbc._
import scalikejdbc.specs2.mutable.AutoRollback
import org.joda.time.DateTime
import org.specs2.mutable.Specification

object MemberSpec extends Specification {

  sequential

  "Member should create a new record" in new AutoRollback {
    val before = Member.count()
    Member.create(3, "Chris")
    Member.count() must_==(before + 1) 
  }

  "Member should ... " in new AutoRollbackWithFixture {
    ...
  }

}

trait AutoRollbackWithFixture extends AutoRollback {
  // override def db = NamedDB('db2).toDB
  override def fixture(implicit session: DBSession) {
    SQL("insert into members values (?, ?, ?)").bind(1, "Alice", DateTime.now).update.apply()
    SQL("insert into members values (?, ?, ?)").bind(2, "Bob", DateTime.now).update.apply()
  }
}

```

- acceptance

```scala
import scalikejdbc._
import scalikejdbc.specs2.AutoRollback
import org.joda.time.DateTime
import org.specs2.Specification

class MemberSpec extends Specification { def is =

  args(sequential = true) ^
  "Member should create a new record" ! autoRollback().create
  end

  case class autoRollback() extends AutoRollback {

    // override def db = NamedDB('db2).toDB
    // override def fixture(implicit session: DBSession) { ... }

    def create = this {
      val before = Member.count()
      Member.create(3, "Chris")
      Member.count() must_==(before + 1) 
    }
  }

}
```


