package scalikejdbc

import java.sql.ResultSet

/**
 * scala.collection.Iterator object which wraps java.sql.ResultSet.
 */
class ResultSetIterator(rs: ResultSet) extends Iterator[WrappedResultSet] {

  private[this] val cursor: ResultSetCursor = new ResultSetCursor(0)
  private[this] var nextOpt: WrappedResultSet = null
  private[this] var closed: Boolean = false

  override def hasNext: Boolean = {
    if (nextOpt != null) {
      true
    } else if (closed) {
      false
    } else if (rs.next) {
      cursor.position += 1
      nextOpt = new WrappedResultSet(rs, cursor, cursor.position)
      true
    } else {
      rs.close()
      closed = true
      false
    }
  }

  override def next(): WrappedResultSet = {
    if (nextOpt != null) {
      val result = nextOpt
      nextOpt = null
      result
    } else if (closed) {
      Iterator.empty.next()
    } else if (rs.next) {
      cursor.position += 1
      new WrappedResultSet(rs, cursor, cursor.position)
    } else {
      rs.close()
      closed = true
      Iterator.empty.next()
    }
  }
}
