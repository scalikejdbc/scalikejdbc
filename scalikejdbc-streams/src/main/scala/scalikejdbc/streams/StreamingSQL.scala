package scalikejdbc.streams

import scalikejdbc.{ DBSession, SQL, WithExtractor, WrappedResultSet }

trait StreamingSQL[A, E <: WithExtractor] {
  private[streams] def underlying: SQL[A, E]

  def statement: String

  def parameters: Seq[Any]

  def extractor: WrappedResultSet => A

  protected[streams] def setSessionAttributes(session: DBSession): DBSession = {
    session
      .fetchSize(underlying.fetchSize)
      .tags(underlying.tags: _*)
      .queryTimeout(underlying.queryTimeout)
  }
}

final class CursorStreamingSQL[A, E <: WithExtractor](s: SQL[A, E], fetchSize: Int) extends StreamingSQL[A, E] {
  // clone the SQL instance so that it is not changed
  private[streams] val underlying = (new SQL[A, E](s.statement, s.rawParameters)(s.extractor) {}).fetchSize(fetchSize)

  override def statement: String = underlying.statement

  override def parameters: Seq[Any] = underlying.parameters

  override def extractor: (WrappedResultSet) => A = underlying.extractor

  override protected[streams] def setSessionAttributes(session: DBSession): DBSession = {
    val s = super.setSessionAttributes(session)
    session.connectionAttributes.driverName match {
      case Some(driver) if driver == "com.mysql.jdbc.Driver" && fetchSize > 0 =>
        // To enable CURSOR (Streaming) in MySQL it is necessary to set the fetchSize to Int.MinValue
        // https://dev.mysql.com/doc/connector-j/5.1/en/connector-j-reference-implementation-notes.html
        // The ResultSet type of the PreparedStatement must also be java.sql.ResultSet.TYPE_FORWARD_ONLY and java.sql.ResultSet.CONCUR_READ_ONLY,
        // but this is implicitly granted when generating a StatementExecutor on the StreamingInvoker.
        s.fetchSize(Int.MinValue)
      case Some(driver) if driver == "org.postgresql.Driver" =>
        // To enable CURSOR (Streaming) in PostgreSQL it is necessary to set the autocommit mode to OFF
        // Changing autocommit mode directly inside scalikejdbc.DBConnection is only when the client explicitly uses DBConnection#autoCommitSession.
        // By default in PostgreSQL driver, autocommit mode is ON, so it is necessary to set Connection here.
        // https://jdbc.postgresql.org/documentation/94/query.html
        // The ResultSet type of the PreparedStatement must also be java.sql.ResultSet.TYPE_FORWARD_ONLY,
        // but this is implicitly granted when generating a StatementExecutor on the StreamingInvoker.
        s.conn.setAutoCommit(false)
        s
      case _ =>
        s
    }
  }
}
