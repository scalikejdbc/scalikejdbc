---
title: One-to-X API - ScalikeJDBC
---

## One-to-X API

<hr/>
### Why One-to-x APIs are needed?

Users can write all the mapping operations by using `#map` or `#foldLeft`... with a lot of boilerplate code.

ScalikeJDBC provides you some useful APIs to map results to objects.

<hr/>
### one-to-many

Simple example:

```java
case class Member(id: Long, name: String)
case class Group(id: Long, name: String,
  members: Seq[Member] = Nil)

object Group extends SQLSyntaxSupport[Group] { /* ... */ }
object Member extends SQLSyntaxSupport[Member] {
  override val tableName = "members"
  def opt(m: ResultName[Member])(rs: WrappedResultSet) = rs.longOpt(m.id).map(_ => Member(m)(rs))
}

val (g, m) = (Group.syntax, Member.syntax)
val groups: List[Group] = withSQL {
    select.from(Group as g).leftJoin(Member as m).on(g.id, m.groupId)
  }.one(Group(g))
   .toMany(Member.opt(m))
   .map { (group, members) => group.copy(members = members) }
   .list.apply()
```

`one.toManies` supports 5 tables to join.

```java
case class Member(id: Long, name: String)
case class Event(id: Long, name: String) { /* ... */ }
case class Group(id: Long, name: String,
  events: Seq[Event] = Nil,
  members: Seq[Member] = Nil)

val (g, m, e) = (Group.syntax, Member.syntax, Event.syntax)
val groups: List[Group] = withSQL {
  select
    .from(Group as g)
    .leftJoin(Member as m).on(g.id, m.groupId)
    .leftJoin(Event as e).on(g.id, e.groupId)
  }.one(Group(m))
   .toManies(
     rs => Member.opt(g)(rs),
     rs => Event(e)(rs))
   .map { (group, members, events) => group.copy(members = members, events = events) }
   .list.apply()
```

<hr/>
### one-to-one

`one.toOne` for inner join queries.

```java
case class Owner(id: Long, name: String)
case class Group(id: Long, name: String,
  ownerId: Long,
  owner: Option[Owner] = None) { /* ... */ }

val (g, o) = (Group.syntax, Owner.syntax)
val groups: List[Group] = withSQL {
  select
    .from(Group as g)
    .innerJoin(Owner as o).on(g.ownerId, o.id)
  }.one(Group(g))
   .toOne(Owner(o))
   .map { (group, owner) => group.copy(owner = Some(owner)) }
   .list.apply()
```

If you don't want to define `owner` as an optional value, use `#map` instead.

```java
case class Owner(id: Long, name: String)
case class Group(id: Long, name: String,
  ownerId: Long,
  owner: Owner)

object Group extends SQLSyntaxSupport[Group] {
  def apply(g: ResultName[Group], o: ResultName[Owner])(rs: WrappedResultSet) = new Group(
    id = rs.long(g.id),
    name = rs.string(g.name),
    ownerId = rs.long(g.ownerId),
    group = Owner(id = rs.long(o.id),
    name = rs.string(o.name))
  )
}

val (g, o) = (Group.syntax, Owner.syntax)
val groups: List[Group] = withSQL {
    select.from(Group as g).innerJoin(Onwer as o).on(g.ownerId, o.id)
  }.map(Group(g, o)).list.apply()
```

`one.toOptionalOne` for outer join queries.

```java
case class Owner(id: Long, name: String)
case class Group(id: Long, name: String,
  ownerId: Option[Long] = None,
  owner: Option[Owner] = None) { /* ... */ }

val (g, o) = (Group.syntax, Owner.syntax)
val groups: List[Group] = withSQL {
  select.from(Group as g).leftJoin(Owner as o).on(g.ownerId, o.id)
  }.one(Group(g))
   .toOptionalOne(Owner.opt(o))
   .map { (group, owner) => group.copy(owner = owner) }
   .list.apply()
```
