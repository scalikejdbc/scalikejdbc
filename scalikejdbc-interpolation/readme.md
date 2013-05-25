# ScalikeJDBC Interpolation

This is a SQL template using [SIP-11](http://docs.scala-lang.org/sips/pending/string-interpolation.html).

## How to use

### build.sbt

```scala
scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.5,)",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.5,)",
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


### More information

Please checkt the wiki page.

https://github.com/seratch/scalikejdbc/wiki/SQLInterpolation

