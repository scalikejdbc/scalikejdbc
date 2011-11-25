# ScalikeJDBC - A thin JDBC wrapper in Scala

### Just write SQL and start writing applications right now

This library is very simply implemented (similar to SpringJDBC) and it's usage is also very simple.

You just use PreparedStatement and map from ResultSet to Option[A] or List[A] by yourself.

## sbt

```scala
resolvers ++= Seq(
  "seratch.github.com releases"  at "http://seratch.github.com/mvn-repo/releases"
)

libraryDependencies ++= Seq(
  "com.github.seratch" %% "scalikejdbc" % "0.1.2" withSources ()
)
```

## Operations

### DB accessor object

Simply get Connection:

```scala
import scalikejdbc._
val conn = DriverManager.getConnection(url, user, password)
val db = new DB(conn)
```

or use Apache Commons DBCP:

```scala
import scalikejdbc._

Class.forName("org.hsqldb.jdbc.JDBCDriver")
ConnectionPool.initialize(url, user, password)
val conn = ConnectionPool.borrow()
val db = new DB(conn)
```

Store a DB instance as thread-local variable:

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

### Query

```scala
val names: List[String] = db readOnly {
  session => session.asList("select * from emp") { rs =>
    Some(rs.getString("name"))
  }
}
```

```scala
val extractName = (rs: java.sql.ResultSet) => Some(rs.getString("name"))
val name: Option[String] = db readOnly {
  _.asOne("select * from emp where id = ?", 1)(extractName)
}
```

### Update

```scala
val count: Int = db autoCommit {
   _.update("update emp set name = ? where id = ?", "foo", 1)
}
```

### Execute

```scala
db autoCommit {
  _.execute("create table emp (id integer primary key, name varchar(30))")
}
```

## Transaction

### readOnly

```scala
val names = db readOnly {
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}
```

```scala
val session = db.readOnlySession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}
val myName = session.asOne("select * from emp where id = ?", 1) {
  rs => Some(rs.getString("name"))
}
```

```scala
val count = db readOnly {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
// java.sql.SQLException!
```

### autoCommit

```scala
val count = db autoCommit {
  _.update("update emp set name = ? where id = ?", "foo", 1)
}
// cannot rollback
```

```scala
val session = db.autoCommitSession()
session.update("update emp set name = ? where id = ?", "foo", 1)
session.update("update emp set name = ? where id = ?", "bar", 2)
// cannot rollback
```

### localTx

```scala
val count = db localTx {
  session => {
    session.update("update emp set name = ? where id = ?", "foo", 1)
    session.update("update emp set name = ? where id = ?", "bar", 2)
  }
}
// cannot rollback
```

### withinTx

```scala
db.begin()
val names = db withinTx {
  // if a transaction has not been started, IllegalStateException will be thrown
  session => session.asList("select * from emp") {
    rs => Some(rs.getString("name"))
  }
}
db.rollback() // might throw Exception
```

```scala
db.begin()
val session = db.withinTxSession()
val names = session.asList("select * from emp") {
  rs => Some(rs.getString("name"))
}
db.rollbackIfActive() // never throw Exception
```

## DB instance as an implicit parameter

It's also possible to pass an instance of scalikejdbc.DB as an implicit parameter:

```scala
implicit val db = new DB(conn)
val names = readOnly { session => session.asList("select * from emp") { rs => Some(rs.getString("name")) } }
```

```scala
implicit val db = new DB(conn)
val count = autoCommit { _.update("update emp set name = ? where id = ?", "foo", 1) }
```

```scala
implicit val db = new DB(conn)
val count = localTx {
  s => {
    s.update("update emp set name = ? where id = ?", "foo", 1)
    s.update("update emp set name = ? where id = ?", "bar", 2)
  }
}
```

```scala
implicit val db = new DB(conn)
val names = withinTx { s => s.asList("select * from emp") { rs => Some(rs.getString("name")) } }
```

## TxFilter example

See also: https://github.com/seratch/scalikejdbc/tree/master/src/test/scala/snippet/unfiltered.scala

```scala
class TxFilter extends Filter {

  def init(filterConfig: FilterConfig) {
    ConnectionPool.initialize(url, user, password)
  }

  def doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {

    // simply using DriverManager
    //    val conn = DriverManager.getConnection(url, user, password)

    // using Commons DBCP
    import scalikejdbc.LoanPattern._
    using(ConnectionPool.borrow()) {
      conn => {
        val db = ThreadLocalDB.create(conn)
        db.begin()
        try {
          chain.doFilter(req, res)
          db.commit()
        } catch {
          case e: Exception => {
            db.rollbackIfActive()
            throw e
          }
        }
      }
    }

  }

  def destroy() {
  }

}
```

Unfiltered example:

```scala
class TxSample1 extends Plan {
  def intent = {
    case req @ GET(Path("/rollbackTest")) => {
      val db = ThreadLocalDB.load()
      db withinTx { _.update("update emp set name = ? where id = ?", "foo", 1) }
      throw new RuntimeException("Rollback Test!")
      // The transaction will rollback.
    }
  }
}

object Server1 extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new TxSample1)
    .run { s => unfiltered.util.Browser.open("http://127.0.0.1:%d/rollbackTest".format(s.port))}
}
```

DB instance as an implicit parameter:

```scala
trait TxSupport {
  implicit val db = ThreadLocalDB.load()
}

class Hello2 extends Plan {
  this: TxSupport =>
  def intent = {
    case req @ GET(Path("/rollbackTest")) => {
      withinTx { _.update("update emp set name = ? where id = ?", "foo", 1) }
      throw new RuntimeException("Rollback Test!")
      // The transaction will rollback.
    }
  }
}

object Server2 extends App {
  unfiltered.jetty.Http.anylocal
    .filter(new TxFilter)
    .plan(new TxSample2 with DBTxSupport)
    .run { s => unfiltered.util.Browser.open("http://127.0.0.1:%d/rollbackTest".format(s.port))}
}
```
