# ScalikeJDBC

## Just write SQL and get things done!

ScalikeJDBC is a tidy SQL-based DB access library for Scala developers. This library naturally wraps JDBC APIs and provides you easy-to-use APIs.

ScalikeJDBC is a practical and production-ready one. Use this library for your real projects.

http://scalikejdbc.org/

[![Build Status](https://travis-ci.org/scalikejdbc/scalikejdbc.svg?branch=develop)](https://travis-ci.org/scalikejdbc/scalikejdbc)

## Getting Started

All you need to do is just adding ScalikeJDBC, JDBC driver & slf4j implementation.

```
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"        % "2.0.0",
  "com.h2database"  %  "h2"                 % "1.4.178",
  "ch.qos.logback"  %  "logback-classic"    % "1.1.2"
)
```

If you're a Play2 user, take a look at play-support project, too.

https://github.com/scalikejdbc/scalikejdbc-play-support

#### First example

Put above dependencies into your `build.sbt` and run `sbt console` now.

```java
//import scalikejdbc._, SQLInterpolation._
import scalikejdbc._

// initialize JDBC driver & connection pool
Class.forName("org.h2.Driver")
ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

// ad-hoc session provider on the REPL
implicit val session = AutoSession

// table creation, you can run DDL by using #execute as same as JDBC
sql"""
create table members (
  id serial not null primary key,
  name varchar(64),
  created_at timestamp not null
)
""".execute.apply()

// insert initial data
Seq("Alice", "Bob", "Chris") foreach { name =>
  sql"insert into members (name, created_at) values (${name}, current_timestamp)".update.apply()
}

// for now, retrieves all data as Map value
val entities: List[Map[String, Any]] = sql"select * from members".map(_.toMap).list.apply()

// defines entity object and extractor
import org.joda.time._
case class Member(id: Long, name: Option[String], createdAt: DateTime)
object Member extends SQLSyntaxSupport[Member] {
  override val tableName = "members"
  def apply(rs: WrappedResultSet) = new Member(
    rs.long("id"), rs.stringOpt("name"), rs.dateTime("created_at"))
}

// find all members
val members: List[Member] = sql"select * from members".map(rs => Member(rs)).list.apply()
```

How did it go? If you'd like to know more details or practical examples, see documentation.

http://scalikejdbc.org/


## License

Published binary files have the following copyright:

```
Copyright 2013 ScalikeJDBC committers
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0.html
```

