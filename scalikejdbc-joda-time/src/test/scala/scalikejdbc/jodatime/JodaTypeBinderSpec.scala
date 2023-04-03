package scalikejdbc
package jodatime

import java.sql.ResultSet
import java.time.ZoneId
import org.joda.time._
import scalikejdbc.jodatime.JodaTypeBinder._
import JodaUnixTimeInMillisConverterImplicits._
import org.mockito.Mockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JodaTypeBinderSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "TypeBinder"

  it should "deal with optional values" in {
    val rs: ResultSet = mock[ResultSet]
    implicitly[TypeBinder[Option[DateTime]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalDateTime]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalDate]]].apply(rs, 1) should be(None)
    implicitly[TypeBinder[Option[LocalTime]]].apply(rs, 1) should be(None)
  }

  it should "configurable timezone" in {
    val current = System.currentTimeMillis
    val date = new java.util.Date(current)
    val rs: ResultSet = mock[ResultSet]

    Mockito
      .when(rs.getTimestamp("time"))
      .thenReturn(new java.sql.Timestamp(current))
    Mockito.when(rs.getDate("date")).thenReturn(new java.sql.Date(current))
    Mockito.when(rs.getTime("time")).thenReturn(new java.sql.Time(current))

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
      dateTime: DateTime,
      localDate: LocalDate,
      localTime: LocalTime,
      localDateTime: LocalDateTime
    ) {
      def notEqualAll(that: Values) = {
        this.dateTime should not be that.dateTime
        this.localTime should not be that.localTime
        this.localDateTime should not be that.localDateTime
      }
    }

    val valuesDefault = locally {
      val values = Values(
        implicitly[TypeBinder[DateTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDate]].apply(rs, "date"),
        implicitly[TypeBinder[LocalTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDateTime]].apply(rs, "time")
      )

      values.dateTime shouldBe date.toJodaDateTime
      values.localDate shouldBe date.toJodaLocalDate
      values.localTime shouldBe date.toJodaLocalTime
      values.localDateTime shouldBe date.toJodaLocalDateTime

      values
    }

    val valuesAnother = locally {
      implicit val overwrittenZone: OverwrittenZoneId =
        OverwrittenZoneId(anotherZone)

      val values = Values(
        implicitly[TypeBinder[DateTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDate]].apply(rs, "date"),
        implicitly[TypeBinder[LocalTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDateTime]].apply(rs, "time")
      )

      values.dateTime shouldBe date.toJodaDateTimeWithZoneId(anotherZone)
      values.localDate shouldBe date.toJodaLocalDateWithZoneId(anotherZone)
      values.localTime shouldBe date.toJodaLocalTimeWithZoneId(anotherZone)
      values.localDateTime shouldBe date.toJodaLocalDateTimeWithZoneId(
        anotherZone
      )

      values
    }

    valuesDefault notEqualAll valuesAnother

    val valuesExplicitDefault = locally {
      implicit val overwrittenZone: OverwrittenZoneId =
        OverwrittenZoneId(ZoneId.systemDefault)

      Values(
        implicitly[TypeBinder[DateTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDate]].apply(rs, "date"),
        implicitly[TypeBinder[LocalTime]].apply(rs, "time"),
        implicitly[TypeBinder[LocalDateTime]].apply(rs, "time")
      )
    }

    valuesDefault shouldBe valuesExplicitDefault
  }

}
