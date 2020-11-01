package scalikejdbc

import org.mockito.Mockito.{ mock, when }

import java.sql._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ActiveSessionSpec extends AnyFlatSpec with Matchers {

  behavior of "ActiveSession"

  it should "be available without tx" in {
    val conn: Connection = mock(classOf[Connection])
    val tx: Option[Tx] = None
    val isReadOnly: Boolean = false
    new ActiveSession(conn, DBConnectionAttributes(), tx, isReadOnly)
  }

  it should "be available with inactive tx" in {
    val conn: Connection = mock(classOf[Connection])
    val tx: Option[Tx] = Some(mock(classOf[Tx]))
    val isReadOnly: Boolean = false

    intercept[java.lang.IllegalStateException] {
      new ActiveSession(conn, DBConnectionAttributes(), tx, isReadOnly)
    }
  }

  it should "be available with active tx" in {
    val conn: Connection = mock(classOf[Connection])

    val tx = mock(classOf[Tx])
    when(tx.isActive()).thenReturn(true)
    val txOpt: Option[Tx] = Some(tx)

    val isReadOnly: Boolean = false

    new ActiveSession(conn, DBConnectionAttributes(), txOpt, isReadOnly)
  }

}
