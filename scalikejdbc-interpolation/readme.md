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


### Experimental Feature

You can use more powerful `SQLSyntaxSupport` trait. This feature is still experimental. So APIs or specifications are subject to change.

#### Prepared tables

```scala
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

DB autoCommit { implicit s =>
  sql"create table users (id int not null, first_name varchar(256), group_id int)".execute.apply()
  sql"create table groups (id int not null, website_url varchar(256))".execute.apply()
}
```

#### Classes to bind and setup thier companion objects

```scala
case class User(id: Int, name: Option[String], groupId: Option[Int] = None, group: Option[Group] = None)

object User extends SQLSyntaxSupport[User] {

  override def tableName = "users"
  override def columns = Seq("id", "first_name", "group_id")
  override def nameConverters = Map("givenName" -> "first_name")

  def apply(rs: WrappedResultSet, u: ResultName[User]): User = {
    // ResultName provides dynamic camelCase methods
    User(id = rs.int(u.id), name = rs.stringOpt(u.givenName), groupId = rs.intOpt(u.groupId))
  }

  def apply(rs: WrappedResultSet, u: ResultName[User], g: ResultName[Group]): User = {
    // User might have a Group
    apply(rs, u).copy(group = rs.intOpt(g.id).map(id => Group(id = id, websiteUrl = rs.stringOpt(g.websiteUrl))))
  }
}

case class Group(id: Int, websiteUrl: Option[String])

object Group extends SQLSyntaxSupport[Group] {
  override def tableName = "groups"
  override def columns = Seq("id", "website_url")
}
```

### Query Examples

```scala
val id = 3
val u = User.syntax("u")
val g = Group.syntax

val user: Option[User] = sql"""
  select ${u.result.*}, ${g.result.*}
  from ${User.as(u)} left join ${Group.as(g)} on ${u.groupId} = ${g.id}
  where ${u.id} = ${id}
"""
  .map(rs => User(rs, u.result.names, g.result.names))
  .single.apply()
```

This code generates the following SQL. It's quite easy-to-understand and open for extension:

```sql
select 
  u.id as id__on__u, u.first_name as first_name__on__u, u.group_id as group_id__on__u, 
  groups.id as id__on__groups, groups.website_url as website_url__on__groups
from users u left join groups on u.group_id = groups.id 
where u.id = 3;
```

