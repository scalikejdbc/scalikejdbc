---
title: Connection Pool - ScalikeJDBC
---

## Connection Pool

<hr/>
### Configuration

See [/documentation/configuration](/documentation/configuration.html)


<hr/>
### Borrowing a connection from pool

Simply just call `#borrow` method.

```java
import scalikejdbc._
// default
val conn: java.sql.Connection = ConnectionPool.borrow()
// named
val conn: java.sql.Connection = ConnectionPool('named).borrow()
```

Be careful. The connection object should be released by yourself.

Basically using loan pattern is recommended to avoid human errors.

```java
using(ConnectionPool.borrow()) { conn =>
  // do something
}
```

ScalikeJDBC wraps a `java.sql.Connection` object as a `scalikejdbc.DB` object.

```java
using(DB(ConnectionPool.borrow())) { db =>
  // ...
}
```

`DB` object can provide `DBSession` for each operation.

```java
using(DB(ConnectionPool.borrow())) { db =>
  db.readOnly { implicit session =>
    // ...
  }
}
```

Right, above code is too verbose! Using DB object make it much simpler.

You can simplify the same thins by using `DB` or `NamedDB` objects and it's the common usage of ScalikeJDBC.

```java
// default
DB readOnly { implicit session =>
  // ...
}
// named
NamedDB('named) readOnly { implicit session =>
  // ...
}
```

<hr/>
### Thread-local Connection pattern

You can share DB connections as thread-local values. The connection should be released by yourself.

```java
def init() = {
  val newDB = ThreadLocalDB.create(conn)
  newDB.begin()
}
// after that..
def action() = {
  val db = ThreadLocalDB.load()
}
def finalize() = {
  try { ThreadLocalDB.load().close() } catch { case e => }
}
```

<hr/>
### Replacing ConnectionPool

If you want to use another one which is not Commons DBCP as the connection provider, You can also specify your own `ConnectionPoolFactory` as follows:

```java
/**
 * c3p0 Connection Pool Factory
 */
object C3P0ConnectionPoolFactory extends ConnectionPoolFactory {
  override def apply(url: String, user: String, password: String,
    settings: ConnectionPoolSettings = ConnectionPoolSettings()) = {
    new C3P0ConnectionPool(url, user, password, settings)
  }
}

/**
 * c3p0 Connection Pool
 */
class C3P0ConnectionPool(
  override val url: String,
  override val user: String,
  password: String,
  override val settings: ConnectionPoolSettings = ConnectionPoolSettings())
  extends ConnectionPool(url, user, password, settings) {

  import com.mchange.v2.c3p0._
  private[this] val _dataSource = new ComboPooledDataSource
  _dataSource.setJdbcUrl(url)
  _dataSource.setUser(user)
  _dataSource.setPassword(password)
  _dataSource.setInitialPoolSize(settings.initialSize)
  _dataSource.setMaxPoolSize(settings.maxSize);
  _dataSource.setCheckoutTimeout(settings.connectionTimeoutMillis.toInt);

  override def dataSource: DataSource = _dataSource
  override def borrow(): Connection = dataSource.getConnection()
  override def numActive: Int = _dataSource.getNumBusyConnections(user, password)
  override def numIdle: Int = _dataSource.getNumIdleConnections(user, password)
  override def maxActive: Int = _dataSource.getMaxPoolSize
  override def maxIdle: Int = _dataSource.getMaxPoolSize
  override def close(): Unit = _dataSource.close()
}

implicit val factory = C3P0ConnectionPoolFactory
ConnectionPool.add('xxxx, url, user, password)
```

