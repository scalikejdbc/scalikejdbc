# ScalikeJDBC

![ScalikeJDBC Logo](http://scalikejdbc.org/img/logo.png)

## Just write SQL and get things done!

ScalikeJDBC is a tidy SQL-based DB access library for Scala developers. This library naturally wraps JDBC APIs and provides you easy-to-use APIs.


## Supported RDBMS

We never release without passing all the unit tests with the following RDBMS.

- PostgreSQL
- MySQL 
- H2 Database Engine
- HSQLDB

ScalikeJDBC uses JDBC drivers, so we believe that ScalikeJDBC basically works with any other RDBMS.


## Setup

### sbt

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"               % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "org.postgresql"     %  "postgresql"                % "9.2-1003-jdbc4", // your JDBC driver
  "org.slf4j"          %  "slf4j-simple"              % "[1.7,)"          // slf4j implementation
)
```

## Try it now

Try ScalikeJDBC right now!

```sh
git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console

// simple query
// val ids = withSQL { select(u.id).from(Member as m).orderBy(m.id) }.map(_.long(1)).list.apply()
```

## Basic usage

### Scala 2.10.x

The combination of SQLInterpolation and SQLSyntaxSupport is much powerful.

```scala
case class Member(id: Long, name: String, groupId: Option[Long], group: Option[Group])

object Member extends SQLSyntaxSupport[Member] {
  // can be omitted if your table name is "member"
  override val tableName = "members" 
  // can be omitted if column names are just snake-case'd from fields
  override val columnNames = Seq("id", "member_name", "group_id")
  // converts field names to column names
  override val nameConverters = Map("^name$" -> "member_name")

  def apply(u: SyntaxProvider[Member])(rs: WrappedResultSet): Member = { ... }
  def apply(u: SyntaxProvider[Member], g: SyntaxProvider[Group])(rs: WrappedResultSet): Member = { ... }
}

case class Group(id: Long, name: Option[String] = None)
object Group extends SQLSyntaxSupport[Group] { 
  override val tableName = "groups" 
  def apply(g: SyntaxProvider[Group])(rs: WrappedResultSet): Group = { ... }
}

val (m, g) = (Member.syntax("m"), Group.sytnax("g"))
val members: List[Member] = DB readOnly { implicit session =>
  withSQL { 
    select
      .from(Member as m)
      .leftJoin(Group as g).on(m.groupId, g.id)
      .where.eq(m.id, 123) 
      .orderBy(m.createdAt).desc
      .limit(20)
      .offset(0)
  }.map(Member(m, g)).list.apply()
}

// or using SQLInterpolation directly
val members: List[Member] = DB readOnly { implicit session =>
  sql"""
    select ${m.result.*}, ${g.result.*} 
    from ${Member as m} left join ${Group as g} on ${m.groupId} = ${g.id} 
    where ${m.id} = ${123}
    order by ${m.createdAt} desc limit ${limit} offset ${offset}
  """.map(Member(m, g)).list.apply()
}

val name = Some("Chris")
val newbie: Member = DB localTx { implicit session =>
  val id = withSQL { insert.into(Member).values(name) }.updateAndReturnGeneratedKey.apply()
  Member(id, name)
}

DB localTx { implicit session =>
  withSQL {
    update(Member as m).set(m.name -> "Bobby", m.updatedAt -> DateTime.now)
      .where.eq(m.id, 123)
  }.update.apply()

  withSQL { delete.from(Member).where.eq(Member.column.id, 123) }.update.apply()
}
```

More examples:

https://github.com/seratch/scalikejdbc/blob/master/scalikejdbc-interpolation/src/test/scala/scalikejdbc/QueryInterfaceSpec.scala

### Scala 2.9.x

Basically, use string template. Indeed, it's an old style but still good.

```scala
case class Member(id: Long, name: Option[String] = None)
val * = (rs: WrappedResultSet) => Member(rs.long("id"), rs.stringOpt("name"))
val members: List[Member] = DB readOnly { implicit session => 
  SQL("select id, name from members where id = ?").bind(123).map(*).list.apply()
}
val name = Some("Chris")

val newMember: Member = DB localTx { implicit session => 
  val id = SQL("insert into members values ({name})")
    .bindByName('name -> name)).updateAndReturnGeneratedKey.apply() 
  Member(id, name)
}
```

If you need more information(connection management, transaction, CRUD), please check the following wiki page or scaladoc in detail.

https://github.com/seratch/scalikejdbc/wiki/GettingStarted


### Typesafe Activator template

Also check the [Typesafe Activator](http://typesafe.com/activator) example: 

http://typesafe.com/activator/template/scalikejdbc-activator-template

https://github.com/seratch/hello-scalikejdbc


## Features

### Easy-to-use connection management

`ConnectionPool` object is so easy-to-use API.

```scala
// Just load the jdbc driver and register connection pool
Class.forName("org.h2.Driver")
ConnectionPool.singleton("jdbc:h2:file:db/test", "sa", "")

// Now DB operations are available
val idList: List[Long] = DB readOnly { implicit session =>
  sql"select id from members".map(_.long("id")).list.apply()
}
````

### Flexible transactions

`DB.autoCommit { session => }`, `DB.localTx { session => }`, `DB.withinTx { session => }` and `DB.readOnly { session => }` are supported.

In addition, passing `AutoSession` as an implicit parameter is quite useful. Like this:

```scala
object Member {
  def find(id: Long)(implicit session: DBSession = AutoSession): Option[Member] = {
    sql"select * from members where id = ${id}".map(*).single.apply() 
  }
  def setProfileVerified(member: Member)(implicit session: DBSession = AutoSession) = {
    sql"update members set profile_verified = true where id = ${member.id}".update.apply()
  }
}

Member.find(id) // new read-only session provided by AutoSession

Member.setProfileVerified(member) // new auto-commit session provided by AutoSession

DB localTx { implicit session =>
  // begin transaction 
  Member.findByName(name).foreach { member => 
    member.setProfileVerified(member)
  } 
  val mightBeUpdated = Member.find(id) 
  // end transaction
}
```


### Logging SQL and timing

Using LogginSQLAndTime feature, you can check the actual SQL(not exactly) and time.

https://github.com/seratch/scalikejdbc/wiki/LoggingSQLAndTime


### Source code generator

If you want to create mapper modules easily, also take a look at this sbt plugin. 

```sh
sbt "scalikejdbc-gen [table-name (class-name)]"
```

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-mapper-generator

### Testing support

Testing support for ScalaTest:

```scala
class AutoRollbackSpec extends fixture.FlatSpec with AutoRollback {

  override def fixture(implicit session: DBSession) {
    val (id, name, createdAt) = (1, "Alice", DateTime.now)
    sql"insert into members values (${id}, ${name}, ${createdAt})".update.apply()
  }

  it should "create a new record" in { implicit session =>
    val before = Member.count() 
    Member.create(3, "Chris")
    Member.count() should equal(before + 1)
  }

}
```

for specs2(unit):

```scala
object MemberSpec extends Specification {

  "Member should create a new record" in new AutoRollback {
    val before = Member.count()
    Member.create(3, "Chris")
    Member.count() must_==(before + 1) 
  }

  trait AutoRollbackWithFixture extends AutoRollback {
    override def fixture(implicit session: DBSession) { ... }
  }

  "Member should ..." in new AutoRollbackWithFixture { ... }

}
```

Support for specs2(acceptance) is also available. See in detail:

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-test


### Play! Framework 2.x support

```
10000:scalikejdbc.PlayPlugin
11000:scalikejdbc.PlayFixturePlugin
```

You can use ScalikeJDBC with Play framework 2.x seamlessly. We promise you that it becomes more productive when using together with scalikejdbc-mapper-generator.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-plugin

We also provides fixture for Play apps.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-fixture-plugin


### Typesafe Config support

This is an easy-to-use configuration loader for ScalikeJDBC which reads typesafe config.

https://github.com/seratch/scalikejdbc/tree/develop/scalikejdbc-config


### ScalikeJDBC-Async (Extension)

ScalikeJDBC-Async provides non-blocking APIs to talk with PostgreSQL and MySQL in the JDBC way.

This library is built with postgrsql-async and mysql-async,incredible works by @mauricio.

https://github.com/seratch/scalikejdbc-async


### dbconsole

`dbconsole` is an extended sbt console to connect database. Try it now!

- Mac OS X, Linux

```sh
curl -L http://git.io/dbconsole | sh
```

- Windows

```sh
http://git.io/dbconsole.bat
```

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-cli


### ScalikeJDBC Cookbook on the Kindle store

"ScalikeJDBC Cookbook" is the e-book for ScalikeJDBC users.

https://github.com/seratch/scalikejdbc-cookbook


## License

Published binary files have the following copyright:

```
Copyright 2013 ScalikeJDBC committers

Apache License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.html
```

