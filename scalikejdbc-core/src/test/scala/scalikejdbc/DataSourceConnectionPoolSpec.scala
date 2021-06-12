package scalikejdbc

import javax.sql.DataSource

import org.mockito.Mockito.mock
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DataSourceConnectionPoolSpec extends AnyFlatSpec with Matchers {

  case class DummyDataSourceCloser() extends DataSourceCloser {
    var closed = false
    override def close(): Unit = closed = true
  }

  behavior of "DataSourceConnectionPool with DataSourceCloser"

  it should "be close" in {
    val dataSource: DataSource = mock(classOf[DataSource])
    val dataSourceCloser = DummyDataSourceCloser()
    val instance =
      new DataSourceConnectionPool(dataSource, closer = dataSourceCloser)
    ConnectionPool.add("close", instance)
    Thread.sleep(100L)
    ConnectionPool.close("close")
    dataSourceCloser.closed shouldBe true
  }

  it should "be impossible to close with DefaultDataSourceCloser" in {
    val dataSource: DataSource = mock(classOf[DataSource])
    val instance = new DataSourceConnectionPool(dataSource)
    ConnectionPool.add("close", instance)
    Thread.sleep(100L)
    assertThrows[UnsupportedOperationException] {
      ConnectionPool.close("close")
    }
  }

}
