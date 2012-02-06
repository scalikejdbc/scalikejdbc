package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import util.control.Exception._

@RunWith(classOf[JUnitRunner])
class TxSpec extends FlatSpec with ShouldMatchers with Settings {

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
