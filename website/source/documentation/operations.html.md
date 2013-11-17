---
title: Operations - ScalikeJDBC
---

## Operations

<hr/>
### Query API

There are various query APIs.

All of them (single, first, list and foreach) will execute `java.sql.PreparedStatement#executeQuery()`.

<hr/>
### single

`single` returns matched single row as an `Option` value. If matched rows is not single, Exception will be thrown.

```java
import scalikejdbc._, SQLInterpolation._

val id = 123

// simple example
val name: Option[String] = DB readOnly { implicit session =>
  sql"select * from emp where id = ${id}".map(rs => rs.string("name")).single.apply()
}

// defined mapper as a function
val nameOnly = (rs: WrappedResultSet) => rs.string("name")
val name: Option[String] = DB readOnly { implicit session =>
  sql"select * from emp where id = ${id}".map(nameOnly).single.apply()
}

// define a class to map the result
case class Emp(id: String, name: String)
val emp: Option[Emp] = DB readOnly { implicit session =>
  sql"select * from emp where id = ${id}"
    .map(rs => Emp(rs.string("id"), rs.string("name"))).single.apply()
}
```

<hr/>
### first

`first` returns the first row of matched rows as an `Option` value.

```java
val name: Option[String] = DB readOnly { implicit session =>
  sql"select * from emp".map(rs => rs.string("name")).first.apply()
}
```

<hr/>
### first

`list` returns matched multiple rows as `scala.collection.immutable.List`.

```java
val name: List[String] = DB readOnly { implicit session =>
  sql"select * from emp".map(rs => rs.string("name")).list.apply()
}
```

<hr/>
### foreach

`foreach` allows you to make some side-effect in iterations. This API is useful for handling large `ResultSet`.

```java
DB readOnly { implicit session =>
  sql"select * from emp" foreach { rs => out.write(rs.string("name")) }
}
```

<hr/>
### Update API

`update` executes `java.sql.PreparedStatement#executeUpdate()`.

```java
DB localTx { implicit session =>
  sql"""insert into emp (id, name, created_at) values (${id}, ${name}, ${DateTime.now})"""
    .update.apply()
  val id = sql"insert into emp (name, created_at) values (${name}, current_timestamp)"
    .updateAndReturnGeneratedKey.apply()
  sql"update emp set name = ${newName} where id = ${id}".update.apply()
  sql"delete emp where id = ${id}".update.apply()
}
```

<hr/>
### Execute API

`execute` executes `java.sql.PreparedStatement#execute()`.

```java
DB autoCommit { implicit session =>
  sql"create table emp (id integer primary key, name varchar(30))".execute.apply()
}
```

<hr/>
### Batch API

`batch` and `batchByName` executes `java.sql.PreparedStatement#executeBatch()`.

```java
DB localTx { implicit session =>
  val batchParams1: Seq[Seq[Any]] = (2001 to 3000).map(i => Seq(i, "name" + i))
  sql"insert into emp (id, name) values (?, ?)".batch(batchParams1: _*).apply()
}
```
