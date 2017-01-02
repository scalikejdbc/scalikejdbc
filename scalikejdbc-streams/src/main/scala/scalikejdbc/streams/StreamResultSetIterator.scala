package scalikejdbc.streams

import java.io.Closeable
import java.sql.ResultSet

import scalikejdbc.{ ResultSetCursor, WrappedResultSet }

/**
 * An iterator which handles JDBC ResultSet in the fashion of Reactive Streams.
 */
private[streams] class StreamResultSetIterator[+A](
    rs: ResultSet,
    extractor: WrappedResultSet => A,
    autoClose: Boolean = true
) extends BufferedIterator[A] with Closeable { self =>

  private[this] var state = 0 // 0: no data, 1: cached, 2: finished
  private[this] var preFetchedNextValue: A = null.asInstanceOf[A]
  private[this] val cursor: ResultSetCursor = new ResultSetCursor(0)

  // ------------------------------------
  // Iterator APIs
  // ------------------------------------

  override def head: A = {
    update()
    if (state == 1) preFetchedNextValue
    else throw new NoSuchElementException("head on empty iterator")
  }

  override def hasNext: Boolean = {
    update()
    state == 1
  }

  override def next(): A = {
    update()
    if (state == 1) {
      state = 0
      preFetchedNextValue
    } else throw new NoSuchElementException("next on empty iterator")
  }

  // ------------------------------------
  // Closeable APIs
  // ------------------------------------

  override def close(): Unit = {
    self.close()
  }

  // ------------------------------------
  // Internal APIs
  // ------------------------------------

  private[this] def markedAsFinishedAndReturnNullValue(): A = {
    state = 2
    null.asInstanceOf[A]
  }

  private[this] def update(): Unit = {
    if (state == 0) {
      preFetchedNextValue = fetchNext()
      if (state == 0) state = 1
    }
  }

  private[this] def fetchNext(): A = {
    if (rs.next()) {
      cursor.position += 1
      extractor.apply(WrappedResultSet(rs, cursor, cursor.position))
    } else {
      if (autoClose) {
        close()
      }
      markedAsFinishedAndReturnNullValue()
    }
  }

}