# ScalikeJDBC Sandbox

## Query Example

Query Interface provides you fluent APIs to write SQL as-is.

```scala
// implicit val session = AutoSession
// val (u, g, gm, c) = (User.syntax("u"), Group.syntax("g"), GroupMember.syntax("gm"), Company.syntax("c"))

val alice: Option[User] = withSQL {
  select
    .from(User as u)
    .leftJoin(Company as c).on(u.companyId, c.id)
    .where.eq(u.name, "Alice")
}.map(User(u, c)).single.apply()

val groups: List[Group] = withSQL {
  select
    .from(GroupMember as gm)
    .innerJoin(User as u).on(u.id, gm.userId)
    .innerJoin(Group as g).on(g.id, gm.groupId)
    .leftJoin(Company as c).on(u.companyId, c.id)
    .where.eq(g.id, 1)
    .orderBy(u.id).desc
}.one(Group(g)).
 toMany(User.opt(u, c)).
 map { (g, members) => g.copy(members = members) }.
 list.
 apply()
```

Of course, it's also fine to write SQLInterpolation.

```scala
val name = "Alice"
val alice: Option[User] = sql"""
  select ${u.result.*}, ${c.result.*}
  from ${User.as(u)} left join ${Company.as(c)} on ${u.companyId} = ${c.id}
  where ${u.name} = ${name}
""".map(User(u, c)).single.apply()

val groups: List[Group] = sql"""
  select
    ${u.result.*}, ${g.result.*}, ${c.result.*}
  from
    ${GroupMember.as(gm)}
    inner join ${User.as(u)} on ${u.id} = ${gm.userId}
    inner join ${Group.as(g)} on ${g.id} = ${gm.groupId}
    left join ${Company.as(c)} on ${u.companyId} = ${c.id}
  where ${g.id} = 1
  order by ${u.id} desc
""".
 one(Group(g)).
 toMany(User.opt(u, c)).
 map { (g, members) => g.copy(members = members) }.
 list.
 apply()
```

## How to run

```sh
git clone git://github.com/seratch/scalikejdbc.git
cd scalikejdbc/sandbox
sbt console
```

