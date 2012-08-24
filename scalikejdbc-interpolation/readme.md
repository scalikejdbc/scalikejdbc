# ScalikeJDBC Interpolation

## How to use

### build.sbt

```
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

```
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:mem:hsqldb:interpolation", "", "")

case class User(id: Int, name: String)

DB localTx { implicit s =>
  sql"create table users (id int, name varchar(256));".execute.apply()
  sql"insert into users values (${1}, ${"foo"})".update.apply()
  sql"insert into users values (${2}, ${"bar"})".update.apply()
  sql"insert into users values (${3}, ${"baz"})".update.apply()
  val id = 3
  val user = sql"select * from users where id = ${id}".map {
    rs => User(id = rs.int("id"), name = rs.string("name"))
  }.single.apply()
}
```
