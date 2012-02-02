# ScalikeJDBC - A thin JDBC wrapper in Scala


## Just write SQL

This is a thin JDBC wrapper library which just uses `java.sql.PreparedStatement` internally.

Users only need to write SQL and map from `java.sql.ResultSet` objects to Scala objects.

It's very simple.


## sbt

```scala
resolvers ++= Seq(
  "seratch.github.com releases"  at "http://seratch.github.com/mvn-repo/releases"
)

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "0.2.0" withSources ()
)
```

### ls.implicit.ly

http://ls.implicit.ly/seratch/scalikejdbc

```
ls -n scalikejdbc
ls-install scalikejdbc
```

## DB Access Object

### Connection Management

#### DriverManager

```scala
import scalikejdbc._
Class.forName(driverName)
val conn = DriverManager.getConnection(url, user, password)
val db = new DB(conn)
```

#### ConnectionPool (Apache Commons DBCP)

```scala
import scalikejdbc._
Class.forName(driverName)
ConnectionPool.singleton(url, user, password)
val conn = ConnectionPool.borrow()
val db = new DB(conn)
```

If you need to connect several datasources:

```scala
import scalikejdbc._
Class.forName(driverName)
ConnectionPool.add('db1, url1, user1, password1)
ConnectionPool.add('db2, url2, user2, password2)
val conn = ConnectionPool('db1).borrow()
val db = new DB(conn)
```


#### Thread-local Connection

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

### Query

#### asOne

`asOne` returns optionally single row.

```scala
val name: Option[String] = db readOnly { session =>
  session.asOne("select * from emp where id = ?", 1) { rs => Some(rs.getString("name")) }
}

val extractName = (rs: java.sql.ResultSet) => Some(rs.getString("name"))
val name: Option[String] = db readOnly {
  _.asOne("select * from emp where id = ?", 1)(extractName)
}
```
#### asList

`asList` returns multiple rows as `scala.collection.immutable.List`.

```scala
val names: List[String] = db readOnly {
  _.asList("select * from emp") { rs => Some(rs.getString("name")) }
}
```

#### asIterator

`asIterator` allows you to handle `scala.collection.Iterator` directly.

```scala
val iter: Iterator[String] = db readOnly {
  _.asIterator("select * from emp") { rs => Some(rs.getString("name")) }
}
iter.next()
iter.next()
```

#### foreach

`foreach` allows you to make some side-effect in the iteration with `scala.collection.Iterator`.

```scala
db readOnly {
  _.foreach("select * from emp") { rs => outout.write(rs.getString("name")) }
}
```


### Update

`update` executes `PreparedStatement#executeUpdate()`.

```scala
db.begin()
val inserted: Int = db withinTx { _.update("insert into emp (id, name) values (?, ?)", 1, "foo") }
val updated: Int  = db withinTx { _.update("update emp set name = ? where id = ?", "bar", 1) }
val deleted: Int  = db withinTx { _.update("delete emp where id = ?", 1) }
db.commit()
```

### Execute

`execute` executes `PreparedStatement#execute()`.

```scala
db autoCommit {
  _.execute("create table emp (id integer primary key, name varchar(30))")
}
```


## Transaction Control

### readOnly block / session object

```scala
val names = db readOnly {
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}

val session = db.readOnlySession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}

val updateCount = db readOnly {
  _.update("update emp set name = ? where id = ?", "foo", 1)
} // will throw java.sql.SQLException
```

### autoCommit block / session object

```scala
val count = db autoCommit {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
// cannot rollback

val session = db.autoCommitSession()
session.update("update emp set name = ? where id = ?", "foo", 1)
session.update("update emp set name = ? where id = ?", "bar", 2)
// cannot rollback
```

### localTx block

```scala
val count = db localTx {
  session => {
    session.update("update emp set name = ? where id = ?", "foo", 1)
    session.update("update emp set name = ? where id = ?", "bar", 2)
  }
}
// cannot rollback
```

### withinTx block / session object

```scala
db.begin()
val names = db withinTx {
  // if a transaction has not been started, IllegalStateException will be thrown
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}
db.rollback() // might throw Exception

db.begin()
val session = db.withinTxSession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}
db.rollbackIfActive() // NEVER throws Exception
```

## TxFilter example

See also: https://github.com/seratch/scalikejdbc/tree/master/src/test/scala/snippet/unfiltered.scala

```scala
class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) = {
    ConnectionPool.singleton(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) = {
    import scalikejdbc.LoanPattern._
    // using(java.sql.DriverManager.getConnection(url, user, password)) {
    using(ConnectionPool.borrow()) {
      conn => {
        val db = ThreadLocalDB.create(conn)
        handling(classOf[Throwable]) by {
          case e: Exception => {
            db.rollbackIfActive()
            throw e
          }
        } apply {
          db.begin()
          chain.doFilter(req, res)
          db.commit()
        }
      }
    }
  }

  def destroy() = {}

}
```

Unfiltered example:

```scala
class PlanWithTx extends Plan {
  def intent = {
    case req @ GET(Path("/rollbackTest")) => {
      val db = ThreadLocalDB.load()
      db withinTx { _.update("update emp set name = ? where id = ?", "foo", 1) }
      throw new RuntimeException("Rollback Test!")
      // will rollback
    }
  }
}

object Server1 extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new PlanWithTx)
    .run { s => unfiltered.util.Browser.open("http://127.0.0.1:%d/rollbackTest".format(s.port))}
}
```
