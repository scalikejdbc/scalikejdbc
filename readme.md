# ScalikeJDBC - SQL-Based DB Access Library for Scala

## Just write SQL

ScalikeJDBC is A tidy SQL-based DB access library for Scala developers.

This library naturally wraps JDBC APIs and provides you easy-to-use APIs.

Users do nothing other than writing SQL and mapping from `java.sql.ResultSet` objects to Scala values. 

If you want to create mapper modules easily, also take a look at scalikejdbc-mapper-generator.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-mapper-generator


## Supported RDBMS

We never release without passing all the unit tests with the following RDBMS.

- PostgreSQL
- MySQL 
- H2 Database Engine
- HSQLDB

=======
[![Build Status](https://buildhive.cloudbees.com/job/seratch/job/scalikejdbc/badge/icon)](https://buildhive.cloudbees.com/job/seratch/job/scalikejdbc/)

## Setup

### sbt

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.5,)",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",  // your JDBC driver
  "org.slf4j" % "slf4j-simple" % "[1.7,)"         // slf4j implementation
)
```

If you're using Scala 2.10.x, using scalikejdbc-interpolation is highly recommended.

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.5,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.5,)",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",  // your JDBC driver
  "org.slf4j" % "slf4j-simple" % "[1.7,)"         // slf4j implementation
)
```

## Try it now

Try ScalikeJDBC right now!

```sh
git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

## Basic usage

### Scala 2.10

SQLInterpolation and SQLSyntaxSupport is much powerful.

```scala
case class User(id: Long, name: Option[String] = None)
object User extends SQLSyntaxSupport[User] {
  override val tableName = "users"
  def apply(u: ResultName[User])(rs: WrappedResultSet) = new User(rs.long(u.id), rs.stringOpt(u.name))
}

val u = User.syntax("u")
val users: List[User] = DB readOnly { implicit session =>
  sql"select ${u.result.*} from ${User as u}".map(User(u.resultName)).list.apply()
}

val name = Some("Chris")
val newUser: User = DB localTx { implicit session =>
  val id = sql"insert into ${User.table} values (${name})").updateAndReturnGeneratedKey.apply()
  User(id, name)
}
```

### Scala 2.9

Basically, use string template. Indeed, it's an old style but still good.

```scala
case class User(id: Long, name: Option[String] = None)

val * = (rs: WrappedResultSet) => User(rs.long("id"), rs.stringOpt("name"))

val users: List[User] = DB readOnly { implicit session => 
  SQL("select id, name from users").map(*).list.apply()
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


### Basic SQL Template

The most basic way is just using prepared statement as follows.

```scala
SQL("""insert into users values (?, ?)""")
  .bind(132430, Some("Bob")).update.apply()
```


### Named Parameters SQL Template

Instead of embedding `?`(place holder), you can specify named place holder that is similar to [Anorm](http://www.playframework.org/documentation/latest/ScalaAnorm). 

```scala
SQL("insert into users values ({id}, {name})")
  .bindByName('id -> 132430, 'name -> Some("Bob"))
  .update.apply()
```


### Executable SQL Template

Instead of embedding `?`(place holder), you can specify executable SQL as template. Using this API, it's possible to validate SQL before building into application. 

Usage is simple. Just specify Scala Symbol literal values inside of comments with dummy value in SQL template, and pass named values by using not `bind(Any*)` but `bindByName((Symbol, Any)*)`. When some of the passed names by `#bindByName` are not used, or `#bind` is used although the template seems to be executable SQL template, runtime exception will be thrown.

```scala
SQL("insert into users values (/*'id*/123, /*'name*/'Alice')")
  .bindByName('id -> 132430, 'name -> Some("Bob"))
  .update.apply()
```


### SQLInterpolation since Scala 2.10

New powerful SQL template using SIP-11 String Interpolation.

```scala
val name = "Martin"
val email = "martin@example.com"
val id = sql"insert into users values (${name}, ${email})".updateAndReturnGeneratedKey.apply()
```

See in detail:

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-interpolation


### Flexible transactions

`DB.autoCommit { session => }`, `DB.localTx { session => }`, `DB.withinTx { session => }` and `DB.readOnly { session => }` are supported.

In addition, passing `AutoSession` as an implicit parameter is quite useful. Like this:

```scala
object User {
  def find(id: Long)(implicit session: DBSession = AutoSession): Option[User] = {
    sql"select * from users where id = ${id}").map(*).single.apply() 
  }
  def setProfileVerified(member: User)(implicit session: DBSession = AutoSession) = {
    sql"update users set profile_verified = true where id = ${id}").update.apply()
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
    sql"insert into users values (${1}, ${"Alice"}, ${DateTime.now})").update.apply()
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

We also provides fixtures for Play apps.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-fixtures-plugin


### Typesafe Config support

This is an easy-to-use configuration loader for ScalikeJDBC which reads typesafe config.

https://github.com/seratch/scalikejdbc/tree/develop/scalikejdbc-config


### dbconsle

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

Apache License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.html

