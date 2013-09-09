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


## Setup

### sbt

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "org.postgresql"     %  "postgresql"  % "9.2-1003-jdbc4", // your JDBC driver
  "org.slf4j"          % "slf4j-simple" % "[1.7,)"         // slf4j implementation
)
```

## Try it now

Try ScalikeJDBC right now!

```sh
git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console

// simple query
// val ids = withSQL { select(u.id).from(User as u).orderBy(u.id) }.map(_.long(1)).list.apply()
```

## Basic usage

### Scala 2.10.x

SQLInterpolation and SQLSyntaxSupport is much powerful.

```scala
case class User(id: Long, name: String, groupId: Option[Long], group: Option[Group])
object User extends SQLSyntaxSupport[User] {
  def apply(u: SyntaxProvider[User])(rs: WrappedResultSet): User = { ... }
  def apply(u: SyntaxProvider[User], g: SyntaxProvider[Group])(rs: WrappedResultSet): User = { ... }
}

case class Group(id: Long, name: Option[String] = None)
object Group extends SQLSyntaxSupport[Group] { 
  def apply(g: SyntaxProvider[Group])(rs: WrappedResultSet): Group = { ... }
}

val (u, g) = (User.syntax("u"), Group.sytnax("g"))
val users: List[User] = DB readOnly { implicit session =>
  withSQL { 
    select
      .from(User as u)
      .leftJoin(Group as g).on(u.groupId, g.id)
      .where.eq(u.id, 123) 
      .orderBy(u.createdAt).desc
      .limit(20)
      .offset(0)
  }.map(User(u, g)).list.apply()
}

  // or using SQLInterpolation directly
val users: List[User] = DB readOnly { implicit session =>
  sql"""
    select ${u.result.*}, ${g.result.*} 
    from ${User as u} left join ${Group as g} on ${u.groupId} = ${g.id} 
    where ${u.id} = ${123}
    order by ${u.createdAt} desc limit ${limit} offset ${offset}
  """.map(User(u, g)).list.apply()
}

val name = Some("Chris")
val newUser: User = DB localTx { implicit session =>
  val id = withSQL { insert.into(User).values(name) }.updateAndReturnGeneratedKey.apply()
  User(id, name)
}

DB localTx { implicit session =>
  withSQL {
    update(User as u).set(u.name -> "Bobby", u.updatedAt -> DateTime.now)
      .where.eq(u.id, 123)
  }.update.apply()

  withSQL { delete.from(User).where.eq(User.column.id, 123) }.update.apply()
}
```

More examples:

https://github.com/seratch/scalikejdbc/blob/master/scalikejdbc-interpolation/src/test/scala/scalikejdbc/QueryInterfaceSpec.scala

### Scala 2.9.x

Basically, use string template. Indeed, it's an old style but still good.

```scala
case class User(id: Long, name: Option[String] = None)
val * = (rs: WrappedResultSet) => User(rs.long("id"), rs.stringOpt("name"))
val users: List[User] = DB readOnly { implicit session => 
  SQL("select id, name from users where id = ?").bind(123).map(*).list.apply()
}
val name = Some("Chris")

val newUser: User = DB localTx { implicit session => 
  val id = SQL("insert into users values ({name})")
    .bindByName('name -> name)).updateAndReturnGeneratedKey.apply() 
  User(id, name)
}
```

If you need more information(connection management, transaction, CRUD), please check the following wiki page or scaladoc in detail.

https://github.com/seratch/scalikejdbc/wiki/GettingStarted


## Features

### Easy-to-use connection management

`ConnectionPool` object is so easy-to-use API.

```scala
// Just load the jdbc driver and register connection pool
Class.forName("org.h2.Driver")
ConnectionPool.singleton("jdbc:h2:file:db/test", "sa", "")

// Now DB operations are available
val idList: List[Long] = DB readOnly { implicit session =>
  sql"select id from users".map(_.long("id")).list.apply()
}
````

### Flexible transactions

`DB.autoCommit { session => }`, `DB.localTx { session => }`, `DB.withinTx { session => }` and `DB.readOnly { session => }` are supported.

In addition, passing `AutoSession` as an implicit parameter is quite useful. Like this:

```scala
object User {
  def find(id: Long)(implicit session: DBSession = AutoSession): Option[User] = {
    sql"select * from users where id = ${id}").map(*).single.apply() 
  }
  def setProfileVerified(member: User)(implicit session: DBSession = AutoSession) = {
    sql"update users set profile_verified = true where id = ${member.id}").update.apply()
  }
}

User.find(id) // new read-only session provided by AutoSession

User.setProfileVerified(member) // new auto-commit session provided by AutoSession

DB localTx { implicit session =>
  // begin transaction 
  User.findByName(name).foreach { member => 
    member.setProfileVerified(member)
  } 
  val mightBeUpdated = User.find(id) 
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
    sql"insert into users values (${id}, ${name}, ${createdAt})").update.apply()
  }

  it should "create a new record" in { implicit session =>
    val before = User.count() 
    User.create(3, "Chris")
    User.count() should equal(before + 1)
  }

}
```

for specs2(unit):

```scala
object UserSpec extends Specification {

  "User should create a new record" in new AutoRollback {
    val before = User.count()
    User.create(3, "Chris")
    User.count() must_==(before + 1) 
  }

  trait AutoRollbackWithFixture extends AutoRollback {
    override def fixture(implicit session: DBSession) { ... }
  }

  "User should ..." in new AutoRollbackWithFixture { ... }

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

