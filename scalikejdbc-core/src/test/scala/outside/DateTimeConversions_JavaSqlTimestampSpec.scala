package outside

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
//import org.joda.time.{ DateTime, DateTimeZone }

class DateTimeConversions_JavaSqlTimestampSpec
  extends AnyFlatSpec
  with Matchers {

  behavior of "implicit conversions for java.sql.Timestamp"

  // TODO This test case affects others
  /*
  val timezoneId = "America/Los_Angeles"
  val timezone = DateTimeZone.forID(timezoneId)
  DateTimeZone.setDefault(timezone)
  TimeZone.setDefault(TimeZone.getTimeZone(timezoneId))

  val datetime = new DateTime(2012, 5, 12, 13, 40, timezone)

  it should "have #toDateTime" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toDateTime
    actual.getYear should equal(2012)
    actual.getMonthOfYear should equal(5)
    actual.getDayOfMonth should equal(12)
    actual.getHourOfDay should equal(13)
    actual.getMinuteOfHour should equal(40)
    actual.getSecondOfMinute should equal(0)
  }

  it should "have #toDateTimeWithTimeZone" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toDateTimeWithTimeZone(timezone)
    actual.toDateTime.getYear should equal(2012)
    actual.toDateTime.getMonthOfYear should equal(5)
    actual.toDateTime.getDayOfMonth should equal(12)
    actual.getHourOfDay should equal(13)
    actual.getMinuteOfHour should equal(40)
    actual.getSecondOfMinute should equal(0)
  }

  it should "have #toJavaUtilDate" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toJavaUtilDate
    actual.toDateTime.getYear should equal(2012)
    actual.toDateTime.getMonthOfYear should equal(5)
    actual.toDateTime.getDayOfMonth should equal(12)
    actual.toDateTime.getHourOfDay should equal(13)
    actual.toDateTime.getMinuteOfHour should equal(40)
    actual.toDateTime.getSecondOfMinute should equal(0)
  }

  it should "have #toLocalDate" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toLocalDate
    actual.getYear should equal(2012)
    actual.getMonthOfYear should equal(5)
    actual.getDayOfMonth should equal(12)
  }

  it should "have #toLocalDateWithTimeZone" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toLocalDateWithTimeZone(timezone)
    actual.getYear should equal(2012)
    actual.getMonthOfYear should equal(5)
    actual.getDayOfMonth should equal(12)
  }

  it should "have #toLocalDateTime" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toLocalDateTime
    actual.getYear should equal(2012)
    actual.getMonthOfYear should equal(5)
    actual.getDayOfMonth should equal(12)
    actual.getHourOfDay should equal(13)
    actual.getMinuteOfHour should equal(40)
    actual.getSecondOfMinute should equal(0)
  }

  it should "have #toLocalTime" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toLocalTime
    actual.getHourOfDay should equal(13)
    actual.getMinuteOfHour should equal(40)
    actual.getSecondOfMinute should equal(0)
  }

  it should "have #toLocalTimeWithTimeZone" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toLocalTimeWithTimeZone(timezone)
    actual.getHourOfDay should equal(13)
    actual.getMinuteOfHour should equal(40)
    actual.getSecondOfMinute should equal(0)
  }

  it should "have #toSqlTimestamp" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toSqlTimestamp
    actual should not be (null)
  }

  it should "have #toSqlDate" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toSqlDate
    actual should not be (null)
  }

  it should "have #toSqlTime" in {
    val date = datetime.toDate.toSqlTimestamp
    val actual = date.toSqlTime
    actual should not be (null)
  }
   */

}
