scalikejdbcSettings

scalaVersion := "2.10.0"

resolvers ++= Seq(
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "1.4.7-SNAPSHOT",
  "com.github.seratch" %% "scalikejdbc-interpolation" % "1.4.7-SNAPSHOT",
  "org.slf4j" % "slf4j-simple" % "[1.7,)",
  "org.hsqldb" % "hsqldb" % "[2,)",
  "org.specs2" %% "specs2" % "1.14" % "test"
)

initialCommands := """import scalikejdbc._
import scalikejdbc.SQLInterpolation._
// -----------------------------
Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:mem:test", "", "")
DB autoCommit { implicit s =>
  try {
    // create tables
    sql"create table users(id bigint primary key not null, name varchar(255), company_id bigint)".execute.apply()
    sql"create table companies(id bigint primary key not null, name varchar(255))".execute.apply()
    sql"create table groups(id bigint primary key not null, name varchar(255))".execute.apply()
    sql"create table group_members(group_id bigint not null, user_id bigint not null, primary key(group_id, user_id))".execute.apply()
    // insert data
    sql"insert into users values (${1}, ${"Alice"}, null)".update.apply()
    sql"insert into users values (${2}, ${"Bob"}, ${1})".update.apply()
    sql"insert into users values (${3}, ${"Chris"}, ${1})".update.apply()
    sql"insert into companies values (${1}, ${"Typesafe"})".update.apply()
    sql"insert into groups values (${1}, ${"Japan Scala Users Group"})".update.apply()
    sql"insert into group_members values (${1}, ${1})".update.apply()
    sql"insert into group_members values (${1}, ${2})".update.apply()
  } catch { case e: Exception => println(e.getMessage) }
}
// -----------------------------
// users
case class User(id: Long, val name: Option[String], 
  companyId: Option[Long] = None, company: Option[Company] = None)
object User extends SQLSyntaxSupport[User] { 
  override val tableName = "users"
  override val columns = Seq("id", "name", "company_id")
  def apply(rs: WrappedResultSet, u: ResultName[User]): User = User(rs.long(u.id), rs.stringOpt(u.name), rs.longOpt(u.companyId))
  def apply(rs: WrappedResultSet, u: ResultName[User], c: ResultName[Company]): User = {
    apply(rs, u).copy(company = rs.longOpt(c.id).map(id => Company(rs.long(c.id), rs.stringOpt(c.name))))
  }
} 
// companies
case class Company(id: Long, name: Option[String])
object Company extends SQLSyntaxSupport[Company] {
  override val tableName = "companies"
  override val columns = Seq("id", "name")
  def apply(rs: WrappedResultSet, c: ResultName[Company]): Company = Company(rs.long(c.id), rs.stringOpt(c.name))
} 
// groups
case class Group(id: Long, name: Option[String], members: Seq[User] = Nil)
object Group extends SQLSyntaxSupport[Group] { 
  override val tableName = "groups"
  override val columns = Seq("id", "name")
  def apply(rs: WrappedResultSet, g: ResultName[Group]): Group = Group(rs.long(g.id), rs.stringOpt(g.name))
}
// group_members
case class GroupMember(groupId: Long, userId: Long)
object GroupMember extends SQLSyntaxSupport[GroupMember] {
  override val tableName = "group_members"
  override val columns = Seq("group_id", "user_id")
}
GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
  enabled = true,
  logLevel = 'info
)
// -----------------------------
// Query Examples
// -----------------------------
val users: Seq[User] = DB readOnly { implicit s =>
  val (u, c) = (User.syntax, Company.syntax)
  sql"select ${u.result.*}, ${c.result.*} from ${User.as(u)} left join ${Company.as(c)} on ${u.companyId} = ${c.id}"
   .map(rs => User(rs, u.resultName, c.resultName)).list.apply()
}
println("-------------------")
users.foreach(user => println(user))
println("-------------------")
val groups: Seq[Group] = DB readOnly { implicit s =>
  val (u, g, gm, c) = (User.syntax("u"), Group.syntax("g"), GroupMember.syntax("gm"), Company.syntax("c"))
  sql"select ${u.result.*}, ${g.result.*}, ${c.result.*} from ${GroupMember.as(gm)} inner join ${User.as(u)} on ${u.id} = ${gm.userId} inner join ${Group.as(g)} on ${g.id} = ${gm.groupId} left join ${Company.as(c)} on ${u.companyId} = ${c.id}"
  .one(rs => Group(rs, g.resultName))
  .toMany(rs => rs.intOpt(u.resultName.id)
  .map(id => User(rs, u.resultName, c.resultName))).map { (g, us) => g.copy(members = us) }
  .lis
  t.apply()
}
println("-------------------")
groups.foreach(group => println(group))
println("-------------------")
"""

