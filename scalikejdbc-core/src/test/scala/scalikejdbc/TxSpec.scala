package scalikejdbc

import java.sql.Connection

import org.mockito.Mockito.{ mock, times, verify, when }

import scala.util.control.Exception._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TxSpec extends AnyFlatSpec with Matchers with Settings {

  behavior of "Tx"

  it should "be available" in {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx should not be null
    }
  }

  "begin" should "be available" in {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.rollbackIfActive()
    }
  }

  "commit" should "be available" in {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.commit()
    }
  }

  it should "be rolled back when it throws an exception" in {
    val conn = mock(classOf[Connection])
    val commitException = new RuntimeException
    when(conn.commit()).thenThrow(commitException)

    val tx = new Tx(conn)
    the[RuntimeException] thrownBy tx.commit() should be(commitException)
    verify(conn, times(1)).rollback()
  }

  it should "have a suppressed exception when rollback() throws an exception" in {
    val conn = mock(classOf[Connection])
    val commitException = new RuntimeException
    when(conn.commit()).thenThrow(commitException)
    val rollbackException = new RuntimeException
    when(conn.rollback()).thenThrow(rollbackException)

    val tx = new Tx(conn)
    the[RuntimeException] thrownBy tx.commit() should be(commitException)
    commitException.getSuppressed should contain(rollbackException)
  }

  "rollback" should "be available" in {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.rollback()
    }
  }

}
