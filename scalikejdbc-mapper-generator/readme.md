# ScalikeJDBC Mapper Generator

## How to use

### project/plugins.sbt

```
resolvers += "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases"

// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "[1.5,)")
```

### project/scalikejdbc.properties

```
jdbc.driver=org.hsqldb.jdbc.JDBCDriver
jdbc.url=jdbc:hsqldb:file:db/test
jdbc.username=sa
jdbc.password=
jdbc.schema=
generator.packageName=models
# generator.lineBreak: LF/CRLF
geneartor.lineBreak=LF
# generator.template: basic/namedParameters/executable/interpolation
generator.template=interpolation
# generator.testTemplate: specs2unit/specs2acceptance/ScalaTestFlatSpec
generator.testTemplate=specs2unit
generator.encoding=UTF-8
```

### build.sbt

```
seq(scalikejdbcSettings: _*)
```

### Sbt command

```
sbt "scalikejdbc-gen [table-name (class-name)]"
```

### Output example

From the following table:

```
create table member (
  id int generated always as identity,
  name varchar(30) not null,
  description varchar(1000),
  birthday date,
  created_at timestamp not null,
  primary key(id)
)
```

This tool will generate the following Scala source code:

```scala
package models

import scalikejdbc._
import scalikejdbc.SQLInterpolation._
import org.joda.time.{LocalDate, DateTime}

case class Member(
  id: Int,
  name: String,
  description: Option[String] = None,
  birthday: Option[LocalDate] = None,
  createdAt: DateTime) {

  def save()(implicit session: DBSession = Member.autoSession): Member = Member.update(this)(session)

  def destroy()(implicit session: DBSession = Member.autoSession): Unit = Member.delete(this)(session)

}


object Member extends SQLSyntaxSupport[Member] {

  override val tableName = "MEMBER"

  override val columns = Seq("ID", "NAME", "DESCRIPTION", "BIRTHDAY", "CREATED_AT")

  def apply(m: ResultName[Member])(rs: WrappedResultSet): Member = new Member(
    id = rs.int(m.id),
    name = rs.string(m.name),
    description = rs.stringOpt(m.description),
    birthday = rs.dateOpt(m.birthday).map(_.toLocalDate),
    createdAt = rs.timestamp(m.createdAt).toDateTime
  )

  val m = Member.syntax("m")

  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Member] = {
    sql"""select ${m.result.*} from ${Member as m} where ID = ${id}"""
      .map(Member(m.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    sql"""select ${m.result.*} from ${Member as m}""".map(Member(m.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    sql"""select count(1) from ${Member.table}""".map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Member] = {
    sql"""select ${m.result.*} from ${Member as m} where ${where}"""
      .map(Member(m.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    sql"""select count(1) from ${Member as m} where ${where}"""
      .map(_.long(1)).single.apply().get
  }

  def create(
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    val generatedKey = sql"""
      insert into ${Member.table} (
        ${Member.column.name},
        ${Member.column.description},
        ${Member.column.birthday},
        ${Member.column.createdAt}
      ) values (
        ${name},
        ${description},
        ${birthday},
        ${createdAt}
      )
      """.updateAndReturnGeneratedKey.apply()

    Member(
      id = generatedKey.toInt,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def update(m: Member)(implicit session: DBSession = autoSession): Member = {
    sql"""
      update
        ${Member.table}
      set
        ${Member.column.id} = ${m.id},
        ${Member.column.name} = ${m.name},
        ${Member.column.description} = ${m.description},
        ${Member.column.birthday} = ${m.birthday},
        ${Member.column.createdAt} = ${m.createdAt}
      where
        ${Member.column.id} = ${m.id}
      """.update.apply()
    m
  }

  def delete(m: Member)(implicit session: DBSession = autoSession): Unit = {
    sql"""delete from ${Member.table} where ${Member.column.id} = ${m.id}""".update.apply()
  }

}
```

And specs2 or ScalaTest's FlatSpec.


```scala
package models

import scalikejdbc.specs2.mutable.AutoRollback
import org.specs2.mutable._
import org.joda.time._
import scalikejdbc.SQLInterpolation._

class MemberSpec extends Specification {

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
      val results = Member.findAllBy(sqls"ID = ${123}")
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Member.countBy(sqls"ID = ${123}")
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Member.create(name = "MyString", createdAt = DateTime.now)
      created should not beNull
    }
    "update a record" in new AutoRollback {
      val entity = Member.findAll().head
      val updated = Member.update(entity)
      updated should not equalTo(entity)
    }
    "delete a record" in new AutoRollback {
      val entity = Member.findAll().head
      Member.delete(entity)
      val shouldBeNone = Member.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
```
