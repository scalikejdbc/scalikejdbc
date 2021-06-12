package scalikejdbc

import java.sql.Connection._
import java.sql.{ Connection, SQLException }

import scala.util.control.Exception._
import scala.util.control.NonFatal

/**
 * DB Transaction abstraction.
 * @param conn connection
 */
class Tx(
  val conn: Connection,
  isolationLevel: IsolationLevel = IsolationLevel.Default
) {

  private[this] def setTransactionIsolation(): Unit = {
    // Set isolation level for the transaction
    isolationLevel match {
      case IsolationLevel.Serializable =>
        conn.setTransactionIsolation(TRANSACTION_SERIALIZABLE)
      case IsolationLevel.RepeatableRead =>
        conn.setTransactionIsolation(TRANSACTION_REPEATABLE_READ)
      case IsolationLevel.ReadCommitted =>
        conn.setTransactionIsolation(TRANSACTION_READ_COMMITTED)
      case IsolationLevel.ReadUncommitted =>
        conn.setTransactionIsolation(TRANSACTION_READ_UNCOMMITTED)
      case IsolationLevel.Default =>
      // Do nothing
    }
  }

  /**
   * Begins this transaction.
   */
  def begin(): Unit = {
    setTransactionIsolation()
    conn.setAutoCommit(false)
    if (!GlobalSettings.jtaDataSourceCompatible) {
      conn.setReadOnly(false)
    }
  }

  /**
   * Commits this transaction.
   */
  def commit(): Unit = {
    try conn.commit()
    catch {
      case NonFatal(e) =>
        try conn.rollback()
        catch {
          case NonFatal(e2) => e.addSuppressed(e2)
        }
        throw e
    }
    conn.setAutoCommit(true)
  }

  /**
   * Returns is this transaction active.
   * Since connections from JTA managed DataSource should not be used as-is, we don't care autoCommit/readOnly props.
   * @return active
   */
  def isActive(): Boolean =
    GlobalSettings.jtaDataSourceCompatible || !conn.getAutoCommit

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
