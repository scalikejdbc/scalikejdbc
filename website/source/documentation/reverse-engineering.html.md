---
title: Reverse Engineering - ScalikeJDBC
---

## Reverse Engineering

<hr/>
### How to setup

See [/documentation/setup](/documentation/setup.html).

<hr/>
### Sbt command

```sh
sbt "scalikejdbc-gen [table-name (class-name)]"
```

e.g.

```sh
sbt "scalikejdbc-gen company"
sbt "scalikejdbc-gen companies Company"
```

<hr/>
### Output example

From the following table:

```sql
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

```java
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

  def save()(implicit session: DBSession = Member.autoSession): Member = Member.save(this)(session)

  def destroy()(implicit session: DBSession = Member.autoSession): Unit = Member.destroy(this)(session)

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
    withSQL {
      select.from(Member as m).where.eq(m.id, id)
    }.map(Member(m.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    withSQL(select.from(Member as m)).map(Member(m.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Member as m)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Member] = {
    withSQL {
      select.from(Member as m).where.append(sqls"${where}")
    }.map(Member(m.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Member as m).where.append(sqls"${where}")
    }.map(_.long(1)).single.apply().get
  }

  def create(
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    val generatedKey = withSQL {
      insert.into(Member).columns(
        column.name,
        column.description,
        column.birthday,
        column.createdAt
      ).values(
        name,
        description,
        birthday,
        createdAt
      )
    }.updateAndReturnGeneratedKey.apply()

    Member(
      id = generatedKey.toInt,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def save(m: Member)(implicit session: DBSession = autoSession): Member = {
    withSQL {
      update(Member as m).set(
        m.id -> m.id,
        m.name -> m.name,
        m.description -> m.description,
        m.birthday -> m.birthday,
        m.createdAt -> m.createdAt
      ).where.eq(m.id, m.id)
    }.update.apply()
    m
  }

  def destroy(m: Member)(implicit session: DBSession = autoSession): Unit = {
    withSQL { delete.from(Member).where.eq(column.id, m.id) }.update.apply()
  }

}
```

And specs2 or ScalaTest's FlatSpec.


```java
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
      val results = Member.findAllBy(sqls.eq(m.id, 123))
      results.size should be_>(0)
    }
    "count by where clauses" in new AutoRollback {
      val count = Member.countBy(sqls.eq(m.id, 123))
      count should be_>(0L)
    }
    "create new record" in new AutoRollback {
      val created = Member.create(name = "MyString", createdAt = DateTime.now)
      created should not beNull
    }
    "save a record" in new AutoRollback {
      val entity = Member.findAll().head
      val updated = Member.save(entity)
      updated should not equalTo(entity)
    }
    "destroy a record" in new AutoRollback {
      val entity = Member.findAll().head
      Member.destroy(entity)
      val shouldBeNone = Member.find(123)
      shouldBeNone.isDefined should beFalse
    }
  }

}
```
