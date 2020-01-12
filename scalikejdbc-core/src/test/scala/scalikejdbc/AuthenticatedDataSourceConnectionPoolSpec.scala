package scalikejdbc

import javax.sql.DataSource

import org.mockito.Mockito.mock
import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AuthenticatedDataSourceConnectionPoolSpec extends AnyFlatSpec with Matchers {

  case class DummyDataSourceCloser() extends DataSourceCloser {
    var closed = false
    override def close(): Unit = closed = true
  }

  behavior of "AuthenticatedDataSourceConnectionPool with DataSourceCloser"

  it should "be close" in {
    val dataSource: DataSource = mock(classOf[DataSource])
    val dataSourceCloser = DummyDataSourceCloser()
    val instance = new AuthenticatedDataSourceConnectionPool(dataSource, "user", "password ", closer = dataSourceCloser)
    ConnectionPool.add(Symbol("close"), instance)
    Thread.sleep(100L)
    ConnectionPool.close(Symbol("close"))
    dataSourceCloser.closed shouldBe true
  }

  it should "be impossible to close with DefaultDataSourceCloser" in {
    val dataSource: DataSource = mock(classOf[DataSource])
    val instance = new AuthenticatedDataSourceConnectionPool(dataSource, "user", "password")
    ConnectionPool.add(Symbol("close"), instance)
    Thread.sleep(100L)
    assertThrows[UnsupportedOperationException] {
      ConnectionPool.close(Symbol("close"))
    }
  }

}
