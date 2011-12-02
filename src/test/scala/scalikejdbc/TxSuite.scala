package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.sql._
import util.control.Exception._

@RunWith(classOf[JUnitRunner])
class TxSuite extends FunSuite with ShouldMatchers with Settings {

  type ? = this.type // for IntelliJ IDEA

  test("available") {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx should not be null
    }
  }

  test("begin") {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.rollbackIfActive()
    }
  }

  test("commit") {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.commit()
    }
  }

  test("rollback") {
    val tx = new Tx(ConnectionPool.borrow())
    ultimately {
      tx.conn.close()
    } apply {
      tx.begin()
      tx.rollback()
    }
  }

}
