---
title: Configuration - ScalikeJDBC
---

## Configuration

<hr/>
The following 3 things should be configured.

<hr/>
### JDBC Driver

In advance, JDBC drivers must be loaded by using

```
Class.forName(String)
```

or

```
java.sql.DriverManager.registerDriver(java.sql.Driver)
```

If you use `scalikejdbc-config` or `scalikejdbc-play-plugin`, they do the legacy work for you.

<hr/>
### Connection Pool Settings

ConnectionPool should be initialized on startu.

```java
import scalikejdbc._

// after loading JDBC drivers
ConnectionPool.singleton(url, user, password)
ConnectionPool.add('foo, user, password)

val settings = ConnectionPoolSettings(
  initialSize = 5,
  maxSize = 20,
  connectionTimeoutMillis = 3000L,
  validationQuery = "select 1 from dual")

// all the connections are released, old connection pool will be abandoned
ConnectionPool.add('foo, user, password, settings)
```

`ConnectionPool` and `ConnectionPoolSettings`'s parameters are like this:

```java
abstract class ConnectionPool(
  val url: String,
  val user: String,
  password: String,
  val settings: ConnectionPoolSettings)
```

```java
case class ConnectionPoolSettings(
  initialSize: Int,
  maxSize: Int,
  connectionTimeoutMillis: Long,
  validationQuery: String)
```

FYI: [Source Code](https://github.com/seratch/scalikejdbc/blob/master/scalikejdbc-library/src/main/scala/scalikejdbc/ConnectionPool.scala)


<hr/>
### Global Settings

Global settings for logging for query inspection and so on.

```java
object GlobalSettings {
  var loggingSQLErrors: Boolean
  var loggingSQLAndTime: LoggingSQLAndTimeSettings
  var sqlFormatter: SQLFormatterSettings
  var nameBindingSQLValidator: NameBindingSQLValidatorSettings
  var queryCompletionListener: (String, Seq[Any], Long) => Unit
  var queryFailureListener: (String, Seq[Any], Throwable) => Unit
}
```

FYI: [Source Code](https://github.com/seratch/scalikejdbc/blob/master/scalikejdbc-library/src/main/scala/scalikejdbc/GlobalSettings.scala)

<hr/>
### Using scalikejdbc-config

If you use `scalikejdbc-config` which is an easy-to-use configuration loader for ScalikeJDBC which reads typesafe config, configuration is much simple.

[Typesafe Config](https://github.com/typesafehub/config)

If you'd like to setup `scalikejdbc-config`, see setup page.

[/documentation/setup](/documentation/setup.html)

Configuration file should be like `src/main/resources/application.conf`. See Typesafe Config documentation in detail.

```
# JDBC settings
db.default.driver="org.h2.Driver"
db.default.url="jdbc:h2:file:db/default"
db.default.user="sa"
db.default.password=""
# Connection Pool settings
db.default.poolInitialSize=10
db.default.poolMaxSize=20
db.default.connectionTimeoutMillis=1000

db.legacy.driver="org.h2.Driver"
db.legacy.url="jdbc:h2:file:db/db2"
db.legacy.user="foo"
db.legacy.password="bar"
```

After just calling `scalikejdbc.config.DBs.setupAll()`, Connection pools are prepared.

```java
import scalikejdbc._, SQLInterpolation._
import scalikejdbc.config._

DBs.setupAll()
// DBs.setup()
// DBs.setup('legacy)

// loaded from "db.default.*"
val memberIds = DB readOnly { implicit session =>
  sql"select id from members".map(_.long(1)).list.apply()
}
// loaded from "db.legacy.*"
val legacyMemberIds = NamedDB('legacy) readOnly { implicit session =>
  sql"select id from members".map(_.long(1)).list.apply()
}

// wipes out ConnectionPool
DBs.closeAll()
```

<hr/>
### Using scalikejdbc-config with environment

It's also possible to add prefix(e.g. environment).

```
development.db.default.driver="org.h2.Driver"
development.db.default.url="jdbc:h2:file:db/default"
development.db.default.user="sa"
development.db.default.password=""

prod {
  db {
    sandbox {
      driver="org.h2.Driver"
      url="jdbc:h2:file:are-you-sure-in-production"
      user="user"
      password="pass"
    }
  }
}
```

Use `DBsWithEnv` instead of `DBs`.

```java
DBsWithEnv("development").setupAll()
DBsWithEnv("prod").setup('sandbox)
```

<hr/>
### Using scalikejdbc-config for Global Settings

The following settings are available.

```
# Global settings
scalikejdbc.global.loggingSQLAndTime.enabled=true
scalikejdbc.global.loggingSQLAndTime.logLevel=info
scalikejdbc.global.loggingSQLAndTime.warningEnabled=true
scalikejdbc.global.loggingSQLAndTime.warningThresholdMillis=1000
scalikejdbc.global.loggingSQLAndTime.warningLogLevel=warn
```



