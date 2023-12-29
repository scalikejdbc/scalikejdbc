package scalikejdbc

import org.mockito.Mockito._
import java.sql.ResultSet
import java.util.Calendar
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class WrappedResultSetSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "WrappedResultSet"

  it should "be available" in {
    val underlying: ResultSet = null
    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val instance = new WrappedResultSet(underlying, cursor, cursor.position)
    instance should not be null
  }

  // [NOTICE]
  // most of test cases at scalikejdbc.DBSessionSpec

  it should "have expected methods" in {

    import java.sql.{
      Array => sqlArray,
      Blob,
      Clob,
      NClob,
      Ref,
      SQLXML,
      Time,
      Timestamp
    }
    import java.io.InputStream
    import java.io.Reader
    import java.math.{ BigDecimal, BigInteger }
    import java.util.Date
    import java.net.URL

    val underlying: ResultSet = mock[ResultSet]
    val (one: AnyRef, zero: AnyRef, minusOne: AnyRef) =
      (1: Integer, 0: Integer, -1: Integer)
    when(underlying.getObject("one")).thenReturn(one, Array[Object]()*)
    // TODO this code doesn't work as expected, should I use ScalaMock?
    // when(underlying.getObject("zero")).thenReturn(zero, Array[Object](): _*)
    when(underlying.getObject("zero")).thenReturn("0", Array[Object]()*)
    when(underlying.getObject("minusOne"))
      .thenReturn(minusOne, Array[Object]()*)
    when(underlying.getObject("str")).thenReturn("abc", Array[Object]()*)

    val cursor: ResultSetCursor = new ResultSetCursor(0)
    val rs = new WrappedResultSet(underlying, cursor, cursor.position)

    {
      val res1: sqlArray = rs.array("foo")
      val res2: sqlArray = rs.array(0)
      val res3: sqlArray = rs.arrayOpt("foo").orNull[sqlArray]
      val res4: sqlArray = rs.arrayOpt(0).orNull[sqlArray]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: InputStream = rs.asciiStream("foo")
      val res2: InputStream = rs.asciiStream(0)
      val res3: InputStream = rs.asciiStreamOpt("foo").orNull[InputStream]
      val res4: InputStream = rs.asciiStreamOpt(0).orNull[InputStream]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: BigDecimal = rs.bigDecimal("foo")
      val res2: BigDecimal = rs.bigDecimal(0)
      val res3: BigDecimal = rs.bigDecimalOpt("foo").orNull[BigDecimal]
      val res4: BigDecimal = rs.bigDecimalOpt(0).orNull[BigDecimal]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: BigInteger = Option(rs.bigDecimal("foo")) match {
        case Some(bd) => bd.toBigInteger
        case None     => null
      }
      val res2: BigInteger = Option(rs.bigDecimal(0)) match {
        case Some(bd) => bd.toBigInteger
        case None     => null
      }
      val res3: BigInteger = rs.bigDecimalOpt("foo") match {
        case Some(bd) => bd.toBigInteger
        case None     => null
      }
      val res4: BigInteger = rs.bigDecimalOpt(0) match {
        case Some(bd) => bd.toBigInteger
        case None     => null
      }
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: InputStream = rs.binaryStream("foo")
      val res2: InputStream = rs.binaryStream(0)
      val res3: InputStream = rs.binaryStreamOpt("foo").orNull[InputStream]
      val res4: InputStream = rs.binaryStreamOpt(0).orNull[InputStream]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Blob = rs.blob("foo")
      val res2: Blob = rs.blob(0)
      val res3: Blob = rs.blobOpt("foo").orNull[Blob]
      val res4: Blob = rs.blobOpt(0).orNull[Blob]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: java.lang.Boolean = rs.nullableBoolean("foo")
      val res2: java.lang.Boolean = rs.nullableBoolean(0)
      val res3: Option[scala.Boolean] = rs.booleanOpt("foo")
      val res4: Option[scala.Boolean] = rs.booleanOpt(0)
      when(underlying.getObject("boolean"))
        .thenReturn("true", Array[Object]()*)
      when(underlying.getObject(1)).thenReturn("false", Array[Object]()*)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)
      intercept[ResultSetExtractorException] { rs.boolean("foo") }
      intercept[ResultSetExtractorException] { rs.boolean(0) }

      rs.boolean("one") should be(true)
      rs.boolean("zero") should be(false)
      rs.boolean("minusOne") should be(true)
      rs.boolean("str") should be(true)
      rs.boolean("boolean") should be(true)
      rs.boolean(1) should be(false)
    }

    {
      val res1: java.lang.Byte = rs.nullableByte("foo")
      val res2: java.lang.Byte = rs.nullableByte(0)
      val res3: Option[scala.Byte] = rs.byteOpt("foo")
      val res4: Option[scala.Byte] = rs.byteOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)
      intercept[ResultSetExtractorException] { rs.byte("foo") }
      intercept[ResultSetExtractorException] { rs.byte(0) }
    }

    {
      val res1: scala.Array[scala.Byte] = rs.bytes("foo")
      val res2: scala.Array[scala.Byte] = rs.bytes(0)
      val res3: scala.Array[scala.Byte] =
        rs.bytesOpt("foo").orNull[scala.Array[scala.Byte]]
      val res4: scala.Array[scala.Byte] =
        rs.bytesOpt(0).orNull[scala.Array[scala.Byte]]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Reader = rs.characterStream("foo")
      val res2: Reader = rs.characterStream(0)
      val res3: Reader = rs.characterStreamOpt("foo").orNull[Reader]
      val res4: Reader = rs.characterStreamOpt(0).orNull[Reader]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Clob = rs.clob("foo")
      val res2: Clob = rs.clob(0)
      val res3: Clob = rs.clobOpt("foo").orNull[Clob]
      val res4: Clob = rs.clobOpt(0).orNull[Clob]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Date = rs.date("foo")
      val res2: Date = rs.date(0)
      val res3: Date = rs.date("foo", Calendar.getInstance())
      val res4: Date = rs.date(0, Calendar.getInstance())
      val res5: Date = rs.dateOpt("foo").orNull[Date]
      val res6: Date = rs.dateOpt(0).orNull[Date]
      val res7: Date = rs.dateOpt("foo", Calendar.getInstance()).orNull[Date]
      val res8: Date = rs.dateOpt(0, Calendar.getInstance()).orNull[Date]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
      res5 should be(null)
      res6 should be(null)
      res7 should be(null)
      res8 should be(null)
    }

    {
      val res1: java.lang.Double = rs.nullableDouble("foo")
      val res2: java.lang.Double = rs.nullableDouble(0)
      val res3: Option[scala.Double] = rs.doubleOpt("foo")
      val res4: Option[scala.Double] = rs.doubleOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)

      intercept[ResultSetExtractorException] { rs.double("foo") }
      intercept[ResultSetExtractorException] { rs.double(0) }
    }

    {
      val res1: java.lang.Float = rs.nullableFloat("foo")
      val res2: java.lang.Float = rs.nullableFloat(0)
      val res3: Option[scala.Float] = rs.floatOpt("foo")
      val res4: Option[scala.Float] = rs.floatOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)

      intercept[ResultSetExtractorException] { rs.float("foo") }
      intercept[ResultSetExtractorException] { rs.float(0) }
    }

    {
      val res1: java.lang.Integer = rs.nullableInt("foo")
      val res2: java.lang.Integer = rs.nullableInt(0)
      val res3: Option[scala.Int] = rs.intOpt("foo")
      val res4: Option[scala.Int] = rs.intOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)

      intercept[ResultSetExtractorException] { rs.int("foo") }
      intercept[ResultSetExtractorException] { rs.int(0) }
    }

    {
      val res1: java.lang.Long = rs.nullableLong("foo")
      val res2: java.lang.Long = rs.nullableLong(0)
      val res3: Option[scala.Long] = rs.longOpt("foo")
      val res4: Option[scala.Long] = rs.longOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)

      intercept[ResultSetExtractorException] { rs.long("foo") }
      intercept[ResultSetExtractorException] { rs.long(0) }
    }

    {
      val res1: Reader = rs.nCharacterStream("foo")
      val res2: Reader = rs.nCharacterStream(0)
      val res3: Reader = rs.nCharacterStreamOpt("foo").orNull[Reader]
      val res4: Reader = rs.nCharacterStreamOpt(0).orNull[Reader]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: NClob = rs.nClob("foo")
      val res2: NClob = rs.nClob(0)
      val res3: NClob = rs.nClobOpt("foo").orNull[NClob]
      val res4: NClob = rs.nClobOpt(0).orNull[NClob]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: String = rs.nString("foo")
      val res2: String = rs.nString(0)
      val res3: String = rs.nStringOpt("foo").orNull[String]
      val res4: String = rs.nStringOpt(0).orNull[String]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Any = rs.any("foo")
      val res2: Any = rs.any(0)
      val res3: Any = rs.any("foo", Map[String, Class[?]]())
      val res4: Any = rs.any(0, Map[String, Class[?]]())
      val res5: Option[Any] = rs.anyOpt("foo")
      val res6: Option[Any] = rs.anyOpt(0)
      val res7: Option[Any] = rs.anyOpt("foo", Map[String, Class[?]]())
      val res8: Option[Any] = rs.anyOpt(0, Map[String, Class[?]]())
      res1 == null should be(true)
      res2 == null should be(true)
      res3 == null should be(true)
      res4 == null should be(true)
      res5.isDefined should be(false)
      res6.isDefined should be(false)
      res7.isDefined should be(false)
      res8.isDefined should be(false)
    }

    {
      val res1: Ref = rs.ref("foo")
      val res2: Ref = rs.ref(0)
      val res3: Ref = rs.refOpt("foo").orNull[Ref]
      val res4: Ref = rs.refOpt(0).orNull[Ref]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: java.lang.Short = rs.nullableShort("foo")
      val res2: java.lang.Short = rs.nullableShort(0)
      val res3: Option[scala.Short] = rs.shortOpt("foo")
      val res4: Option[scala.Short] = rs.shortOpt(0)
      res1 should be(null)
      res2 should be(null)
      res3.isDefined should be(false)
      res4.isDefined should be(false)

      intercept[ResultSetExtractorException] { rs.short("foo") }
      intercept[ResultSetExtractorException] { rs.short(0) }
    }

    {
      val res1: SQLXML = rs.sqlXml("foo")
      val res2: SQLXML = rs.sqlXml(0)
      val res3: SQLXML = rs.sqlXmlOpt("foo").orNull[SQLXML]
      val res4: SQLXML = rs.sqlXmlOpt(0).orNull[SQLXML]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: String = rs.string("foo")
      val res2: String = rs.string(0)
      val res3: String = rs.stringOpt("foo").orNull[String]
      val res4: String = rs.stringOpt(0).orNull[String]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

    {
      val res1: Time = rs.time("foo")
      val res2: Time = rs.time(0)
      val res3: Time = rs.time("foo", Calendar.getInstance())
      val res4: Time = rs.time(0, Calendar.getInstance())
      val res5: Time = rs.timeOpt("foo").orNull[Time]
      val res6: Time = rs.timeOpt(0).orNull[Time]
      val res7: Time = rs.timeOpt("foo", Calendar.getInstance()).orNull[Time]
      val res8: Time = rs.timeOpt(0, Calendar.getInstance()).orNull[Time]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
      res5 should be(null)
      res6 should be(null)
      res7 should be(null)
      res8 should be(null)
    }

    {
      val res1: Timestamp = rs.timestamp("foo")
      val res2: Timestamp = rs.timestamp(0)
      val res3: Timestamp = rs.timestamp("foo", Calendar.getInstance())
      val res4: Timestamp = rs.timestamp(0, Calendar.getInstance())
      val res5: Timestamp = rs.timestampOpt("foo").orNull[Timestamp]
      val res6: Timestamp = rs.timestampOpt(0).orNull[Timestamp]
      val res7: Timestamp =
        rs.timestampOpt("foo", Calendar.getInstance()).orNull[Timestamp]
      val res8: Timestamp =
        rs.timestampOpt(0, Calendar.getInstance()).orNull[Timestamp]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
      res5 should be(null)
      res6 should be(null)
      res7 should be(null)
      res8 should be(null)
    }

    {
      val res1: URL = rs.url("foo")
      val res2: URL = rs.url(0)
      val res3: URL = rs.urlOpt("foo").orNull[URL]
      val res4: URL = rs.urlOpt(0).orNull[URL]
      res1 should be(null)
      res2 should be(null)
      res3 should be(null)
      res4 should be(null)
    }

  }

}
