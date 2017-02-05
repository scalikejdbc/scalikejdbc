package scalikejdbc

import java.sql.ResultSet

import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{ FlatSpec, Matchers }

class ScalaBigDecimalConverterImplicitsSpec extends FlatSpec with Matchers with MockitoSugar with ScalaBigDecimalConverterImplicits {

  behavior of "ScalaBigDecimalConverterImplicits"

  case class obj(value: Option[scala.math.BigDecimal])

  object obj {
    def apply(rs: WrappedResultSet): obj = {
      obj(rs.bigDecimalOpt("one").toScalaBigDecimalOpt)
    }
  }

  it should "Convert to a scala BigDecimal Option if the column contains a value" in {
    val underlying: ResultSet = mock[ResultSet]
    when(underlying.getBigDecimal("one")).thenReturn(new java.math.BigDecimal(0))

    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val rs = new WrappedResultSet(underlying, cursor, cursor.position)

    obj(rs).value shouldEqual (Some(scala.math.BigDecimal(0)))
  }

  it should "Return None if the column does not contain a value" in {
    val underlying: ResultSet = mock[ResultSet]
    when(underlying.getBigDecimal("one")).thenReturn(null)

    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val rs = new WrappedResultSet(underlying, cursor, cursor.position)

    obj(rs).value shouldEqual (None)
  }
}
