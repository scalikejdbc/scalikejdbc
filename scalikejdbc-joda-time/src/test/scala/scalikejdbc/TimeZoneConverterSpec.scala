package scalikejdbc

import java.time.temporal.ChronoUnit
import java.util.TimeZone

import org.joda.time.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TimeZoneConverterSpec extends AnyFlatSpec with Matchers {

  behavior of "TimeZoneConverter"

  val jst = TimeZone.getTimeZone("Asia/Tokyo") // UTC+9
  val ast = TimeZone.getTimeZone("AST") // UTC-9 with DST
  val converter = TimeZoneConverter.from(jst).to(ast)

  val date = new DateTime(2016, 1, 3, 12, 0).toDate.getTime
  val dstDate = new DateTime(2016, 7, 3, 12, 0).toDate.getTime

  val jstOffset = jst.getOffset(date)
  val astOffset = ast.getOffset(date)
  val astOffsetWithDst = ast.getOffset(dstDate)

  it should "convert timeZone of java.sql.Timestamp" in {
    val sqlTimestamp = new java.sql.Timestamp(date)
    val convertedTimestamp = converter.convert(sqlTimestamp)

    (sqlTimestamp.getTime - convertedTimestamp.getTime) should equal(
      jstOffset - astOffset
    )
  }

  it should "convert timeZone of java.sql.Timestamp with DST" in {
    val sqlTimestamp = new java.sql.Timestamp(dstDate)
    val convertedTimestamp = converter.convert(sqlTimestamp)

    (sqlTimestamp.getTime - convertedTimestamp.getTime) should equal(
      jstOffset - astOffsetWithDst
    )
  }

  it should "cache converter for same [from/to]" in {
    val converter1 = TimeZoneConverter.from(jst).to(ast)
    val converter2 = TimeZoneConverter.from(jst).to(ast)

    converter1 should be theSameInstanceAs converter2
  }

  it should "keep nano seconds" in {
    // https://github.com/scalikejdbc/scalikejdbc/issues/1133
    Seq(
      java.time.Instant.parse("2021-01-01T01:23:45.123456Z") -> astOffset,
      java.time.Instant.parse("2021-07-07T02:34:18.123456Z") -> astOffsetWithDst
    ).foreach { case (time1, offset) =>
      val time2 = converter.convert(java.sql.Timestamp.from(time1))
      val time3 = time2.toInstant
      val time4 = time1.plus(offset - jstOffset, ChronoUnit.MILLIS)
      time3 should equal(time4)
    }
  }
}
