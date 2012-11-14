seq(scalikejdbcSettings: _*)

resolvers ++= Seq(
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.4,)",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "org.hsqldb" % "hsqldb" % "[2,)",
  "org.specs2" %% "specs2" % "1.12.2" % "test"
)

initialCommands := """import scalikejdbc._
import scalikejdbc.StringSQLRunner._
Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:file:db/test", "", "")
DB autoCommit { implicit s =>
  try {
    SQL("create table users(id bigint primary key not null, name varchar(255))").execute.apply()
    SQL("insert into users values ({id}, {name})").bindByName('id -> 1, 'name -> "Andy").update.apply()
    SQL("insert into users values ({id}, {name})").bindByName('id -> 2, 'name -> "Brian").update.apply()
  } catch { case e => println(e.getMessage) }
}
case class User(val id: Long, val name: String)
val * = (rs: WrappedResultSet) => new User(rs.long("id"), rs.string("name"))
implicit val session = DB.autoCommitSession
"""

