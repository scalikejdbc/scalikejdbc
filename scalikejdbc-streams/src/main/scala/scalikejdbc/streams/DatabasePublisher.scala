package scalikejdbc.streams

import org.reactivestreams.Publisher

trait DatabasePublisher[A] extends Publisher[A]
