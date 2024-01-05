# ScalikeJDBC Streams

scalikejdbc-streams is a Reactive Streams 1.0 compliant implementation. We never release without passing the existing unit tests to satisfy [PublisherVerification](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck#structure-of-the-tck) in [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/v1.0.0/tck).

This library is originally assumed to be suitable for batch applications. You need to keep borrowing a connection while someone is subscribing a stream provided by scalikejdbc-streams. Be aware of the max size of connection pool and carefully monitor the state of the pool.

## First Example

```scala
import scalikejdbc._
import scalikejdbc.streams._

// Prepare a connection pool in advance.
// https://scalikejdbc.org/documentation/configuration.html#scalikejdbc-config

// Prepare an ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

val publisher: DatabasePublisher[Int] = DB.readOnlyStream {
  sql"select id from users order by id".map(r => r.get[Int]("id")).iterator
}

val subscriber = new SyncSubscriber[Int] {
  def foreach(element: Int): Boolean = {
    // do something with the element here
    true // true if you need more elements
  }
}
publisher.subscribe(subscriber)
```

## Supported RDBMS

At the current moment, scalikejdbc-streams natively supports MySQL and PostgreSQL.

For MySQL and PostgreSQL, scalikejdbc-streams automatically enables required settings to use cursor feature.

