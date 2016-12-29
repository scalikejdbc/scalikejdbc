package scalikejdbc.streams

import java.sql.ResultSet

import scalikejdbc.{ ResultSetCursor, WrappedResultSet }

abstract class ExtractedResultIterator[A](rs: ResultSet, autoClose: Boolean)(extract: WrappedResultSet => A) extends BufferedIterator[A] with CloseableIterator[A] {
  private[this] var state = 0 // 0: no data, 1: cached, 2: finished
  private[this] var cached: A = null.asInstanceOf[A]

  protected[this] final def finished(): A = {
    state = 2
    null.asInstanceOf[A]
  }

  def head: A = {
    update()
    if (state == 1) cached
    else throw new NoSuchElementException("head on empty iterator")
  }

  def headOption: Option[A] = {
    update()
    if (state == 1) Some(cached)
    else None
  }

  private[this] def update() {
    if (state == 0) {
      cached = fetchNext()
      if (state == 0) state = 1
    }
  }

  def hasNext: Boolean = {
    update()
    state == 1
  }

  def next(): A = {
    update()
    if (state == 1) {
      state = 0
      cached
    } else throw new NoSuchElementException("next on empty iterator");
  }

  private[this] val cursor: ResultSetCursor = new ResultSetCursor(0)

  protected def fetchNext(): A = {
    if (rs.next()) {
      cursor.position += 1
      val res = extract(WrappedResultSet(rs, cursor, cursor.position))
      res
    } else {
      if (autoClose) close()
      finished()
    }
  }

}
