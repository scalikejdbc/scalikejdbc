package scalikejdbc.streams

import scalikejdbc.DBSession

/**
 * The ability to modify DB session when before running query.
 */
trait SessionModification {

  /**
   * Modify DB session.
   *
   * SHOULD return the same session instance.
   *
   * @param session db session
   * @return db session
   */
  def modify(session: DBSession): session.type
}

object SessionModification {

  val none: SessionModification = new SessionModification {
    override def modify(session: DBSession): session.type = session
  }

  val default: SessionModification = new SessionModification {
    override def modify(session: DBSession): session.type = {

      // setup required settings to enable cursor operations
      session.connectionAttributes.driverName match {
        case Some(driver) if driver == "com.mysql.jdbc.Driver" && session.fetchSize.exists(_ > 0) =>
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
}
