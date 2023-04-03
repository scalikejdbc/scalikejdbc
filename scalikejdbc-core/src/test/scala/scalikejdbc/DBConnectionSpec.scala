package scalikejdbc

import java.sql.Connection
import org.mockito.Mockito.{ mock, verify, times, when }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DBConnectionSpec extends AnyFlatSpec with Matchers {

  behavior of "DBConnection"

  "#begin" should "simply work with mocked java.sql.Connection objects" in {
    val mockConn = mock(classOf[java.sql.Connection])
    when(mockConn.setAutoCommit(false))
      .thenThrow(new IllegalStateException("Failed to start a transaction"))
    val conn = new DBConnection {
      override protected[this] val settingsProvider: SettingsProvider =
        SettingsProvider.default
      override def conn: Connection = mockConn
    }
    try {
      conn.begin()
      fail()
    } catch {
      case _: IllegalStateException =>
    }
    verify(mockConn, times(1)).getAutoCommit()
    verify(mockConn, times(1)).isReadOnly()
    verify(mockConn, times(1)).isClosed()
    verify(mockConn, times(1)).setReadOnly(false)
  }

  it should "close the resource when an exception is thrown in #begin" in {
    val mockConn = mock(classOf[java.sql.Connection])
    when(mockConn.setAutoCommit(false))
      .thenThrow(new IllegalStateException("Failed to start a transaction"))
    val conn = new DBConnection {
      override protected[this] val settingsProvider: SettingsProvider =
        SettingsProvider.default
      override def conn: Connection = mockConn
    }
    try {
      conn.localTx { implicit session =>
        // supposed to do something here
      }
      fail()
    } catch {
      case _: IllegalStateException =>
    }
    verify(mockConn, times(1)).getAutoCommit()
    verify(mockConn, times(1)).isReadOnly()
    verify(mockConn, times(1)).isClosed()
    verify(mockConn, times(1)).setReadOnly(false)

    // issue #977 Issue with open connection after exception in begin(tx) method
    verify(mockConn, times(1)).close()
  }

}
