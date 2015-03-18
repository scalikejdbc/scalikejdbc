package scalikejdbc

import java.sql.{ SQLException, Connection }
import scala.util.control.Exception._

/**
 * DB Transaction abstraction.
 * @param conn connection
 */
class Tx(val conn: Connection) {

  /**
   * Begins this transaction.
   */
  def begin(): Unit = {
    conn.setAutoCommit(false)
    if (!GlobalSettings.jtaDataSourceCompatible) {
      conn.setReadOnly(false)
    }
  }

  /**
   * Commits this transaction.
   */
  def commit(): Unit = {
    conn.commit()
    conn.setAutoCommit(true)
  }

  /**
   * Returns is this transaction active.
   * Since connections from JTA managed DataSource should not be used as-is, we don't care autoCommit/readOnly props.
   * @return active
   */
  def isActive(): Boolean = GlobalSettings.jtaDataSourceCompatible || !conn.getAutoCommit

  /**
   * Rolls this transaction back.
   */
  def rollback(): Unit = {
    conn.rollback()
    ignoring(classOf[SQLException]) apply {
      conn.setAutoCommit(true)
    }
  }

  /**
   * Rolls this transaction back if this transaction is still active.
   */
  def rollbackIfActive(): Unit = {
    ignoring(classOf[SQLException]) apply {
      conn.rollback()
    }
    ignoring(classOf[SQLException]) apply {
      conn.setAutoCommit(true)
    }
  }

}

