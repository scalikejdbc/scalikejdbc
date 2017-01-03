package scalikejdbc.streams

import scalikejdbc._

/**
 * Streaming-ready SQL object.
 *
 * The primary constructor is intentionally hidden, use only StreamSQL object's apply method to instantiate.
 */
private[streams] case class StreamReadySQL[A] private (underlying: SQL[A, HasExtractor]) {

  private[streams] lazy val extractor: (WrappedResultSet) => A = underlying.extractor

  private[streams] lazy val statement: String = underlying.statement
  private[streams] lazy val rawParameters: Seq[Any] = underlying.rawParameters
  private[streams] lazy val parameters: Seq[Any] = underlying.parameters

  private[streams] lazy val fetchSize: Option[Int] = underlying.fetchSize
  private[streams] lazy val tags: Seq[String] = underlying.tags

  private[streams] lazy val queryTimeout: Option[Int] = underlying.queryTimeout

}

private[streams] object StreamReadySQL {

  /**
   * The only way to instantiate StreamSQL.
   */
  private[streams] def apply[A, E <: WithExtractor](sql: SQL[A, E], fetchSize: Int): StreamReadySQL[A] = {
    val underlying: SQL[A, HasExtractor] = {
      (new SQL[A, HasExtractor](sql.statement, sql.rawParameters)(sql.extractor) {})
        .fetchSize(fetchSize)
    }
    new StreamReadySQL(underlying)
  }

}
