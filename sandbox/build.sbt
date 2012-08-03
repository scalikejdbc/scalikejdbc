libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "[1.3,)",
  "org.slf4j" % "slf4j-simple" % "1.6.4",
  "com.h2database" % "h2" % "[1.3,)"
)

initialCommands := """import scalikejdbc._
Class.forName("org.h2.Driver")
ConnectionPool.singleton("jdbc:h2:mem:scalikejdbc", "", "")
DB autoCommit { implicit s =>
  SQL("create table users(id bigint primary key, name text)").execute.apply()
  SQL("insert into users values ({id}, {name})").bindByName('id -> 1, 'name -> "Andy").update.apply()
  SQL("insert into users values ({id}, {name})").bindByName('id -> 2, 'name -> "Brian").update.apply()
}
case class User(val id: Long, val name: String)
val * = (rs: WrappedResultSet) => new User(rs.long("id"), rs.string("name"))
"""


