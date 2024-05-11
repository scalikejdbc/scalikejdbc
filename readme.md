# ScalikeJDBC

[![Maven Central](https://img.shields.io/maven-central/v/org.scalikejdbc/scalikejdbc_2.13.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:org.scalikejdbc%20AND%20a:scalikejdbc_2.13)

## Just Write SQL And Get Things Done 💪

[**ScalikeJDBC**](https://scalikejdbc.org/) seamlessly wraps JDBC APIs, offering intuitive and highly flexible functionalities. With QueryDSL, your code becomes inherently type-safe and reusable. This library is not just practical; it’s production-ready. Utilize this library confidently in your real-world projects.

## Getting Started

### Simple Database Library

If you're looking to execute SQL queries efficiently, the best approach is to use ScalikeJDBC along with the appropriate JDBC driver for your database. Here’s how you can get started quickly!

#### Dependencies

To get started with ScalikeJDBC, add the following dependency to your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"     % "4.3.+",
  "com.h2database"  %  "h2"              % "2.2.+",
  "ch.qos.logback"  %  "logback-classic" % "1.5.+"
)
```

If you're a Play Framework user, take a look at play-support project, too: https://github.com/scalikejdbc/scalikejdbc-play-support

#### Quick Example

Here’s a quick example to get you up and running:

```scala
import scalikejdbc._

// initialize JDBC driver & connection pool
Class.forName("org.h2.Driver")
ConnectionPool.singleton("jdbc:h2:mem:hello", "user", "pass")

// ad-hoc session provider on the REPL
implicit val session: DBSession = AutoSession

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
import java.time._
case class Member(id: Long, name: Option[String], createdAt: ZonedDateTime)
object Member extends SQLSyntaxSupport[Member] {
  override val tableName = "members"
  def apply(rs: WrappedResultSet) = new Member(
    rs.long("id"), rs.stringOpt("name"), rs.zonedDateTime("created_at"))
}

// find all members
val members: List[Member] = sql"select * from members".map(rs => Member(rs)).list.apply()

// use paste mode (:paste) on the Scala REPL
val m = Member.syntax("m")
val name = "Alice"
val alice: Option[Member] = withSQL {
  select.from(Member as m).where.eq(m.name, name)
}.map(rs => Member(rs)).single.apply()
```

### Rich O/R Mapper

For those who require more robust functionalities, consider using **scalikejdbc-orm**. This extension is an O/R mapper built on top of the ScalikeJDBC core library, drawing significant inspiration from Ruby on Rails' ActiveRecord library.


#### Efficient Data Fetching with Join Queries / Eager Loading

One of the standout features of scalikejdbc-orm is its ability to efficiently handle data associations, effectively eliminating the common N+1 query problem. This is achieved through the smart use of join queries in resolving associations like `#belongsTo`, `#hasOne`, and `#hasMany/#hasManyThrough`. These are processed behind the scenes, allowing you to focus on your application without worrying about performance degradation due to N+1 issues.

While join queries are suitable for many scenarios, some complex data relationships might require a different approach. For such use cases, you can do eager loading (i.e. resolve the main entity and then perform in-clause query to resolve deep nested associations) with the `#includes` method.

#### Dependencies

Like the instruction for the simple DB library, add the library along with a JDBC driver and logging tool:

```scala
libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc-orm" % "4.3.+",
  "com.h2database"  %  "h2"              % "2.2.+",
  "ch.qos.logback"  %  "logback-classic" % "1.5.+"
)
```

#### Quick Example

Save the following code as `example.scala`:

```scala
import java.time.ZonedDateTime

import scalikejdbc.*
import scalikejdbc.orm.*
import scalikejdbc.orm.timstamps.TimestampsFeature

case class Email(
  id: Long,
  memberId: Long,
  address: String,
)
object Email extends CRUDMapper[Email] {
  override lazy val tableName = "member_email"
  lazy val defaultAlias = createAlias("me")
  def extract(rs: WrappedResultSet, e: ResultName[Email]): Email = autoConstruct(rs, e)
}

case class Member(
  id: Long,
  name: Option[String],
  createdAt: ZonedDateTime,
  updatedAt: Option[ZonedDateTime],
  email: Option[Email] = None,
)
object Member extends CRUDMapper[Member] with TimestampsFeature[Member] {
  lazy val defaultAlias = createAlias("m")
  def extract(rs: WrappedResultSet, n: ResultName[Member]): Member = autoConstruct(rs, n, "email")

  val email = hasOne[Email](Email, (m, e) => m.copy(email = e))
}

class Example extends App {
  // ### Database connection ###
  Class.forName("org.h2.Driver")
  ConnectionPool.singleton("jdbc:h2:mem:hello;MODE=PostgreSQL", "user", "pass")
  implicit val session = AutoSession

  // ### Create tables ###
  sql"""create table member (
    id serial not null primary key,
    name varchar(64),
    created_at timestamp not null,
    updated_at timestamp
  )""".execute.apply()
  sql"""create table member_email (
    id serial not null primary key,
    member_id int not null,
    address varchar(256) not null
  )""".execute.apply()

  val m = Member.column

  // ### Insert rows ###
  val ids = Seq("Alice", "Bob", "Chris") map { name =>
    // insert into member (name, created_at, updated_at) values ('Alice', '2024-05-11 14:52:27.13', '2024-05-11 14:52:27.13');
    Member.createWithNamedValues(m.name -> name)
  }

  // ### Find all rows ###
  // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m order by m.id;
  val allMembers1: Seq[Member] = Member.findAll()
  // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where m.id in (1, 2, 3);
  val allMembers2: Seq[Member] = Member.findAllByIds(ids*)

  // ### Run queries with where conditions ###
  // Quick way but less type-safety
  // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where m.name = 'Alice' order by m.id;
  val member1: Seq[Member] = Member.where("name" -> "Alice").apply()
  // Types-safe query builder
  // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m from member m where name = 'Alice' order by m.id;
  val member2: Seq[Member] = Member.where(sqls.eq(m.name, "Alice")).apply()

  val memberId = member2.head.id

  // ### Run join queries ###
  val e = Email.column
  // insert into member_email (member_id, address) values (1, 'a@example.com');
  Email.createWithNamedValues(e.memberId -> memberId, e.address -> "a@example.com")

  // Note that member3.email exists while it does not in member1,2
  // select m.id as i_on_m, m.name as n_on_m, m.created_at as ca_on_m, m.updated_at as ua_on_m , me.id as i_on_me, me.member_id as mi_on_me, me.address as a_on_me from member m left join member_email me on m.id = me.member_id where name = 'Alice' order by m.id;
  val member3 = Member.joins(Member.email).where(sqls.eq(m.name, "Alice")).apply()

  // ### Update/delete rows ###
  // update member set updated_at = '2024-05-11 14:52:27.188', name = 'Ace' where id = 1;
  Member.updateById(memberId).withAttributes("name" -> "Ace")
  // delete from member where id = 1;
  Member.deleteById(memberId)
}
```

Run the code by the `sbt run` command.

How did it go? If you'd like to know more details or see more practical examples, see the full documentation at:

https://scalikejdbc.org/

## License

Published source code and binary files have the following copyright:

```
Copyright scalikejdbc.org
Apache License, Version 2.0
https://www.apache.org/licenses/LICENSE-2.0.html
```

