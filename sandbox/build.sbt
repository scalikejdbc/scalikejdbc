scalikejdbcSettings

scalaVersion := "2.11.7"

lazy val scalikejdbcVersion = "2.2.8"

resolvers ++= Seq(
  "Sonatype releases"  at "https://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)

libraryDependencies ++= Seq(
  "org.scalikejdbc"    %% "scalikejdbc"        % scalikejdbcVersion,
  "org.scalikejdbc"    %% "scalikejdbc-jsr310" % scalikejdbcVersion,
  "com.github.seratch" %  "java-time-backport" % "1.0.0",
  "org.scalikejdbc"    %% "scalikejdbc-test"   % scalikejdbcVersion,
  "org.slf4j"          %  "slf4j-simple"       % "1.7.+",
  "org.hibernate"      %  "hibernate-core"     % "4.3.11.Final",
  "org.hsqldb"         %  "hsqldb"             % "2.3.+",
  "org.specs2"         %% "specs2-core"        % "2.4.4"             % "test"
)

initialCommands := """import scalikejdbc._, jsr310._
import java.time._
// classes
case class User(id: Long, name: Option[String], companyId: Option[Long] = None, company: Option[Company] = None)
object User extends SQLSyntaxSupport[User] {
  override val tableName = "users"
  override val columns = Seq("id", "name", "company_id")
  def opt(u: SyntaxProvider[User], c: SyntaxProvider[Company])(rs: WrappedResultSet): Option[User] = opt(u.resultName, c.resultName)(rs)
  def opt(u: ResultName[User], c: ResultName[Company])(rs: WrappedResultSet): Option[User] = rs.longOpt(u.id).map(_ => apply(u, c)(rs))
  def apply(u: SyntaxProvider[User])(rs: WrappedResultSet): User = apply(u.resultName)(rs)
  def apply(u: ResultName[User])(rs: WrappedResultSet): User = User(rs.long(u.id), rs.stringOpt(u.name), rs.longOpt(u.companyId))
  def apply(u: SyntaxProvider[User], c: SyntaxProvider[Company])(rs: WrappedResultSet): User = apply(u.resultName, c.resultName)(rs)
  def apply(u: ResultName[User], c: ResultName[Company])(rs: WrappedResultSet): User = {
    (apply(u)(rs)).copy(company = rs.longOpt(c.id).map(_ => Company(c)(rs)))
  }
}
case class Company(id: Long, name: Option[String])
object Company extends SQLSyntaxSupport[Company] {
  override val tableName = "companies"
  override val columns = Seq("id", "name")
  def apply(c: SyntaxProvider[Company])(rs: WrappedResultSet): Company = apply(c.resultName)(rs)
  def apply(c: ResultName[Company])(rs: WrappedResultSet): Company = Company(rs.long(c.id), rs.stringOpt(c.name))
}
case class Group(id: Long, name: Option[String], createdAt: ZonedDateTime, members: Seq[User] = Nil)
object Group extends SQLSyntaxSupport[Group] {
  override val tableName = "groups"
  override val columns = Seq("id", "name", "created_at")
  def apply(g: SyntaxProvider[Group])(rs: WrappedResultSet): Group = apply(g.resultName)(rs)
  def apply(g: ResultName[Group])(rs: WrappedResultSet): Group = Group(rs.get(g.id), rs.get(g.name), rs.get(g.createdAt))
}
case class GroupMember(groupId: Long, userId: Long)
object GroupMember extends SQLSyntaxSupport[GroupMember] {
  override val tableName = "group_members"
  override val columns = Seq("group_id", "user_id")
}
// loading data
Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.singleton("jdbc:hsqldb:file:db/test", "", "")
DB localTx { implicit session =>
  try {
    sql"create table users(id bigint primary key not null, name varchar(255), company_id bigint)".execute.apply()
    sql"create table companies(id bigint primary key not null, name varchar(255))".execute.apply()
    sql"create table groups(id bigint primary key not null, name varchar(255), created_at timestamp not null)".execute.apply()
    sql"create table group_members(group_id bigint not null, user_id bigint not null, primary key(group_id, user_id))".execute.apply()
    Seq(
      insert.into(User).values(1, "Alice", null),
      insert.into(User).values(2, "Bob",   1),
      insert.into(User).values(3, "Chris", 1),
      insert.into(Company).values(1, "Typesafe"),
      insert.into(Company).values(2, "Oracle"),
      insert.into(Group).values(1, "Japan Scala Users Group", new java.util.Date()),
      insert.into(GroupMember).values(1, 1),
      insert.into(GroupMember).values(1, 2)
    ).foreach(sql => applyUpdate(sql))
  } catch { case e: Exception => e.printStackTrace }
}
GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = true, logLevel = 'info)
implicit val session = AutoSession
val (u, g, gm, c) = (User.syntax("u"), Group.syntax("g"), GroupMember.syntax("gm"), Company.syntax("c"))
"""

