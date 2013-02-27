# ScalikeJDBC Sandbox

## Example

```scala
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

// users
case class User(id: Long, val name: Option[String], companyId: Option[Long] = None, company: Option[Company] = None)

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
case class Group(id: Long, name: Option[String], members: List[User] = Nil)

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

// -----------------------------
// Query Examples
// -----------------------------

val users: List[User] = DB readOnly { implicit s =>
  val (u, c) = (User.syntax, Company.syntax)
  sql"""
    select ${u.result.*}, ${c.result.*} 
    from ${User.as(u)} left join ${Company.as(c)} on ${u.companyId} = ${c.id}
  """
    .map(rs => User(rs, u.resultName, c.resultName)).list.apply()
}

println("-------------------")
users.foreach(user => println(user))
println("-------------------")

val groups: List[Group] = DB readOnly { implicit s =>
  val (u, g, gm, c) = (User.syntax("u"), Group.syntax("g"), GroupMember.syntax("gm"), Company.syntax("c"))
  sql"""
    select 
      ${u.result.*}, ${g.result.*}, ${c.result.*} 
    from 
      ${GroupMember.as(gm)} 
        inner join ${User.as(u)} on ${u.id} = ${gm.userId} 
        inner join ${Group.as(g)} on ${g.id} = ${gm.groupId} 
        left join ${Company.as(c)} on ${u.companyId} = ${c.id}
  """
    .one(rs => Group(rs, g.resultName))
    .toMany(rs => rs.intOpt(u.resultName.id).map(id => User(rs, u.resultName, c.resultName)))
    .map { (g, us) => g.copy(members = us) }
    .list
    .apply()
}

println("-------------------")
groups.foreach(group => println(group))
println("-------------------")
```

## How to run

```sh
git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

```sh
[info] Starting scala interpreter...
[info] 
[run-main] INFO scalikejdbc.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select users.id as i_on_users, users.name as n_on_users, users.company_id as ci_on_users, companies.id as i_on_companies, companies.name as n_on_companies from users left join companies on users.company_id = companies.id; (8 ms)

  [Stack Trace]
    ...
    $line1.$read$$iw$$iw$$anonfun$3.apply(<console>:71)
    $line1.$read$$iw$$iw$$anonfun$3.apply(<console>:68)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:500)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:499)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB.readOnly(DB.scala:499)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:133)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:132)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB$.readOnly(DB.scala:132)
    $line1.$read$$iw$$iw$.<init>(<console>:68)
    $line1.$read$$iw$$iw$.<clinit>(<console>)
    $line1.$eval$.<init>(<console>:7)
    ...

-------------------
User(1,Some(Alice),None,None)
User(2,Some(Bob),Some(1),Some(Company(1,Some(Typesafe))))
User(3,Some(Chris),Some(1),Some(Company(1,Some(Typesafe))))
-------------------
[run-main] INFO scalikejdbc.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select u.id as i_on_u, u.name as n_on_u, u.company_id as ci_on_u, g.id as i_on_g, g.name as n_on_g, c.id as i_on_c, c.name as n_on_c from group_members gm inner join users u on u.id = gm.user_id inner join groups g on g.id = gm.group_id left join companies c on u.company_id = c.id; (0 ms)

  [Stack Trace]
    ...
    $line1.$read$$iw$$iw$$anonfun$6.apply(<console>:78)
    $line1.$read$$iw$$iw$$anonfun$6.apply(<console>:76)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:500)
    scalikejdbc.DB$$anonfun$readOnly$2.apply(DB.scala:499)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB.readOnly(DB.scala:499)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:133)
    scalikejdbc.DB$$anonfun$readOnly$1.apply(DB.scala:132)
    scalikejdbc.LoanPattern$.using(LoanPattern.scala:29)
    scalikejdbc.package$.using(package.scala:76)
    scalikejdbc.DB$.readOnly(DB.scala:132)
    $line1.$read$$iw$$iw$.<init>(<console>:76)
    $line1.$read$$iw$$iw$.<clinit>(<console>)
    $line1.$eval$.<init>(<console>:7)
    ...

-------------------
Group(1,Some(Japan Scala Users Group),List(User(2,Some(Bob),Some(1),Some(Company(1,Some(Typesafe)))), User(1,Some(Alice),None,None)))
-------------------
```

