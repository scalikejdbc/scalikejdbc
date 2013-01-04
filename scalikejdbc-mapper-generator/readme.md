# ScalikeJDBC Mapper Generator

## How to use

### project/plugins.sbt

```
resolvers += "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases"

// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "[1.4,)")
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
package com.example

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
      

object Member {

  val tableName = "MEMBER"

  object columnNames {
    val id = "ID"
    val name = "NAME"
    val description = "DESCRIPTION"
    val birthday = "BIRTHDAY"
    val createdAt = "CREATED_AT"
    val all = Seq(id, name, description, birthday, createdAt)
  }
      
  val * = {
    import columnNames._
    (rs: WrappedResultSet) => Member(
      id = rs.int(id),
      name = rs.string(name),
      description = rs.stringOpt(description),
      birthday = rs.dateOpt(birthday).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }
      
  object joinedColumnNames {
    val delimiter = "__ON__"
    def as(name: String) = name + delimiter + tableName
    val id = as(columnNames.id)
    val name = as(columnNames.name)
    val description = as(columnNames.description)
    val birthday = as(columnNames.birthday)
    val createdAt = as(columnNames.createdAt)
    val all = Seq(id, name, description, birthday, createdAt)
    val inSQL = columnNames.all.map(name => tableName + "." + name + " AS " + as(name)).mkString(", ")
  }
      
  val joined = {
    import joinedColumnNames._
    (rs: WrappedResultSet) => Member(
      id = rs.int(id),
      name = rs.string(name),
      description = rs.stringOpt(description),
      birthday = rs.dateOpt(birthday).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }
      
  val autoSession = AutoSession

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Member] = {
    sql"""SELECT * FROM MEMBER WHERE ID = ${id}""".map(*).single.apply()
  }
          
  def findAll()(implicit session: DBSession = autoSession): List[Member] = {
    sql"""SELECT * FROM MEMBER""".map(*).list.apply()
  }
          
  def countAll()(implicit session: DBSession = autoSession): Long = {
    sql"""SELECT COUNT(1) FROM MEMBER""".map(rs => rs.long(1)).single.apply().get
  }
          
  def findAllBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): List[Member] = {
    SQL("""SELECT * FROM MEMBER WHERE """ + where)
      .bindByName(params: _*).map(*).list.apply()
  }
      
  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = autoSession): Long = {
    SQL("""SELECT count(1) FROM MEMBER WHERE """ + where)
      .bindByName(params: _*).map(rs => rs.long(1)).single.apply().get
  }
      
  def create(
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = autoSession): Member = {
    val generatedKey = sql"""
      INSERT INTO MEMBER (
        NAME,
        DESCRIPTION,
        BIRTHDAY,
        CREATED_AT
      ) VALUES (
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
      UPDATE
        MEMBER
      SET
        ID = ${m.id},
        NAME = ${m.name},
        DESCRIPTION = ${m.description},
        BIRTHDAY = ${m.birthday},
        CREATED_AT = ${m.createdAt}
      WHERE
        ID = ${m.id}
      """.update.apply()
    m
  }
        
  def delete(m: Member)(implicit session: DBSession = autoSession): Unit = {
    sql"""DELETE FROM MEMBER WHERE ID = ${m.id}""".update.apply()
  }
        
}
```

And specs2 or ScalaTest's FlatSpec.


```scala
package com.example

import scalikejdbc.specs2.mutable.AutoRollback
import org.specs2.mutable._
import org.joda.time._

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
```
