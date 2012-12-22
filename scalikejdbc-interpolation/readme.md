# ScalikeJDBC Interpolation

This is a SQL template using [SIP-11](http://docs.scala-lang.org/sips/pending/string-interpolation.html).

## How to use

### build.sbt

```scala
scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.4,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.4,)",
  "org.slf4j" % "slf4j-simple" % "[1.7,)"
  "org.hsqldb" % "hsqldb" % "[2,)"
)

initialCommands := """
  import scalikejdbc._
  import scalikejdbc.SQLInterpolation._
  Class.forName("org.hsqldb.jdbc.JDBCDriver")
  ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")
  case class Member(id: Int, name: String)
  implicit val session = DB.autoCommitSession
"""
```

### Try it now

Invoke `sbt console` and try the following example.

Followng is the old style ScalikeJDBC code. It's still fine.

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

SQL("create table members (id int, name varchar(256));").execute.apply()

Seq((1, "foo"),(2, "bar"),(3, "baz")) foreach { case (id, name) =>
  SQL("insert into members values ({id}, {name})")
    .bindByName('id -> id, 'name -> name)
    .update.apply()
}

val id = 3
val user = SQL("select * from members where id = {id}")
  .bindByName('id -> id)
  .map { rs => Member(id = rs.int("id"), name = rs.string("name")) }
  .single.apply()

// Exiting paste mode, now interpreting.

id: Int = 3
user: Option[Member] = Some(Member(3,baz))

scala> :q

```

However, now we can write the same more simply with SQLInterpolation.

```scala
scala> :paste
// Entering paste mode (ctrl-D to finish)

sql"create table members (id int, name varchar(256));".execute.apply()

Seq((1, "foo"),(2, "bar"),(3, "baz")) foreach { case (id, name) =>
  sql"insert into members values (${id}, ${name})".update.apply()
}

val id = 3
val user = sql"select * from members where id = ${id}"
  .map { rs => Member(id = rs.int("id"), name = rs.string("name")) }
  .single.apply()

// Exiting paste mode, now interpreting.

id: Int = 3
user: Option[Member] = Some(Member(3,baz))

scala> :q
```

Of course, this code is safely protected from SQL injection attacks. 

Unfortunately, there is a known limitation that the number of parameters should be less than 23 due to Scala tuple limit...


