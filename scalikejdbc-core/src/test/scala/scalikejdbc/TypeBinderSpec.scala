package scalikejdbc

import org.scalatest._
import org.mockito.Mockito._
import java.sql.ResultSet

import org.joda.time._
import org.scalatest.mockito.MockitoSugar

class TypeBinderSpec extends FlatSpec with Matchers with MockitoSugar with UnixTimeInMillisConverterImplicits {

  behavior of "TypeBinder"

  it should "be able to return an Option value when NPE" in {
    val result: Option[String] = implicitly[TypeBinder[Option[String]]].read(null, 1)
    result should be(None)
  }

  it should "have TypeBinder for scala.BigDecimal" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getBigDecimal("decimal")).thenReturn(new java.math.BigDecimal("1234567"))
    when(rs.getBigDecimal(1)).thenReturn(new java.math.BigDecimal("2345678"))

    implicitly[TypeBinder[BigDecimal]].read(rs, "decimal") should be(BigDecimal("1234567"))
    implicitly[TypeBinder[BigDecimal]].read(rs, 1) should be(BigDecimal("2345678"))

    implicitly[TypeBinder[Option[BigDecimal]]].read(rs, "decimal") should be(Some(BigDecimal("1234567")))
    implicitly[TypeBinder[Option[BigDecimal]]].read(rs, 1) should be(Some(BigDecimal("2345678")))
    implicitly[TypeBinder[Option[BigDecimal]]].read(rs, "none") should be(None)
    implicitly[TypeBinder[Option[BigDecimal]]].read(rs, 2) should be(None)
  }

  it should "have TypeBinder for scala.BigInt" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getBigDecimal("integer")).thenReturn(new java.math.BigDecimal("1234567"))
    when(rs.getBigDecimal(1)).thenReturn(new java.math.BigDecimal("2345678"))

    implicitly[TypeBinder[BigInt]].read(rs, "integer") should be(BigInt("1234567"))
    implicitly[TypeBinder[BigInt]].read(rs, 1) should be(BigInt("2345678"))

    implicitly[TypeBinder[Option[BigInt]]].read(rs, "integer") should be(Some(BigInt("1234567")))
    implicitly[TypeBinder[Option[BigInt]]].read(rs, 1) should be(Some(BigInt("2345678")))
    implicitly[TypeBinder[Option[BigInt]]].read(rs, "none") should be(None)
    implicitly[TypeBinder[Option[BigInt]]].read(rs, 2) should be(None)
  }

  it should "have TypeBinder for java.util.Date/Calendar" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getTimestamp("time")).thenReturn(new java.sql.Timestamp(DateTime.now.getMillis))
    when(rs.getDate("date")).thenReturn(new java.sql.Date(DateTime.now.getMillis))

    implicitly[TypeBinder[java.sql.Timestamp]].read(rs, "time") should not be (null)
    implicitly[TypeBinder[DateTime]].read(rs, "time") should not be (null)
    implicitly[TypeBinder[LocalDate]].read(rs, "date") should not be (null)

    //implicitly[TypeBinder[java.util.Date]].apply(rs, "time") should not be (null)
    implicitly[TypeBinder[java.sql.Timestamp]].read(rs, "time").toJavaUtilDate should not be (null)

    implicitly[TypeBinder[java.util.Calendar]].read(rs, "time") should not be (null)
  }

  it should "handle result values of type java.math.BigDecimal" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getObject("zero")).thenReturn(java.math.BigDecimal.ZERO, Array[Object](): _*)
    when(rs.getObject("nonzero")).thenReturn(java.math.BigDecimal.ONE, Array[Object](): _*)

    val wrapped = WrappedResultSet(rs, new ResultSetCursor(0), 0)
    wrapped.boolean("zero") should be(false)
    wrapped.boolean("nonzero") should be(true)
    wrapped.booleanOpt("zero") should be(Some(false))
    wrapped.booleanOpt("nonzero") should be(Some(true))
    wrapped.int("zero") should be(0)
    wrapped.int("nonzero") should be(1)
    wrapped.intOpt("zero") should be(Some(0))
    wrapped.intOpt("nonzero") should be(Some(1))
    wrapped.long("zero") should be(0)
    wrapped.long("nonzero") should be(1L)
    wrapped.longOpt("zero") should be(Some(0L))
    wrapped.longOpt("nonzero") should be(Some(1L))
    wrapped.short("zero") should be(0)
    wrapped.short("nonzero") should be(1)
    wrapped.shortOpt("zero") should be(Some(0))
    wrapped.shortOpt("nonzero") should be(Some(1))
  }

  it should "handle result values of type java.math.BigInt" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getObject("zero")).thenReturn(java.math.BigInteger.ZERO, Array[Object](): _*)
    when(rs.getObject("nonzero")).thenReturn(java.math.BigInteger.ONE, Array[Object](): _*)

    val wrapped = WrappedResultSet(rs, new ResultSetCursor(0), 0)
    wrapped.boolean("zero") should be(false)
    wrapped.boolean("nonzero") should be(true)
    wrapped.booleanOpt("zero") should be(Some(false))
    wrapped.booleanOpt("nonzero") should be(Some(true))
    wrapped.int("zero") should be(0)
    wrapped.int("nonzero") should be(1)
    wrapped.intOpt("zero") should be(Some(0))
    wrapped.intOpt("nonzero") should be(Some(1))
    wrapped.long("zero") should be(0)
    wrapped.long("nonzero") should be(1L)
    wrapped.longOpt("zero") should be(Some(0L))
    wrapped.longOpt("nonzero") should be(Some(1L))
    wrapped.short("zero") should be(0)
    wrapped.short("nonzero") should be(1)
    wrapped.shortOpt("zero") should be(Some(0))
    wrapped.shortOpt("nonzero") should be(Some(1))
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
    implicitly[TypeBinder[Option[Boolean]]].read(rs, "none") should be(None)
    implicitly[TypeBinder[Option[Int]]].read(rs, "none") should be(None)
    implicitly[TypeBinder[Option[Long]]].read(rs, "none") should be(None)
    implicitly[TypeBinder[Option[String]]].read(rs, "none") should be(None)
  }

  it should "deal with optional values" in {
    val rs: ResultSet = mock[ResultSet]

    implicitly[TypeBinder[Option[java.sql.Array]]].read(rs, 1) should be(None)
//    implicitly[TypeBinder[Option[java.io.InputStream]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[BigDecimal]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Blob]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Boolean]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Byte]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Array[Byte]]]].read(rs, 1) should be(None)
//    implicitly[TypeBinder[Option[java.io.Reader]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Clob]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Date]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Double]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Float]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Int]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Long]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.NClob]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[String]]].read(rs, 1) should be(None)
    //implicitly[TypeBinder[Option[Any]]].read(rs, 1) should be(None)
    //implicitly[TypeBinder[Option[AnyRef]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Ref]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.RowId]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Short]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.SQLXML]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Time]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Time]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Timestamp]]].read(rs, 1) should be(None)
    implicitly[TypeBinder[Option[DateTime]]].read(rs, 1) should be(None)
  }

}
