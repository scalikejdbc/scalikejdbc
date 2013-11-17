---
title: SQLInterpolation - ScalikeJDBC
---

## SQLInterpolation

<hr/>
### What's SQLInterpolation

SQLInterpolation is an SQL builder which uses String interpolation since Scala 2.10. Add scalikejbdc-interpolation to libraryDependencies.

```
libraryDependencies += "com.github.seratch" %% "scalikejdbc-interpolation" % "[1.6,)"
```

The usage is pretty simple - just embedding values into sql"" template without `#bind` or `#bindByName`. It looks really cool.

```java
import scalikejdbc._, SQLInterpolation._

val id = 123
val member = sql"select id, name from members where id = ${id}"
    .map(rs => Member(rs)).single.apply()

// above code is same as follows:
// SQL("select id, name from members where id = {id}").bindByName('id -> id)
//   .map(rs => Member(rs)).single.apply()
```

Don't worry, this code is safely protected from SQL injection attacks. `${id}` will be a place holder.

```sql
select id, name from members where id = ?
```

<hr/>
### SQLSyntax

`SQLSyntax` is not a binding parameter but a part of SQL. You can create a `SQLSyntax` object with sqls"" String interpolation.

```java
val ordering = if (isDesc) sqls"desc" else sqls"asc"
val members = sql"select id, name from members limit 10 order by id ${ordering}"
  .map(rs => Member(rs)).list.apply()
```

`${ordering}` will be directly converted to a part of SQL.

```sql
select id , name from members limit 10 order by id desc
```

Don't worry again, this code is safely protected. `sqls""` always treats external input values as binding parameters.

<hr/>
### SQLSyntaxSupport

SQLSyntaxSupport is one step short of ORM. In other words, it's a powerful DRY way to write SQL.

First of all, mix the `SQLSyntaxSupport[A]` trait to `A` companion object, and define `tableName` and `#apply` method to map values from `ResultSet` object in it. The unfamilier `ResultName[A]` are described later.

```java
import scalikejdbc._, SQLInterpolation._

case class Group(id: Long, name: String)
case class GroupMember(id: Long, name: String, groupId: Option[Long] = None, group: Option[Group])

object Group extends SQLSyntaxSupport[Group] {
  // If the table name is same as snake_case'd name of this companion object,
  // you don't need to specify tableName explicitly.
  override val tableName = "groups"
  def apply(g: ResultName[Group])(rs: WrappedResultSet) =
    new Group(rs.long(g.id), rs.string(g.name))
}

object GroupMember extends SQLSyntaxSupport[GroupMember] {
  def apply(m: ResultName[GroupMember])(rs: WrappedResultSet) =
    new GroupMember(rs.long(m.id), rs.string(m.name), rs.longOpt(m.groupId))

  def apply(m: ResultName[GroupMember], g: ResultName[Group])(rs: WrappedResultSet) =  {
    apply(m)(rs).copy(group = g.longOpt(g.id).map(_ => Group(g)(rs)))
  }
}
```

Use them as follows:

```java
val id = 123

val (m, g) = (GroupMember.syntax("m"), Group.syntax("g"))
val groupMember: Option[GroupMember] = sql"""
  select
    ${m.result.*}, ${g.result.*}
  from
    ${GroupMember.as(m)} left join ${Group.as(g)} on ${m.groupId} = ${g.id}
  where
    ${m.id} = ${id}
  """
  .map(GroupMember(m, g)).single.apply()
```

Though the above code contains some `${...}` parts, I believe that you can understand what it means. Actually, this code runs the following SQL statement.

```sql
select
   m.id as i_on_m, m.name as n_on_m, m.group_id as gi_on_m, g.id as i_on_g, g.name as n_on_g
from
  group_member m left join group g on m.group_id = g.id
where
  m.id = ?
```

It seems to be natural that `${m.result.*}` is converted to listing all the columns, `${m.camelCase}` is converted to the `snake_case` column name. However, since you may have questions, I'll explain about some points.

#### Why is ${m.result.*} transformed to listing all the columns?

In spite of column names are not defined anywhere, `${m.result.*}` is converted to listing all the column names. The secret is that laoding from metadata (and they will be cached) when first access to the table. It's possible to obtain all column names as a `Seq[String]` value via `columns`.

If you need to define column names by yourself or need to access multi databases, please define column names as follows:

```java
object GroupMember extends SQLSyntaxSupport[GroupMember] {
  override val tableName = "group_member"
  override val columns = Seq("id", "name", "group_id")
}
```

#### Why can we use undefined methods such as m.groupId?

By Type Dynamic (SIP-17) since Scala 2.10.0, you can call undefined methods such as `m.groupId`. Type Dynamic is similar to Ruby's method_missing.

https://docs.google.com/document/d/1XaNgZ06AR7bXJA9-jHrAiBVUwqReqG4-av6beoLaf3U

When you call camel case fields, it will actually be transformed to the column name as an underscore separated string. If the column name doesn't exist in the columns, `InvalidColumnNameException` will be thrown.

Before that, the case case field name should be the same as any of the primary constructor arg names of type `A` of `SQLSyntaxSupport[A]`. The validation for this rule works in compilation phase with the power of Scala macros.

http://docs.scala-lang.org/overviews/macros/overview.html

If you don't want to use Type Dynamic, it's also possible to call `#field(String)` or `#column(String)` with String value. For instance, the following four examples mean the same thing.

```java
m.groupId
m.field("groupId")
m.column("group_id")
m.c("group_id")
```

In some cases, you might want to avoid expose column names to application layer. If this is the case, override `nameConverters`. If you want to treat `service_cd` column as `serviceCode`, define that as follows. Since the `nameConverters` works as partial match retrieval, it's also possible to specify just `Map("Code" -> "cd")`.

```java
case class Event(id: Long, name: String, serviceCode: Long)

object Event extends SQLSyntaxSupport[Event] {
  override val tableName = "events"
  override val columns = Seq("id" "name", "service_cd")
  override val nameConverters = Map("serviceCode" -> "service_cd")
}
```

#### What is the difference between m.id, m.result.id and m.resultName.id?

For instance, `m`'s APIs of `val m = Member.syntax("mm")` means as follows:

- `m.groupId` will be converted to `sqls"mm.group_id"`
- `m.result.groupId` will be converted to `sqls"mm.group_id as gi_on_mm"`
- `m.resultName` returns a `ResultName[Member]` object
- `m.resultName.groupId` will be converted to `sqls"gi_on_mm"`

- `Member.as(m)` will be converted to `members m` in SQL

If you use `Member.syntax()`, tableName will be set. For example,  `m.result.groupId` will be `"members.group_id as gi_on_members"`. If you specified as `Member.syntax("m")`, `m.result.groupId` will be `"members.group_id as gi_on_m"`.

That's all the rules of `SQLSyntaxSupport`. I believed that now the following code will be easy to understand for you.

```java
val ids: List[Long] = sql"select ${m.result.id} from ${Member.as(m)} where ${m.gourpId} = 1"
  .map(rs => rs.long(m.resultName.id)).list.apply()
```

Define `#apply(ResultName[Member])` method and the method will make your `#map` operation pretty simple.

```java
object Member extends SQLSyntaxSupport[Member] {
  override val tableName = "members"
  def apply(m: ResultName[User])(rs: WrappedResultSet) = {
    new Member(id = rs.long(m.id), name = rs.string(m.name))
  }
}
```

Use the above code like this:

```java
val m = Member.syntax("m")
val members = sql"select ${m.result.*} from ${Member.as(m)}".map(Member(m)).list.apply()
// select m.id as i_on_m, m.name as n_on_m from members m
```

If you need just column names for insert/update/delete queries, use `#column.{name}` like this:

```java
val c = Member.column
sql"insert into ${Member.table} (${c.name}, ${c.birthday}) values (${name}, ${birthday})"
  .update.apply()
```

Since `#column` is a member of `SQLSyntaxSupport[A]`, you can use `#column` in `Member` object:

```java
object Member extends SQLSyntaxSupport[Member] {
  def create(name: String, birthday: LocalDate)(implicit s: DBSession = AutoSession): Member = {
    val id = sql"insert into ${table} (${column.name}, ${column.birthday}) values (${name}, ${birthday})"
      .updateAndReturnGeneratedKey.apply()
    Member(id, name, birthday)
  }
```

