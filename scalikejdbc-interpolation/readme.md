# ScalikeJDBC Interpolation

## How to use

### build.sbt

```scala
scalaVersion := "2.10.0-M7"

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "1.3.6-SNAPSHOT",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "1.3.6-SNAPSHOT",
  "org.hsqldb" % "hsqldb" % "[2,)"
)
```

### Try it now

Invoke `sbt console` and try the following example.

Followng is the old style ScalikeJDBC code. It's still fine.

```scala
import scalikejdbc._

Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

case class User(id: Int, name: String)

DB localTx { implicit session =>

  SQL("create table users (id int, name varchar(256));").execute.apply()

  Seq((1, "foo"),(2, "bar"),(3, "baz")) foreach { case (id, name) =>
    SQL("insert into users values ({id}, {name})")
      .bindByName('id -> id, 'name -> name)
      .update.apply()
  }

  val id = 3
  val user = SQL("select * from users where id = {id}")
    .bindByName('id -> id)
    .map { rs => User(id = rs.int("id"), name = rs.string("name")) }
    .single.apply()
  println(user)

}
```

However, now we can write the same more simply with SQLInterpolation.

```scala
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

case class User(id: Int, name: String)

DB localTx { implicit session =>

  sql"create table users (id int, name varchar(256));".execute.apply()

  Seq((1, "foo"),(2, "bar"),(3, "baz")) foreach { case (id, name) =>
    sql"insert into users values (${id}, ${name})".update.apply()
  }

  val id = 3
  val user = sql"select * from users where id = ${id}"
    .map { rs => User(id = rs.int("id"), name = rs.string("name")) }
    .single.apply()
  println(user)

}
```

Of course, this code is safely protected from SQL injection attacks. 

Unfortunately, there is a known limitation that the number of parameters should be less than 23 due to Scala tuple limit...


