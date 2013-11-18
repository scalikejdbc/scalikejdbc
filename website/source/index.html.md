# ScalikeJDBC

<hr/>
## Just write SQL and get things done!

ScalikeJDBC is a tidy SQL-based DB access library for Scala developers.
This library naturally wraps JDBC APIs and provides you easy-to-use APIs.
ScalikeJDBC is a practical and production-ready one. Use this library for your real projects.

<hr/>
## Getting Started

All you need to do is just adding ScalikeJDBC, JDBC driver & slf4j implementation. 

```
libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc"               % "[1.6,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)",
  "com.h2database"     %  "h2"                        % "[1.3,)",
  "ch.qos.logback"     %  "logback-classic"           % "[1.0,)"
)
```

#### First example

Put above dependencies into your `build.sbt` and run `sbt console` now.

```java
import scalikejdbc._, SQLInterpolation._

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

#### Typesafe Activator

![Typesafe](images/typesafe.png)

You can try a [Play framework](http://www.playframework.com/) sample app which uses ScalikeJDBC on [Typesafe Activator](http://typesafe.com/activator).

Activator page: [Hello ScalikeJDBC!](http://typesafe.com/activator/template/scalikejdbc-activator-template)

See on GitHub: [seratch/hello-scalikejdbc](https://github.com/seratch/hello-scalikejdbc)

<hr/>
## ScalikeJDBC in 5 minutes

Here is a presentation for beginners which shows overview of ScalikeJDBC in 5 minutes.

<hr/>
<div style="width: 50%">
<script async class="speakerdeck-embed" data-id="5a0fa5b0b4df0130946402d040a74214" src="//speakerdeck.com/assets/embed.js"></script>
</div>



<hr/>
## Overview

<hr/>
### JDBC is the standard SQL interface on the JVM

Whether you like it or not, JDBC is a stable standard interface. Since most of RDBMS supports JDBC interface, we can access RDBMS in the same way.

We never release without passing all the unit tests with the following RDBMS.

- "com.h2database" % "h2"
- "org.hsqldb" % "hsqldb"
- "mysql" % "mysql-connector-java"
- "org.postgresql" % "postgresql"

We believe that ScalikeJDBC basically works with any other RDBMS (Oracle, SQL Server and so on).

#### Non-blocking?

Unfortunately, no. Indeed, JDBC drivers block on socket IO. So using them to talk with RDBMS in async event driven archetecture may not be appropriate. But actually most of real world applications don't need event-driven archetecture yet. JDBC is still important infrastructure for apps on the JVM.

#### Amazon Reshift, Facebook Presto and then...

If you can access some datastore via JDBC interface, that means you can access them via ScalikeJDBC too. Recently, [Amazon Redshift](http://docs.aws.amazon.com/redshift/latest/dg/c_redshift-postgres-jdbc.html) and [Facebook Presto](https://github.com/facebook/presto/tree/master/presto-jdbc) support JDBC interface. You can access them via ScalikeJDBC!


<hr/>
### Less dependencies

Core part of ScalikeJDBC has so less dependencies that you won't be bothered by dependency hell.

- JDBC Drivers that you need
- Commons DBCP
- Joda Time 2.x
- SLF4J API

Of course, you can use c3p0 instead of commons-dbcp though ConnectionPool interface is available by default.


<hr/>
### Using only Scala standard API & SQL

 Library users don't need to learn so many library-specific rules or conventions. If you're already familiar with Scala's standard library APIs and basic SQLs, that much should be enough.

```java
val name = "Alice"
// implicit session represents java.sql.Connection
val memberId: Option[Long] = DB readOnly { implicit session =>
  sql"select id from members where name = ${name}" // don't worry, prevents SQL injection
    .map(rs => rs.long("id")) // extracts values from rich java.sql.ResultSet
    .single                   // single, list, traversable
    .apply()                  // Side effect!!! runs the SQL using Connection
}
```

See in detail: [/documentation/operations](documentation/operations.html)

<hr/>
### Type-safe DSL

Since version 1.6, QueryDSL is available. It's a SQL-like and type-safe DSL to build DRY SQLs.

Here is an example:

```java
val (p, c) = (Programmer.syntax("p"), Company.syntax("c"))

val programmers: List[Long] = DB readOnly { implicit session =>
  withSQL {
    select
      .from(Programmer as p)
      .leftJoin(Company as c).on(p.companyId, c.id)
      .where.ne(p.isDeleted, false)
      .orderBy(p.createdAt)
      .limit(10)
      .offset(0)
  }.map(Programmer(p, c)).list.apply()
}
```

See in detail: [/documentation/query-dsl](documentation/query-dsl.html)

Test code: [src/test/scala/scalikejdbc/QueryInterfaceSpec.scala](https://github.com/seratch/scalikejdbc/blob/master/scalikejdbc-interpolation/src/test/scala/scalikejdbc/QueryInterfaceSpec.scala)

<hr/>
### Flexible transaction control

ScalikeJDBC provides several APIs for session/transaction control.

 - DB autoCommit { implicit session => ... }
 - DB localTx { implicit session => ... }
 - DB withinTx { implicit session => ... }
 - DB readOnly { implicit session => ... }

Here is an example code which re-use methods in both of simple invocation and transactional operations.

```java
object Product {
  def create(name: String, price: Long)(implicit s: DBSession = AutoSession): Long = {
    sql"insert into products values (${name}, ${price})"
      .updateAndReturnGeneratedKey.apply() // returns auto-incremeneted id
  }

  def findById(id: Long)(implicit s: DBSession = AutoSession): Option[Product] = {
    sql"select id, name, price, created_at from products where id = ${id}"
      .map { rs => Product(rs) }.single.apply()
  }
}

Product.findById(123) // borrows connection from pool and gives it back after execution

DB localTx { implicit session => // transactional session
  val id = Product.create("ScalikeJDBC Cookbook", 200) // within transaction
  val product = Product.findById(id) // within transaction
}
```

See in detail: [/documentation/transaction](documentation/transaction.html)

<hr/>
### Useful Query Inspections

By default, ScalikeJDBC shows you what SQL is executed and where it is. We believe that is quite useful for debugging your apps. Logging only slow queries in production also helps you.

```
[debug] s.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select id, name from users where email = 'alice@example.com'; (3 ms)

  [Stack Trace]
    ...
    models.User$.findByEmail(User.scala:26)
    controllers.Projects$$anonfun$index$1$$anonfun$apply$1$$anonfun$apply$2.apply(Projects.scala:20)
    controllers.Projects$$anonfun$index$1$$anonfun$apply$1$$anonfun$apply$2.apply(Projects.scala:19)
    controllers.Secured$$anonfun$IsAuthenticated$3$$anonfun$apply$3.apply(Application.scala:88)
```

See in detail: [/documentation/query-inspector](documentation/query-inspector.html)

<hr/>
### Testing Support

Testing support which provides the following functionalities for [ScalaTest](http://www.scalatest.org/) and [specs2](http://etorreborre.github.io/specs2/).

 - Rollback automatically after each test
 - Testing with fixtures

See in detail: [/documentation/testing](documentation/testing.html)

<hr/>
### Reverse Engineering

You can easily get Scala code from existing database by using ScalikeJDBC's reverse engineering tool.

```
sbt "scalikejdbc-gen [table-name (class-name)]"
```

e.g.

```
sbt "scalikejdbc-gen company"
sbt "scalikejdbc-gen companies Company"
```

See in detail: [/documentation/reverse-engineering](documentation/reverse-engineering.html)

<hr/>
### Play Framework Support

![Play framework](images/play.png)

You can use ScalikeJDBC with Play framework 2 seamlessly. We promise you that it becomes more productive when using together with scalikejdbc-mapper-generator.

See in detail: [/documentation/playframework-support](documentation/playframework-support.html)

<hr/>
## License

Published binary files have the following copyright:

```
Copyright 2013 ScalikeJDBC committers
Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0.html
```

<hr/>
## Related Products

<hr/>
### dbconsole

dbconsole is an extended sbt console to connect database. 

#### Mac OS X, Linux

```sh
curl -L http://git.io/dbconsole | sh
```

#### Windows

```
http://git.io/dbconsole.bat
```

See in detail: [/documentation/dbconsole](documentation/dbconsole.html)


<hr/>
### Skinny ORM

![Skinny framework](images/skinny.png)

Skinny ORM is the default DB access library of [Skinny Framework](https://github.com/seratch/skinny-framework). Skinny ORM is built upon ScalikeJDBC. 

In most cases, ORM makes things easier.

[https://github.com/seratch/skinny-framework#orm](https://github.com/seratch/skinny-framework#orm)



