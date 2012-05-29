# ScalikeJDBC - A thin JDBC wrapper in Scala


## Just write SQL

This is a thin JDBC wrapper library which just uses `java.sql.PreparedStatement` internally.

Users only need to write SQL and map from `java.sql.ResultSet` objects to Scala objects.

It's pretty simple, really.


If you want simple mappers, please also try scalikejdbc-mapper-generator.

https://github.com/seratch/scalikejdbc-mapper-generator


## Supported RDBMS

Passed all the unit tests with the following RDBMS.

- PostgreSQL
- MySQL 
- H2 Database Engine
- HSQLDB
- Apache Derby
- SQLite

[![Build Status](https://secure.travis-ci.org/seratch/scalikejdbc.png?branch=master)](http://travis-ci.org/seratch/scalikejdbc)


## Scaladoc

http://seratch.github.com/scalikejdbc/api/index.html#scalikejdbc.package


## Setup

### sbt

```scala
resolvers += "sonatype" at "http://oss.sonatype.org/content/repositories/releases"

libraryDependencies += "com.github.seratch" %% "scalikejdbc" % "1.2.3"

// slf4j binding you like
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"
```

### ls.implicit.ly

http://ls.implicit.ly/seratch/scalikejdbc

```
ls -n scalikejdbc
ls-install scalikejdbc
```


## Basic Usage

There are two ways.

The first one is just using `DBSession` methods:

```scala
import scalikejdbc._
import org.joda.time.LocalDate
case class User(id: Long, name: String, birthday: Option[LocalDate])

val activeUsers: List[User] = DB readOnly { session =>
  session.list("select * from user where active = ?", true) { 
    rs => User(rs.long("id"), rs.string("name"), Option(rs.date("birthday")).map(_.toLocalDate))
  }
}
```

The other is using API which starts with `SQL.apply`: 

```scala
import scalikejdbc._
import org.joda.time.LocalDate
case class User(id: Long, name: String, birthday: Option[LocalDate])

val activeUsers: List[User] = DB readOnly { implicit session =>
  SQL("select * from user where active = ?").bind(true).map { 
    rs => User(rs.long("id"), rs.string("name"), Option(rs.date("birthday")).map(_.toLocalDate))
  }.list.apply()
}
```

If you need more information(connection management, transaction, CRUD), please check the following wiki page or scaladoc in detail.

https://github.com/seratch/scalikejdbc/wiki/GettingStarted

http://seratch.github.com/scalikejdbc/api/index.html#scalikejdbc.package


## Anorm SQL template

Instead of embedding `?`(place holder), you can specify named place holder that is similar to [Anorm](http://www.playframework.org/documentation/2.0.1/ScalaAnorm). 

```scala
SQL("""
insert into user (
  id,
  email,
  name,
  encrypted_password
) values (
  {id},
  {email},
  {name},
  {encryptedPassword}
)
""").bindByName(
  'id -> 132430,
  'emal -> "bob@example.com",
  'name -> "Bob",
  'encryptedPassword -> "xfewSZe2sd3w"
).update.apply()
```


## Executable SQL template

Instead of embedding `?`(place holder), you can specify executable SQL as template. Using this API, it's possible to validate SQL before building into application. 

Usage is simple. Use Scala Symbol literal in comment with dummy value in SQL template, and pass named values by using not `bind(Any*)` but `bindByName((Symbol, Any)*)`. When some of the passed names by `#bindByName` are not used, or `#bind` is used although the template seems to be executable SQL template, runtime exception will be thrown.

```scala
SQL("""
insert into user (
  id,
  email,
  name,
  encrypted_password
) values (
  /*'id*/123,
  /*'email*/'xxx@example.com',
  /*'name*/'Alice',
  /*'encryptedPassword*/'123456789012'
)
""").bindByName(
  'id -> 132430,
  'emal -> "bob@example.com",
  'name -> "Bob",
  'encryptedPassword -> "xfewSZe2sd3w"
).update.apply()
```

## Logging SQL And Timing

https://github.com/seratch/scalikejdbc/wiki/LoggingSQLAndTime

## Mapper Generator 

https://github.com/seratch/scalikejdbc-mapper-generator


## Play framework 2.x support

https://github.com/seratch/scalikejdbc-play-plugin



