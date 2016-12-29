# ScalikeJDBC Streams

See the website.

http://scalikejdbc.org/

It is assumed to be used in batch application.
In JDBC, you need to keep connections constantly while subscribing to streaming. Therefore, please reconsider the connection pool setting and always be careful not to exhaust the connection.


## Example

```scala
import scalikejdbc._
import scalikejdbc.streams._

// We recommend that you prepare a ThreadPoolExecutor that generates daemon threads
implicit val executor = AsyncExecutor(scala.concurrent.ExecutionContext.global)

val publisher = DB stream {
  sql"select id from users".map(r => r.int("id")).cursor
}
publisher.subscribe(???)
```

Please refer to the document on usage of ScalikeJDBC such as connection pool initialization
http://scalikejdbc.org/documentation/configuration.html


# Note

For MySQL, PostgreSQL, and others, several settings are required to enable CURSOR (Streaming).
In ScalikeJDBC-streams, if driverName can be identified, this setting is automatically enabled.
Currently only MySQL and PostgreSQL are supported.

```scala
import scalikejdbc._
val poolSettings = ConnectionPoolSettings(driverName = "com.mysql.jdbc.Driver")
Class.forName(poolSettings.driverName)
ConnectionPool.singleton("jdbc:mysql://127.0.0.1/scalikejdbc_streams_test", "user", "pass", poolSettings)
```

With scalikejdbc-config you can easily delegate these settings to application.conf.
http://scalikejdbc.org/documentation/configuration.html#scalikejdbc-config


# ReactiveStreams TCK

TCK is released in ReactiveStreams.
https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck
ScalikeJDBC-streams passes PublisherVerification.
However, although it is also described in Section [Structure of the TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck#structure-of-the-tck),
Some tests can not construct (meaningful) automatic test.
If you have a good idea please send us a PullRequest!
