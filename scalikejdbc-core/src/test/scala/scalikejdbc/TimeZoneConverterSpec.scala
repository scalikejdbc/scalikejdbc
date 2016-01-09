package scalikejdbc

import java.util.TimeZone

import org.joda.time.DateTime
import org.scalatest.{ FlatSpec, Matchers }

class TimeZoneConverterSpec extends FlatSpec with Matchers {

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

    (sqlTimestamp.getTime - convertedTimestamp.getTime) should equal(jstOffset - astOffset)
  }

  it should "convert timeZone of java.sql.Timestamp with DST" in {
    val sqlTimestamp = new java.sql.Timestamp(dstDate)
    val convertedTimestamp = converter.convert(sqlTimestamp)

    (sqlTimestamp.getTime - convertedTimestamp.getTime) should equal(jstOffset - astOffsetWithDst)
  }

  it should "convert timeZone of java.sql.Time" in {
    val sqlTime = new java.sql.Time(date)
    val convertedTime = converter.convert(sqlTime)

    (sqlTime.getTime - convertedTime.getTime) should equal(jstOffset - astOffset)
  }

  it should "convert timeZone of java.sql.Time with DST" in {
    val sqlTime = new java.sql.Time(dstDate)
    val convertedTime = converter.convert(sqlTime)

    (sqlTime.getTime - convertedTime.getTime) should equal(jstOffset - astOffsetWithDst)
  }

  it should "convert timeZone of java.sql.Date" in {
    val sqlDate = new java.sql.Date(date)
    val convertedDate = converter.convert(sqlDate)

    (sqlDate.getTime - convertedDate.getTime) should equal(jstOffset - astOffset)
  }

  it should "convert timeZone of java.sql.Date with DST" in {
    val sqlDate = new java.sql.Date(dstDate)
    val convertedDate = converter.convert(sqlDate)

    (sqlDate.getTime - convertedDate.getTime) should equal(jstOffset - astOffsetWithDst)
  }

  it should "cache converter for same [from/to]" in {
    val converter1 = TimeZoneConverter.from(jst).to(ast)
    val converter2 = TimeZoneConverter.from(jst).to(ast)

    converter1 should be theSameInstanceAs converter2
  }
}
