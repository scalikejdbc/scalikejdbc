package scalikejdbc.streams

import scalikejdbc.WithExtractor
import scalikejdbc.streams.sql.StreamingSQL

object DatabasePublisherFactory {

  def createNewPublisher[A, E <: WithExtractor](
    publisherSettings: DatabasePublisherSettings[A, E],
    streamingSql: StreamingSQL[A, E]
  ): DatabasePublisher[A, E] = {
    val emitter = new StreamingEmitter[A, E]()
    new DatabasePublisher[A, E](publisherSettings, streamingSql, emitter)
  }

}
