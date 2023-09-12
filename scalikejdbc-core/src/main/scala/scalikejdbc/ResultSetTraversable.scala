package scalikejdbc

import java.sql.ResultSet
import scala.Iterable

/**
 * scala.collection.Traversable object which wraps java.sql.ResultSet.
 */
@deprecated(message = "use ResultSetIterator instead", since = "3.3.0")
class ResultSetTraversable(rs: ResultSet) extends Iterable[WrappedResultSet] with LoanPattern {

  private[this] val cursor: ResultSetCursor = new ResultSetCursor(0)

  /**
   * Applies a function.
   * @param f function
   * @tparam U type
   */
  override def foreach[U](f: (WrappedResultSet) => U): Unit = {
    using(rs) { rs =>
      while (rs.next()) {
        cursor.position += 1
        f.apply(new WrappedResultSet(rs, cursor, cursor.position))
      }
    }
  }

  def iterator: Iterator[WrappedResultSet] =
    new ResultSetIterator(rs)
}
