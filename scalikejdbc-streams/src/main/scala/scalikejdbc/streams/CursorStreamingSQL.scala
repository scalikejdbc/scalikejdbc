package scalikejdbc.streams

import scalikejdbc._

private[streams] class CursorStreamingSQL[A, E <: WithExtractor](
    override val underlying: SQL[A, E]
) extends StreamingSQL[A, E](underlying) {

  override def extractor: (WrappedResultSet) => A = underlying.extractor

  /**
   * Builds a cursor supported session.
   */
  override private[streams] def updateDBSessionWithSQLAttributes(original: DBSession): DBSession = {

    val session: DBSession = super.updateDBSessionWithSQLAttributes(original)

    // setup required settings to enable cursor operations
    session.connectionAttributes.driverName match {
      case Some(driver) if driver == "com.mysql.jdbc.Driver" && underlying.fetchSize.exists(_ > 0) =>
        /*
         * MySQL - https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-implementation-notes.html
         *
         * StreamAction.StreamingInvoker prepares the following required settings in advance:
         *
         * - java.sql.ResultSet.TYPE_FORWARD_ONLY
         * - java.sql.ResultSet.CONCUR_READ_ONLY
         *
         * If the fetchSize is set as 0 or less, we need to forcibly change the value with the Int min value.
         */
        session.fetchSize(Int.MinValue)

      case Some(driver) if driver == "org.postgresql.Driver" =>
        /*
         * PostgreSQL - https://jdbc.postgresql.org/documentation/94/query.html
         *
         * - java.sql.Connection#autocommit false
         * - java.sql.ResultSet.TYPE_FORWARD_ONLY
         */
        session.conn.setAutoCommit(false)

      case _ =>
    }

    session
  }
}

object CursorStreamingSQL {

  def apply[A, E <: WithExtractor](sql: SQL[A, E], fetchSize: Int): CursorStreamingSQL[A, E] = {
    val underlying = {
      (new SQL[A, E](sql.statement, sql.rawParameters)(sql.extractor) {})
        .fetchSize(fetchSize)
    }
    new CursorStreamingSQL(underlying)
  }

}
