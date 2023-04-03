package scalikejdbc.streams

import java.io.Closeable
import java.sql.ResultSet
import scalikejdbc.{ LogSupport, ResultSetCursor, WrappedResultSet }
import scala.util.control.NonFatal
import scala.collection.BufferedIterator
import scalikejdbc.streams.StreamResultSetIterator._

/**
 * An iterator which handles JDBC ResultSet in the fashion of Reactive Streams.
 */
private[streams] class StreamResultSetIterator[+A](
  rs: ResultSet,
  extractor: WrappedResultSet => A,
  autoClose: Boolean = true
) extends BufferedIterator[A]
  with Closeable
  with LogSupport { self =>

  private[this] var internalState: InternalState = NeedToPrefetchNextValue
  private[this] var fetchedNextValue: Option[A] = None
  private[this] val cursor: ResultSetCursor = new ResultSetCursor(0)

  // ------------------------------------
  // Iterator APIs
  // ------------------------------------

  override def head: A = {
    tryFetchingNextIfNeeded()
    (internalState, fetchedNextValue) match {
      case (NextInvocationReady, Some(nextValue)) => nextValue
      case _ => throw new NoSuchElementException("head on empty iterator")
    }
  }

  override def hasNext: Boolean = {
    tryFetchingNextIfNeeded()
    internalState == NextInvocationReady
  }

  override def next(): A = {
    tryFetchingNextIfNeeded()
    try {
      (internalState, fetchedNextValue) match {
        case (NextInvocationReady, Some(nextValue)) => nextValue
        case _ => throw new NoSuchElementException("head on empty iterator")
      }
    } finally {
      internalState = NeedToPrefetchNextValue
    }
  }

  // ------------------------------------
  // Closeable APIs
  // ------------------------------------

  override def close(): Unit = {
    self.close()
    try {
      rs.close()
    } catch {
      case NonFatal(e) =>
        if (log.isDebugEnabled) {
          log.debug(s"Failed to close ResultSet because ${e.getMessage}", e)
        }
    }
  }

  // ------------------------------------
  // Internal APIs
  // ------------------------------------

  private[this] def tryFetchingNextIfNeeded(): Unit = {
    internalState match {
      case NeedToPrefetchNextValue =>
        fetchNext() match {
          case None =>
            internalState = AlreadyConsumed
            fetchedNextValue = None
          case nextValue =>
            internalState = NextInvocationReady
            fetchedNextValue = nextValue
        }
      case _ =>
    }
  }

  private[this] def fetchNext(): Option[A] = {
    if (rs.next()) {
      cursor.position += 1
      // NOTE: intentionally allowing the possibility of Some(null) here
      Some(extractor.apply(WrappedResultSet(rs, cursor, cursor.position)))
    } else {
      if (autoClose) {
        close()
      }
      None
    }
  }

}

private[streams] object StreamResultSetIterator {

  private sealed trait InternalState
  private case object NeedToPrefetchNextValue extends InternalState
  private case object NextInvocationReady extends InternalState
  private case object AlreadyConsumed extends InternalState

}
