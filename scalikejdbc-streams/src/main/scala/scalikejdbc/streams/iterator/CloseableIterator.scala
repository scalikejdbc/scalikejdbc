package scalikejdbc.streams.iterator

import java.io.Closeable

/**
 * Closeable Iterator
 */
trait CloseableIterator[+A]
    extends Iterator[A]
    with Closeable { self =>

  override def map[B](f: A => B): CloseableIterator[B] = new CloseableIterator[B] {
    def hasNext: Boolean = self.hasNext
    def next(): B = f(self.next())
    def close(): Unit = self.close()
  }

  override def close(): Unit

}

object CloseableIterator {

  def empty[A]: CloseableIterator[A] = {
    new CloseableIterator[A] {

      override def next(): A = throw new NoSuchElementException("next on empty iterator")

      override def hasNext: Boolean = false

      override def close(): Unit = ()
    }
  }

}
