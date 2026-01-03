package scalikejdbc

import util.DynamicVariable
import java.sql.Connection

/**
 * Thread-local DB.
 */
object ThreadLocalDB {

  private[this] val _db = new DynamicVariable[DB](null)

  /**
   * Creates a new DB instance for the current thread.
   */
  def create(conn: Connection): DB = {
    _db.value = DB(conn, DBConnectionAttributes(), SettingsProvider.default)
    _db.value
  }

  /**
   * Returns the DB instance for this thread. It's nullable.
   * @return DB instance
   */
  def load(): DB = _db.value

}
