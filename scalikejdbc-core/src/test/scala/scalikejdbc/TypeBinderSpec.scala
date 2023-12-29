package scalikejdbc

import org.mockito.Mockito._
import java.sql.ResultSet
import java.time._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TypeBinderSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with JavaUtilDateConverterImplicits {

  behavior of "TypeBinder"

  it should "have TypeBinder.from" in {
    case class TestValue(a: Long)

    val t = TypeBinder.from(TestValue.apply)
    t: TypeBinder[TestValue]
  }

  it should "be able to return an Option value when NPE" in {
    val result: Option[String] =
      implicitly[TypeBinder[Option[String]]].apply(null, 1)
    result should be(None)
  }

  it should "have TypeBinder for scala.BigDecimal" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getBigDecimal("decimal"))
      .thenReturn(new java.math.BigDecimal("1234567"))
    when(rs.getBigDecimal(1)).thenReturn(new java.math.BigDecimal("2345678"))

    implicitly[TypeBinder[BigDecimal]].apply(rs, "decimal") should be(
      BigDecimal("1234567")
    )
    implicitly[TypeBinder[BigDecimal]].apply(rs, 1) should be(
      BigDecimal("2345678")
    )

    locally {
      implicitly[TypeBinder[Option[BigDecimal]]].apply(rs, "decimal") should be(
        Some(BigDecimal("1234567"))
      )
      implicitly[TypeBinder[Option[BigDecimal]]].apply(rs, 1) should be(
        Some(BigDecimal("2345678"))
      )
      implicitly[TypeBinder[Option[BigDecimal]]].apply(rs, "none") should be(
        None
      )
      implicitly[TypeBinder[Option[BigDecimal]]].apply(rs, 2) should be(None)
    }

    locally {
      TypeBinder.of[Option[BigDecimal]].apply(rs, "decimal") should be(
        Some(BigDecimal("1234567"))
      )
      TypeBinder.of[Option[BigDecimal]].apply(rs, 1) should be(
        Some(BigDecimal("2345678"))
      )
      TypeBinder.of[Option[BigDecimal]].apply(rs, "none") should be(None)
      TypeBinder.of[Option[BigDecimal]].apply(rs, 2) should be(None)
    }
  }

  it should "have TypeBinder for scala.BigInt" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getBigDecimal("integer"))
      .thenReturn(new java.math.BigDecimal("1234567"))
    when(rs.getBigDecimal(1)).thenReturn(new java.math.BigDecimal("2345678"))

    locally {
      implicitly[TypeBinder[BigInt]].apply(rs, "integer") should be(
        BigInt("1234567")
      )
      implicitly[TypeBinder[BigInt]].apply(rs, 1) should be(BigInt("2345678"))

      implicitly[TypeBinder[Option[BigInt]]].apply(rs, "integer") should be(
        Some(BigInt("1234567"))
      )
      implicitly[TypeBinder[Option[BigInt]]].apply(rs, 1) should be(
        Some(BigInt("2345678"))
      )
      implicitly[TypeBinder[Option[BigInt]]].apply(rs, "none") should be(None)
      implicitly[TypeBinder[Option[BigInt]]].apply(rs, 2) should be(None)
    }

    locally {
      TypeBinder.of[BigInt].apply(rs, "integer") should be(BigInt("1234567"))
      TypeBinder.of[BigInt].apply(rs, 1) should be(BigInt("2345678"))

      TypeBinder.of[Option[BigInt]].apply(rs, "integer") should be(
        Some(BigInt("1234567"))
      )
      TypeBinder.of[Option[BigInt]].apply(rs, 1) should be(
        Some(BigInt("2345678"))
      )
      TypeBinder.of[Option[BigInt]].apply(rs, "none") should be(None)
      TypeBinder.of[Option[BigInt]].apply(rs, 2) should be(None)
    }
  }

  it should "have TypeBinder for java.util.Date/Calendar" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getTimestamp("time"))
      .thenReturn(new java.sql.Timestamp(System.currentTimeMillis))
    when(rs.getDate("date"))
      .thenReturn(new java.sql.Date(System.currentTimeMillis))

    locally {
      implicitly[TypeBinder[java.sql.Timestamp]]
        .apply(rs, "time") should not be (null)
      implicitly[TypeBinder[LocalDateTime]]
        .apply(rs, "time") should not be (null)
      implicitly[TypeBinder[LocalDate]].apply(rs, "date") should not be (null)

      // implicitly[TypeBinder[java.util.Date]].apply(rs, "time") should not be (null)
      implicitly[TypeBinder[java.sql.Timestamp]]
        .apply(rs, "time")
        .toJavaUtilDate should not be (null)
      implicitly[TypeBinder[java.util.Calendar]]
        .apply(rs, "time") should not be (null)
    }

    locally {
      TypeBinder.of[java.sql.Timestamp].apply(rs, "time") should not be (null)
      TypeBinder.of[LocalDateTime].apply(rs, "time") should not be (null)
      TypeBinder.of[LocalDate].apply(rs, "date") should not be (null)

      // implicitly[TypeBinder[java.util.Date]].apply(rs, "time") should not be (null)
      TypeBinder
        .of[java.sql.Timestamp]
        .apply(rs, "time")
        .toJavaUtilDate should not be (null)
      TypeBinder.of[java.util.Calendar].apply(rs, "time") should not be (null)
    }
  }

  it should "handle result values of type java.math.BigDecimal" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getObject("zero"))
      .thenReturn(java.math.BigDecimal.ZERO, Array[Object]()*)
    when(rs.getObject("nonzero"))
      .thenReturn(java.math.BigDecimal.ONE, Array[Object]()*)

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
    when(rs.getObject("zero"))
      .thenReturn(java.math.BigInteger.ZERO, Array[Object]()*)
    when(rs.getObject("nonzero"))
      .thenReturn(java.math.BigInteger.ONE, Array[Object]()*)

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
    when(rs.getObject("none")).thenReturn(null, Array[Object]()*)

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

  it should "deal with optional values" in {
    val rs: ResultSet = mock[ResultSet]

    implicitly[TypeBinder[Option[java.sql.Array]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.io.InputStream]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[java.math.BigDecimal]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[java.sql.Blob]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Boolean]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[Boolean]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Byte]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Byte]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Array[Byte]]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.io.Reader]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Clob]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Date]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Double]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[Double]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Float]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Float]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Integer]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[Int]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Long]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Long]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.NClob]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[String]]].apply(rs, 1) should be(None)
    // implicitly[TypeBinder[Option[Any]]].apply(rs, 1) should be(None)
    // implicitly[TypeBinder[Option[AnyRef]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Ref]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.RowId]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.lang.Short]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[Short]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.SQLXML]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Time]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Time]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.sql.Timestamp]]].apply(rs, 1) should be(
      None
    )
    implicitly[TypeBinder[Option[LocalDateTime]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[java.net.URL]]].apply(rs, 1) should be(None)
  }

  it should "handle result values of type java.lang.Boolean" in {
    val rs: ResultSet = mock[ResultSet]
    when(rs.getObject("boolean")).thenReturn("true", Array[Object]()*)
    when(rs.getObject(1)).thenReturn("false", Array[Object]()*)
    implicitly[TypeBinder[Boolean]].apply(rs, "boolean") should be(true)
    implicitly[TypeBinder[Boolean]].apply(rs, 1) should be(false)
  }

  it should "configurable timezone" in {
    val current = System.currentTimeMillis
    val date = new java.util.Date(current)
    val rs: ResultSet = mock[ResultSet]

    when(rs.getTimestamp("time")).thenReturn(new java.sql.Timestamp(current))
    when(rs.getDate("date")).thenReturn(new java.sql.Date(current))
    when(rs.getTime("time")).thenReturn(new java.sql.Time(current))

    val defaultZone = ZoneId.systemDefault
    val anotherZone = {
      val hawaii = ZoneId.of("US/Hawaii")
      if (defaultZone == hawaii) {
        ZoneId.of("Asia/Tokyo")
      } else {
        hawaii
      }
    }

    case class Values(
      zonedDateTime: ZonedDateTime,
      offsetDateTime: OffsetDateTime,
      localDate: LocalDate,
      localTime: LocalTime,
      localDateTime: LocalDateTime
    ) {
      def notEqualAll(that: Values) = {
        this.zonedDateTime should not be that.zonedDateTime
        this.offsetDateTime should not be that.offsetDateTime
        this.localTime should not be that.localTime
        this.localDateTime should not be that.localDateTime
      }
    }

    val valuesDefault = locally {
      val values = Values(
        TypeBinder.of[ZonedDateTime].apply(rs, "time"),
        TypeBinder.of[OffsetDateTime].apply(rs, "time"),
        TypeBinder.of[LocalDate].apply(rs, "date"),
        TypeBinder.of[LocalTime].apply(rs, "time"),
        TypeBinder.of[LocalDateTime].apply(rs, "time")
      )

      values.zonedDateTime shouldBe date.toZonedDateTime
      values.offsetDateTime shouldBe date.toOffsetDateTime
      values.localDate shouldBe date.toLocalDate
      values.localTime shouldBe date.toLocalTime
      values.localDateTime shouldBe date.toLocalDateTime

      values
    }

    val valuesAnother = locally {
      implicit val overwrittenZone: OverwrittenZoneId =
        OverwrittenZoneId(anotherZone)

      val values = Values(
        TypeBinder.of[ZonedDateTime].apply(rs, "time"),
        TypeBinder.of[OffsetDateTime].apply(rs, "time"),
        TypeBinder.of[LocalDate].apply(rs, "date"),
        TypeBinder.of[LocalTime].apply(rs, "time"),
        TypeBinder.of[LocalDateTime].apply(rs, "time")
      )

      values.zonedDateTime shouldBe date.toZonedDateTimeWithZoneId(anotherZone)
      values.offsetDateTime shouldBe date.toOffsetDateTimeWithZoneId(
        anotherZone
      )
      values.localDate shouldBe date.toLocalDateWithZoneId(anotherZone)
      values.localTime shouldBe date.toLocalTimeWithZoneId(anotherZone)
      values.localDateTime shouldBe date.toLocalDateTimeWithZoneId(anotherZone)

      values
    }

    valuesDefault notEqualAll valuesAnother

    val valuesExplicitDefault = locally {
      implicit val overwrittenZone: OverwrittenZoneId =
        OverwrittenZoneId(ZoneId.systemDefault)

      Values(
        TypeBinder.of[ZonedDateTime].apply(rs, "time"),
        TypeBinder.of[OffsetDateTime].apply(rs, "time"),
        TypeBinder.of[LocalDate].apply(rs, "date"),
        TypeBinder.of[LocalTime].apply(rs, "time"),
        TypeBinder.of[LocalDateTime].apply(rs, "time")
      )
    }

    valuesDefault shouldBe valuesExplicitDefault
  }
}
