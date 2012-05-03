# ScalikeJDBC - A thin JDBC wrapper in Scala


## Just write SQL

This is a thin JDBC wrapper library which just uses `java.sql.PreparedStatement` internally.

Users only need to write SQL and map from `java.sql.ResultSet` objects to Scala objects.

It's pretty simple, really.


But if you want simple mappers, please also try scalikejdbc-mapper-generator.

https://github.com/seratch/scalikejdbc-mapper-generator


## Setup

### sbt

ScalikeJDBC 1.0.0 will be released soon.

```scala
//resolvers += "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
resolvers += "sonatype" at "http://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.github.seratch" %% "scalikejdbc" % "1.0.0-SNAPSHOT"
```

### ls.implicit.ly

http://ls.implicit.ly/seratch/scalikejdbc

```
ls -n scalikejdbc
ls-install scalikejdbc
```

## Basic Usage

Please see this sample:

https://github.com/seratch/scalikejdbc/blob/master/src/test/scala/BasicUsageSpec.scala


## DB object

`scalikejdbc.DB` is the basic class in using this library.

It manages DB connection and also provides active sessions and transaction operations.


### Connection Management

There are two approaches to manage DB connections.

#### DriverManager

Using `java.sq.DriverManager` is the simplest approarch.

```scala
import scalikejdbc._
Class.forName(driverName)
val conn = DriverManager.getConnection(url, user, password)
val db = new DB(conn)
val name: Option[String] = db readOnly { session =>
  session.single("select * from emp where id = ?", 1) { _.string("name") }
}
db.close()
```


#### ConnectionPool

Using `scalikejdbc.ConnectionPool` is encouraged. 

It uses [Apache Commons DBCP](http://commons.apache.org/dbcp/) internally.

```scala
import scalikejdbc._
Class.forName(driverName)
ConnectionPool.singleton(url, user, password)
val conn = ConnectionPool.borrow()
val db = new DB(conn)
```

If you need to connect several data-sources:

```scala
import scalikejdbc._
Class.forName(driverName)
ConnectionPool.add('db1, url1, user1, password1)
ConnectionPool.add('db2, url2, user2, password2)
val conn = ConnectionPool('db1).borrow()
val db = new DB(conn)
```

Using DB/NamedDB object is  much simpler.

```scala
// borrow a new connection automatically
val name: Option[String] = DB readOnly { session =>
  session.single("select * from emp where id = ?", 1) { _.string("name") }
}
val name: Option[String] = NamedDB('named) readOnly { session =>
  session.single("select * from emp where id = ?", 1) { _.string("name") }
}
```


### Thread-local Connection

You can share DB connections as thread-local values.

```scala
def init() = {
  val newDB = ThreadLocalDB.create(conn)
  newDB.begin()
}
// after that..
def doSomething() = {
  val db = ThreadLocalDB.load()
}
```


## Operations

### Query API

ScalikeJDBC has various query APIs. 

`single`, `first`, `list` and `foreach`.

All of them executes `java.sql.PreparedStatement#executeUpdate()`.


#### single

`single` returns single row optionally.

```scala
val name: Option[String] = DB readOnly { session: DBSession =>
  session.single("select * from emp where id = ?", 1) { _.string("name") }
}

val extractName = (rs: WrappedResultSet) => rs.string("name")

val name: Option[String] = DB readOnly {
  _.single("select * from emp where id = ?", 1)(extractName)
}

case class Emp(id: String, name: String)
val emp: Option[Emp] = DB readOnly { 
  _.single("select * from emp where id = ?", 1) { 
    rs => Emp(rs.string("id"), rs.string("name"))
  }
}
```

#### first

`first` returns the first row optionally.

```scala
val name: Option[String] = DB readOnly {
  _.first("select * from emp") { _.string("name") }
}
```

#### list

`list` returns multiple rows as `scala.collection.immutable.List`.

```scala
val names: List[String] = DB readOnly {
  _.list("select * from emp") { _.string("name") }
}
```

#### foreach

`foreach` allows you to make some side-effect in iterations with `scala.collection.Traversable`.

```scala
DB readOnly {
  _.foreach("select * from emp") { rs => out.write(rs.string("name")) }
}
```


### Update API

`update` executes `java.sql.PreparedStatement#executeUpdate()`.

```scala
val conn = ConnectionPool.borrow()
val db = new DB(conn)
db.begin()

val inserted: Int = db withinTx { session =>
  session.update("""insert into emp (id, name, created_at) values (?, ?)""",
    123, "foo", new org.joda.time.DateTime) 
    // java.util.Date, java.sql.* are also available
}
val id: Long = db withTx { 
  _.updateAndReturnGeneratedKey("insert into emp (name, created_at) values (?, ?)", 
    "bar", new java.util.Date) 
}

val updated: Int  = db withinTx { 
  _.update("update emp set name = ? where id = ?", "bar", 1) 
}

val deleted: Int  = db withinTx { 
  _.update("delete emp where id = ?", 1) 
}

db.commit()
db.close()
```

### Execute API

`execute` executes `java.sql.PreparedStatement#execute()`.

```scala
DB autoCommit {
  _.execute("create table emp (id integer primary key, name varchar(30))")
}
```


## Transaction

### readOnly block / session

Execute query in read-only mode.

```scala
val names = DB readOnly {
  session => session.list("select * from emp") { rs => rs.string("name") }
}

val session = DB.readOnlySession
val names = session.list("select * from emp") { rs => rs.string("name") }
session.close()
```

Of course, updating in read-only mode will cause `java.sql.SQLException`.

```scala
val updateCount = DB readOnly {
  _.update("update emp set name = ? where id = ?", "foo", 1)
} // will throw java.sql.SQLException
```


### autoCommit block / session

Execute query / update in auto-commit mode.

```scala
val count = DB autoCommit {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
```

When using autoCommitSession, every operation will execute in auto-commit mode.

```scala
val session = DB.autoCommitSession
session.update("update emp set name = ? where id = ?", "foo", 1) // auto-commit
session.update("update emp set name = ? where id = ?", "bar", 2) // auto-commit
session.close()
```

### localTx block

Execute query / update in block-scoped transactions. 

If an Exception was thrown in the block, the transaction will perform rollback automatically.

```scala
val count = DB localTx { 
  // --- transcation scope start ---
  session => {
    session.update("update emp set name = ? where id = ?", "foo", 1)
    session.update("update emp set name = ? where id = ?", "bar", 2)
  } 
  // --- transaction scope end ---
}
```

### withinTx block / session

Execute query / update in already existing transctions.

`Tx#begin()`, `Tx#rollback()` or `Tx#commit()` should be handled. 

```scala
val db = new DB(conn)
db.begin()
val names = db withinTx {
  // if a transaction has not been started, IllegalStateException will be thrown
  session => session.list("select * from emp") {
    rs => rs.string("name")
  }
}
db.rollback() // it might throw Exception

db.begin()
val session = db.withinTxSession()
val names = session.list("select * from emp") {
  rs => rs.string("name")
}
db.rollbackIfActive() // it NEVER throws Exception
db.close()
```


## SQL, bind params, map, specify output type and apply

Not only using `DBSession` directly, but also using `SQL(String).map(f).{output}.apply()` API is useful.

```scala
val name: Option[String] = DB readOnly { implicit session =>
  SQL("select * from emp where id = ?").bind(1)
    .map { rs => rs.string("name") }.single.apply()
}
```

When you call the `#apply` method, the specified SQL statement will be executed and the result will be extracted from the `ResultSet` object, and finally the statement will be closed.

```scala
import scalikejdbc._
import scala.Option

DB autoCommit { implicit session =>

  case class Emp(id: Int, name: Option[String])
  val empMapper = (rs: WrappedResultSet) => Emp(rs.int("id"), Option(rs.string("name")))

  val emps: List[Emp] = SQL("select * from emp order by id limit 10").map(empMapper).list.apply() // or toList.apply()
  val firstEmp: Option[Emp] = SQL("select * from emp order by id limit 10").map(empMapper).first.apply() // or headOption.apply()
  val andy: Option[Emp] = SQL("select * from emp where id = ? and name = ?").bind(1, "Andy").map(empMapper).single.apply() // or toOption.apply()

  val result: Boolean = SQL("create table company (id integer primary key, name varchar(30))").execute.apply()
  val count: Int = SQL("insert into company values (?, ?)").bind(1,"Typesafe").update.apply()
  val id: Long = SQL("insert into company (name) values (?)").bind("Oracle").updateAndReturnGeneratedKey.apply()

}
```


## Real World Example

### TxFilter Example

The following is an example with `javax.servlet.Filter` which provides thread-local transactions.

for details at https://github.com/seratch/scalikejdbc/tree/master/src/test/scala/snippet/unfiltered.scala

```scala
class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) = {
    ConnectionPool.singleton(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) = {
    using(ConnectionPool.borrow()) {
      conn =>
        val db = ThreadLocalDB.create(conn)
        handling(classOf[Throwable]) by {
          case e: Exception =>
            db.rollbackIfActive()
            throw e
        } apply {
          db.begin()
          chain.doFilter(req, res)
          db.commit()
        }
    }
  }

  def destroy() = {}

}
```

### Unfiltered Example

Using the `TxFilter` in [unfiltered](https://github.com/unfiltered/unfiltered) app:

```scala
class PlanWithTx extends Plan {
  def intent = {
    case req @ GET(Path("/rollbackTest")) =>
      val db = ThreadLocalDB.load()
      db withinTx { _.update("update emp set name = ? where id = ?", "foo", 1) } // or _.executeUpdate
      throw new RuntimeException("Rollback Test!")
      // will perform rollback
  }
}

// Thread-based server
object Server1 extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new PlanWithTx)
    .run { s => unfiltered.util.Browser.open("http://127.0.0.1:%d/rollbackTest".format(s.port))}
}
```

### With Play framework 2.x

ScalikeJDBC works with Play framewrok(2.x).

https://github.com/playframework/Play20
https://github.com/seratch/scalikejdbc-play-plugin


ScalikeJDBC works fine with Anorm API.

```scala
import anorm._
import anorm.SqlParser._

case class Emp(id: Int, name: Option[String])

DB localTxWithConnection { implicit conn =>
  val allColumns = get[Int]("id") ~ get[Option[String]]("name") map { case id ~ name => Emp(id, name) }
  val empOpt: Option[Emp] = SQL("select * from emp where id = {id}").on('id -> 1).as(allColumns.singleOpt)
  val emps: List[Emp] = SQL("select * from emp").as(allColumns.*)
}
```


## Travis CI

[![Build Status](https://secure.travis-ci.org/seratch/scalikejdbc.png?branch=master)](http://travis-ci.org/seratch/scalikejdbc)

