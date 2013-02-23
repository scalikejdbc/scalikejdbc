# ScalikeJDBC Sandbox

## Example

```scala
import scalikejdbc._
import scalikejdbc.SQLInterpolation._

// users
case class User(id: Long, val name: Option[String], companyId: Option[Long] = None, company: Option[Company] = None)

object User extends SQLSyntaxSupport[User] { 
  override def tableName = "users"
  override def columns = Seq("id", "name", "company_id")
  def apply(rs: WrappedResultSet, u: ResultName[User]): User = User(rs.long(u.id), rs.stringOpt(u.name), rs.longOpt(u.companyId))
  def apply(rs: WrappedResultSet, u: ResultName[User], c: ResultName[Company]): User = {
    apply(rs, u).copy(company = rs.longOpt(c.id).map(id => Company(rs.long(c.id), rs.stringOpt(c.name))))
  }
} 

// companies
case class Company(id: Long, name: Option[String])

object Company extends SQLSyntaxSupport[Company] {
  override def tableName = "companies"
  override def columns = Seq("id", "name")
  def apply(rs: WrappedResultSet, c: ResultName[Company]): Company = Company(rs.long(c.id), rs.stringOpt(c.name))
} 

// groups
case class Group(id: Long, name: Option[String], members: List[User] = Nil)

object Group extends SQLSyntaxSupport[Group] { 
  override def tableName = "groups"
  override def columns = Seq("id", "name")
  def apply(rs: WrappedResultSet, g: ResultName[Group]): Group = Group(rs.long(g.id), rs.stringOpt(g.name))
}

// group_members
case class GroupMember(groupId: Long, userId: Long)
object GroupMember extends SQLSyntaxSupport[GroupMember] {
  override def tableName = "group_members"
  override def columns = Seq("group_id", "user_id")
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
    .map(rs => User(rs, u.result.names, c.result.names)).list.apply()
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
    .foldLeft(List[Group]()){ case (groups, rs) => 
       val newGroup = Group(rs, g.result.names)
       val member = User(rs, u.result.names, c.result.names)

       groups.find(_.id == newGroup.id).map { group => 
         group.copy(members = member :: group.members) :: groups.filterNot(_.id == group.id)
       }.getOrElse { 
         newGroup.copy(members = List(member)) :: groups 
       }
     }
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

```scala
[info] Starting scala interpreter...
[info] 
[run-main] INFO scalikejdbc.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select users.id as id__on__users, users.name as name__on__users, users.company_id as company_id__on__users, companies.id as id__on__companies, companies.name as name__on__companies from users left join companies on users.company_id = companies.id; (16 ms)

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
   select u.id as id__on__u, u.name as name__on__u, u.company_id as company_id__on__u, g.id as id__on__g, g.name as name__on__g, c.id as id__on__c, c.name as name__on__c from group_members gm inner join users u on u.id = gm.user_id inner join groups g on g.id = gm.group_id left join companies c on u.company_id = c.id; (0 ms)

  [Stack Trace]
    ...
    $line1.$read$$iw$$iw$$anonfun$5.apply(<console>:79)
    $line1.$read$$iw$$iw$$anonfun$5.apply(<console>:76)
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

