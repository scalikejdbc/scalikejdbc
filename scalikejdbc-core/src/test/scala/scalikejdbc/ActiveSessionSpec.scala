package scalikejdbc

import org.scalatest._
import org.mockito.Mockito.{ mock, when }

import java.sql._

class ActiveSessionSpec extends FlatSpec with Matchers {

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
    when(tx.isActive).thenReturn(true)
    val txOpt: Option[Tx] = Some(tx)

    val isReadOnly: Boolean = false

    new ActiveSession(conn, DBConnectionAttributes(), txOpt, isReadOnly)
  }

}
