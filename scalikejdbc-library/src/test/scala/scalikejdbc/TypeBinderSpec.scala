package scalikejdbc

import org.scalatest._
import mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.matchers._
import java.sql.ResultSet

class TypeBinderSpec extends FlatSpec with ShouldMatchers with MockitoSugar {

  behavior of "TypeBinder"

  it should "be able to return an Option value when NPE" in {
    val result: Option[String] = implicitly[TypeBinder[Option[String]]].apply(null, 1)
    result should be(None)
  }

  it should "have TypeBinder for scala.BigDecimal" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getBigDecimal("decimal")).thenReturn(new java.math.BigDecimal("1234567"))
    implicitly[TypeBinder[BigDecimal]].apply(rs, "decimal") should be(BigDecimal("1234567"))
  }

  it should "fix issue #170" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getObject("none")).thenReturn(null, Array[Object](): _*)

    // WrappedResultSet works fine
    val wrapped = WrappedResultSet(rs, new ResultSetCursor(0), 0)
    wrapped.booleanOpt("none") should be(None)
    wrapped.intOpt("none") should be(None)
    wrapped.longOpt("none") should be(None)

    // using TypeBinder implicitly doesn't work as expected
    implicitly[TypeBinder[Option[Boolean]]].apply(rs, "none") should be(None)
    implicitly[TypeBinder[Option[Int]]].apply(rs, "none") should be(None)
    implicitly[TypeBinder[Option[Long]]].apply(rs, "none") should be(None)
    implicitly[TypeBinder[Option[String]]].apply(rs, "none") should be(None)
  }

}
