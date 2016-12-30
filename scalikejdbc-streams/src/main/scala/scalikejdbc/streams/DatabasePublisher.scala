package scalikejdbc.streams

import org.reactivestreams.Publisher

/**
 * A database backend Publisher in the context of Reactive Streams
 *
 * http://www.reactive-streams.org/
 */
trait DatabasePublisher[A] extends Publisher[A]
