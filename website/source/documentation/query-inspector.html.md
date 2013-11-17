---
title: Query Inspector - ScalikeJDBC
---

## Query Inspector

<hr/>
### Checking the actual SQL and timing

Using LogginSQLAndTime feature, you can check the actual SQL(not exactly) and time.

<hr/>
### Settings

```java
import scalikejdbc._
GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
  enabled = true,
  logLevel = 'DEBUG,
  warningEnabled = true,
  warningThresholdMillis = 1000L,
  warningLogLevel = 'WARN
)
```

<hr/>
### Output

For example, logging as follows:

```sh
[debug] s.StatementExecutor$$anon$1 - SQL execution completed

  [Executed SQL]
   select * from user where email = 'guillaume@sample.com'; (0 ms)

  [Stack Trace]
    ...
    models.User$.findByEmail(User.scala:26)
    controllers.Projects$$anonfun$index$1$$anonfun$apply$1$$anonfun$apply$2.apply(Projects.scala:20)
    controllers.Projects$$anonfun$index$1$$anonfun$apply$1$$anonfun$apply$2.apply(Projects.scala:19)
    controllers.Secured$$anonfun$IsAuthenticated$3$$anonfun$apply$3.apply(Application.scala:88)
    controllers.Secured$$anonfun$IsAuthenticated$3$$anonfun$apply$3.apply(Application.scala:88)
    play.api.mvc.Action$$anon$1.apply(Action.scala:170)
    play.api.mvc.Security$$anonfun$Authenticated$1.apply(Security.scala:55)
    play.api.mvc.Security$$anonfun$Authenticated$1.apply(Security.scala:53)
    play.api.mvc.Action$$anon$1.apply(Action.scala:170)
    play.core.ActionInvoker$$anonfun$receive$1$$anonfun$6.apply(Invoker.scala:126)
    play.core.ActionInvoker$$anonfun$receive$1$$anonfun$6.apply(Invoker.scala:126)
    play.utils.Threads$.withContextClassLoader(Threads.scala:17)
    play.core.ActionInvoker$$anonfun$receive$1.apply(Invoker.scala:125)
    play.core.ActionInvoker$$anonfun$receive$1.apply(Invoker.scala:115)
    akka.actor.Actor$class.apply(Actor.scala:318)
    ...
```

<hr/>
### Single Line Mode

If you don't need stack trace logging and just print SQL in single line, use `singleLineMode = true`.

```java
GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
  enabled = true,
  singleLineMode = true,
  logLevel = 'DEBUG
)
```

In this case, logging as follows:

```sh
2013-05-26 16:23:08,072 DEBUG [pool-4-thread-4] s.StatementExecutor$$anon$1 [Log.scala:81] [SQL Execution] select * from user where email = 'guillaume@sample.com'; (0 ms)
```

<hr/>
### Sending data to external services

You can use hooks such as `GlobalSettings.queryCompletionListener` and `GlobalSettings.queryFailureListener`.

```java
import org.fluentd.logger.scala._
val logger = FluentLoggerFactory.getLogger("scalikejdbc")

GlobalSettings.queryCompletionListener = (sql: String, params: Seq[Any], millis: Long) => {
  if (millis > 1000L) {
    logger.log("completion", Map(
      "sql" -> sql,
      "params" -> params.mkString("[", ",", "]"),
      "millis" -> millis))
  }
}

val counts = DB readOnly { implicit s =>
  sql"select product_id, count(*) from orders group by product_id"
    .map(rs => OrderCount(rs)).list.apply()
}
```

