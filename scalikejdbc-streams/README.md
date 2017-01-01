# ScalikeJDBC Streams

scalikejdbc-streams is a Reactive Streams 1.0 compliant implementation. We never release without passing the existing unit tests to satisfy [PublisherVerification](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck#structure-of-the-tck) in [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck).

This library is originally assumed to be suitable for batch applications. You need to keep borrowing a connection while someone is subscribing a stream provided by scalikejdbc-streams. Be aware of the max size of connection pool and carefully monitor the state of the pool.

## First Example

```scala
import scalikejdbc._
import scalikejdbc.streams._

// Prepare a connection pool in advance.
// http://scalikejdbc.org/documentation/configuration.html#scalikejdbc-config

// using a ThreadPoolExecutor that generates daemon threads is highly recommended
implicit val executor = AsyncExecutor(scala.concurrent.ExecutionContext.global)

val publisher = DB readOnlyStream {
  sql"select id from users".map(r => r.int("id")).iterator
}
publisher.subscribe(???)
```

## Supported RDBMS

At the current moment, scalikejdbc-streams natively supports MySQL and PostgreSQL.

For MySQL and PostgreSQL, scalikejdbc-streams automatically enables required settings to use cursor feature.

