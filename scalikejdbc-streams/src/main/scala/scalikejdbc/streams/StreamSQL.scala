package scalikejdbc.streams

import scalikejdbc._

/**
 * Streaming-ready SQL object.
 *
 * The primary constructor is intentionally hidden, use only StreamSQL object's apply method to instantiate.
 */
case class StreamSQL[A] private (underlying: SQL[A, HasExtractor]) {

  lazy val extractor: (WrappedResultSet) => A = underlying.extractor

  lazy val statement: String = underlying.statement
  lazy val rawParameters: Seq[Any] = underlying.rawParameters
  lazy val parameters: Seq[Any] = underlying.parameters

  lazy val fetchSize: Option[Int] = underlying.fetchSize
  lazy val tags: Seq[String] = underlying.tags

  lazy val queryTimeout: Option[Int] = underlying.queryTimeout

}

object StreamSQL {

  /**
   * The only way to instantiate StreamSQL.
   */
  def apply[A, E <: WithExtractor](sql: SQL[A, E], fetchSize: Int): StreamSQL[A] = {
    val underlying = {
      (new SQL[A, HasExtractor](sql.statement, sql.rawParameters)(sql.extractor) {})
        .fetchSize(fetchSize)
    }
    new StreamSQL(underlying)
  }

}
