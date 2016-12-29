package scalikejdbc.streams

import java.io.Closeable

trait CloseableIterator[A] extends Iterator[A] with Closeable {
  self =>

  override def close(): Unit

  override def map[B](f: A => B): CloseableIterator[B] = new CloseableIterator[B] {
    def hasNext: Boolean = self.hasNext
    def next(): B = f(self.next())
    def close(): Unit = self.close()
  }
}

object CloseableIterator {

  def empty[A]: CloseableIterator[A] = {
    new CloseableIterator[A] {
      override def close(): Unit = ()

      override def next(): A = throw new IllegalStateException("an empty CloseableIterator.")

      override def hasNext: Boolean = false
    }
  }
}
