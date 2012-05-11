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

[![Build Status](https://secure.travis-ci.org/seratch/scalikejdbc.png?branch=master)](http://travis-ci.org/seratch/scalikejdbc)


## Setup

### sbt

```scala
resolvers += "sonatype" at "http://oss.sonatype.org/content/repositories/releases"

libraryDependencies += "com.github.seratch" %% "scalikejdbc" % "1.0.2"
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
import org.joda.time.DateTime
case class User(id: Long, name: String, birthday: Option[DateTime])

val activeUsers: List[User] = DB readOnly { session =>
  session.list("select * from user where active = ?", true) { 
    rs => User(rs.long("id"), rs.string("name"), Option(rs.date("birthday")).map(_.toDateTime))
  }
}
```

The other is using API which starts with `SQL.apply`: 

```scala
import scalikejdbc._
import org.joda.time.DateTime
case class User(id: Long, name: String, birthday: Option[DateTime])

val activeUsers: List[User] = DB readOnly { implicit session =>
  SQL("select * from user where active = ?").bind(true).map { 
    rs => User(rs.long("id"), rs.string("name"), Option(rs.date("birthday")).map(_.toDateTime))
  }.list.apply()
}
```

If you need more information(connection management, transaction, CRUD), please check the following wiki page in detail.

https://github.com/seratch/scalikejdbc/wiki/GettingStarted


## Mapper Generator 

https://github.com/seratch/scalikejdbc-mapper-generator


## Play framework 2.x support

https://github.com/seratch/scalikejdbc-play-plugin



