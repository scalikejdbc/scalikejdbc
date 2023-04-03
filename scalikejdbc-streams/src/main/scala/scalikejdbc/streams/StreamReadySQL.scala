package scalikejdbc.streams

import scalikejdbc._
import StreamReadySQL._

/**
 * Streaming-ready SQL object.
 *
 * The primary constructor is intentionally hidden, use only StreamSQL object's apply method to instantiate.
 */
case class StreamReadySQL[A] private (
  private val underlying: SQL[A, HasExtractor],
  private val adjuster: DBSessionForceAdjuster = defaultDBSessionForceAdjuster
) {

  private[streams] lazy val extractor: WrappedResultSet => A =
    underlying.extractor

  private[streams] def statement: String = underlying.statement
  private[streams] def rawParameters: collection.Seq[Any] =
    underlying.rawParameters
  private[streams] def parameters: collection.Seq[Any] = underlying.parameters

  private[streams] lazy val fetchSize: Option[Int] = underlying.fetchSize
  private[streams] lazy val tags: collection.Seq[String] = underlying.tags

  private[streams] lazy val queryTimeout: Option[Int] = underlying.queryTimeout

  /**
   * New StreamReadySQL with adjuster of DBSession attributes.
   *
   * @param adjuster The Function to adjust the db session before querying.
   */
  final def withDBSessionForceAdjuster(
    adjuster: DBSessionForceAdjuster
  ): StreamReadySQL[A] = {
    new StreamReadySQL(underlying, adjuster)
  }

  private[streams] def createDBSessionAttributesSwitcher
    : DBSessionAttributesSwitcher = {
    new DBSessionAttributesSwitcher(underlying) {
      override def withSwitchedDBSession[T](
        session: DBSession
      )(op: DBSession => T): T = {
        super.withSwitchedDBSession(session) { session =>
          adjuster(session)
          op(session)
        }
      }
    }
  }
}

private[streams] object StreamReadySQL {
  type DBSessionForceAdjuster = DBSession => Unit

  /**
   * The only way to instantiate StreamSQL.
   */
  private[streams] def apply[A, E <: WithExtractor](
    sql: SQL[A, E],
    fetchSize: Int
  ): StreamReadySQL[A] = {
    val underlying: SQL[A, HasExtractor] = {
      (new SQL[A, HasExtractor](sql.statement, sql.rawParameters)(
        sql.extractor
      ) {})
        .fetchSize(fetchSize)
    }
    new StreamReadySQL(underlying)
  }

  /**
   * Forcibly changes the database session to be cursor query ready.
   */
  val defaultDBSessionForceAdjuster: DBSessionForceAdjuster = session => {

    // setup required settings to enable cursor operations
    session.connectionAttributes.driverName match {
      case Some(driver)
        if driver == "com.mysql.jdbc.Driver" && session.fetchSize.exists(
          _ > 0
        ) =>
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
  }

}
