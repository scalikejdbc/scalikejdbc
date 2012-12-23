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

[![Build Status](https://secure.travis-ci.org/seratch/scalikejdbc.png?branch=master)](http://travis-ci.org/seratch/scalikejdbc)


## Scaladoc

http://seratch.github.com/scalikejdbc/api/index.html#scalikejdbc.package


## Setup

### sbt

```scala
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.4,)",
  "postgresql" % "postgresql" % "9.1-901.jdbc4",  // your JDBC driver
  "org.slf4j" % "slf4j-simple" % "[1.7,)"         // slf4j implementation
)
```

### Maven

```xml
<dependency>
  <groupId>com.github.seratch</groupId>
  <artifactId>scalikejdbc_2.9.2</artifactId>
  <version>[1.4,)</version>
</dependency>
<dependency>
  <groupId>postgresql</groupId>
  <artifactId>postgresql</artifactId>
  <version>9.1-901.jdbc4</version>
</dependency>
<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-simple</artifactId>
  <version>[1.7,)</version>
</dependency>
```


## Try it now

Try ScalikeJDBC right now!

```sh

git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

"members" table is already created. You can execute queries as follows:

```scala
case class Member(id: Long, name: String)

val members = DB readOnly { implicit session => 
  SQL("select * from members")
    .map(rs => Member(rs.long("id"), rs.string("name")))
    .list.apply()
}

DB localTx { implicit session => 
  SQL("insert into members (id, name) values ({id}, {name})")
    .bindByName('id -> 3, 'name -> "Charles")
    .update.apply() 
}
```

If you need more information(connection management, transaction, CRUD), please check the following wiki page or scaladoc in detail.

https://github.com/seratch/scalikejdbc/wiki/GettingStarted


## Features

### Basic SQL template

The most basic way is just using prepared statement as follows.

```scala
SQL("""insert into members values (?, ?, ?, ?)""")
  .bind(132430, "bob@example.com", "Bob", "xfewSZe2sd3w")
  .update.apply()
```


### Anorm-like SQL template

Instead of embedding `?`(place holder), you can specify named place holder that is similar to [Anorm](http://www.playframework.org/documentation/2.0.1/ScalaAnorm). 

```scala
SQL("""insert into members values ({id}, {email}, {name}, {encryptedPassword})""")
  .bindByName(
    'id -> 132430,
    'email -> "bob@example.com",
    'name -> "Bob",
    'encryptedPassword -> "xfewSZe2sd3w")
  .update.apply()
```


### Executable SQL template

Instead of embedding `?`(place holder), you can specify executable SQL as template. Using this API, it's possible to validate SQL before building into application. 

Usage is simple. Use Scala Symbol literal in comment with dummy value in SQL template, and pass named values by using not `bind(Any*)` but `bindByName((Symbol, Any)*)`. When some of the passed names by `#bindByName` are not used, or `#bind` is used although the template seems to be executable SQL template, runtime exception will be thrown.

```scala
SQL("""
  insert into members values (
    /*'id*/123,
    /*'email*/'alice@example.com',
    /*'name*/'Alice',
    /*'encryptedPassword*/'123456789012')
""")
  .bindByName(
    'id -> 132430,
    'email -> "bob@example.com",
    'name -> "Bob",
    'encryptedPassword -> "xfewSZe2sd3w")
  .update.apply()
```

### Flexible transactions

`DB.autoCommit { session => }`, `DB.localTx { session => }`, `DB.withinTx { session => }` and 'DB.readOnly { session => }` are supported.

In addition, passing `AutoSession` as an implicit parameter is quite useful. Like this:

```scala
object Member {
  def find(id: Long)(implicit session: DBSession = AutoSession): Option[Member] = {
    SQL("select * from members where id = ?").bind(id).map(*).single.apply() 
  }
  def setProfileVerified(member: Member)(implicit session: DBSession = AutoSession) = {
    SQL("update members set profile_verified = true where id = ?").bind(id).update.apply()
  }
}

Member.find(id) // new read-only session provided by AutoSession

Member.setProfileVerified(member) // new auto-commit session provided by AutoSession

DB localTx { implicit session =>
  // transaction start
  Member.findByName(name).foreach { member => 
    member.setProfileVerified(member)
  } 
  val mightBeUpdated = Member.find(123) 
  // transaction end
}
```

### SQLInterpolation for Scala 2.10

This feature is still experimental, but you can try it now.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-interpolation

```scala
def create(id: Long, email: String, name: String, encryptedPassword: Sting) {
  sql"insert into members values (${id}, ${email}, ${name}, ${encryptedPassword})"
    .update.apply()
}
```


### Logging SQL And Timing

Using LogginSQLAndTime feature, you can check the actual SQL(not exactly) and time.

https://github.com/seratch/scalikejdbc/wiki/LoggingSQLAndTime


### Source code generator

If you want to create mapper modules easily, also take a look at this sbt plugin. 

```sh
sbt "scalikejdbc-gen [table-name (class-name)]"
```

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-mapper-generator


### Play framework 2.x support

You can use ScalikeJDBC with Play framework 2.x seamlessly.

https://github.com/seratch/scalikejdbc/tree/master/scalikejdbc-play-plugin

We promise you that it becomes more useful when using together with scalikejdbc-mapper-generator.


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


## License

Apache License, Version 2.0

http://www.apache.org/licenses/LICENSE-2.0.html

