# ScalikeJDBC Mapper Generator

## How to use

### project/plugins.sbt

```
resolvers += "Sonatype OSS" at "http://oss.sonatype.org/content/repositories/releases"

// Don't forget adding your JDBC driver
libraryDependencies += "org.hsqldb" % "hsqldb" % "[2,)"

addSbtPlugin("com.github.seratch" %% "scalikejdbc-mapper-generator" % "1.3.3")
```

### project/scalikejdbc-mapper-generator.properties

```
jdbc.driver=org.hsqldb.jdbc.JDBCDriver
jdbc.url=jdbc:hsqldb:file:db/test
jdbc.username=sa
jdbc.password=
jdbc.schema=
generator.packageName=models
# generator.lineBreak: LF/CRLF
geneartor.lineBreak=LF
# generator.template: placeHolderSQL/anormSQL/executableSQL
generator.template=executableSQL
generator.encoding=UTF-8
```

### build.sbt

```
seq(scalikejdbcSettings: _*)
```

### Sbt command

```
sbt "scalikejdbc-gen [table-name]"
```

### Output example

From the following table:

```
create table member (
  id bigint generated always as identity,
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
import org.joda.time.{ LocalDate, DateTime }

case class Member(
    id: Long,
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime) {

  def save()(implicit session: DBSession = AutoSession): Member = Member.save(this)(session)

  def destroy()(implicit session: DBSession = AutoSession): Unit = Member.delete(this)(session)

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
      id = rs.long(id),
      name = rs.string(name),
      description = Option(rs.string(description)),
      birthday = Option(rs.date(birthday)).map(_.toLocalDate),
      createdAt = rs.timestamp(createdAt).toDateTime)
  }

  def find(id: Long)(implicit session: DBSession = AutoSession): Option[Member] = {
    SQL("""SELECT * FROM MEMBER WHERE ID = /*'id*/1""")
      .bindByName('id -> id).map(*).single.apply()
  }

  def findAll()(implicit session: DBSession = AutoSession): List[Member] = {
    SQL("""SELECT * FROM MEMBER""").map(*).list.apply()
  }

  def countAll()(implicit session: DBSession = AutoSession): Long = {
    SQL("""SELECT COUNT(1) FROM MEMBER""").map(rs => rs.long(1)).single.apply().get
  }

  def findBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = AutoSession): List[Member] = {
    SQL("""SELECT * FROM MEMBER WHERE """ + where)
      .bindByName(params: _*).map(*).list.apply()
  }

  def countBy(where: String, params: (Symbol, Any)*)(implicit session: DBSession = AutoSession): Long = {
    SQL("""SELECT count(1) FROM MEMBER WHERE """ + where)
      .bindByName(params: _*).map(rs => rs.long(1)).single.apply().get
  }

  def create(
    name: String,
    description: Option[String] = None,
    birthday: Option[LocalDate] = None,
    createdAt: DateTime)(implicit session: DBSession = AutoSession): Member = {
    val generatedKey = SQL("""
      INSERT INTO MEMBER (
        NAME,
        DESCRIPTION,
        BIRTHDAY,
        CREATED_AT
      ) VALUES (
        /*'name*/'abc',
        /*'description*/'abc',
        /*'birthday*/'1958-09-06',
        /*'createdAt*/'1958-09-06 12:00:00'
      )
      """)
      .bindByName(
        'name -> name,
        'description -> description,
        'birthday -> birthday,
        'createdAt -> createdAt
      ).updateAndReturnGeneratedKey.apply()
    Member(
      id = generatedKey,
      name = name,
      description = description,
      birthday = birthday,
      createdAt = createdAt)
  }

  def save(m: Member)(implicit session: DBSession = AutoSession): Member = {
    SQL("""
      UPDATE 
        MEMBER
      SET 
        ID = /*'id*/1,
        NAME = /*'name*/'abc',
        DESCRIPTION = /*'description*/'abc',
        BIRTHDAY = /*'birthday*/'1958-09-06',
        CREATED_AT = /*'createdAt*/'1958-09-06 12:00:00'
      WHERE 
        ID = /*'id*/1
      """)
      .bindByName(
        'id -> m.id,
        'name -> m.name,
        'description -> m.description,
        'birthday -> m.birthday,
        'createdAt -> m.createdAt
      ).update.apply()
    m
  }

  def delete(m: Member)(implicit session: DBSession = AutoSession): Unit = {
    SQL("""DELETE FROM MEMBER WHERE ID = /*'id*/1""")
      .bindByName('id -> m.id).update.apply()
  }

}
```
