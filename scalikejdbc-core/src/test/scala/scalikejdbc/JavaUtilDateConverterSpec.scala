package scalikejdbc

import java.time.temporal.ChronoUnit
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JavaUtilDateConverterSpec
  extends AnyFlatSpec
  with Matchers
  with JavaUtilDateConverterImplicits {

  behavior of "JavaUtilDateConverter"

  it should "have #toJavaUtilDate" in {
    val d: java.util.Date = new java.util.Date().toJavaUtilDate
    d should not be null
  }

  it should "have #toSqlDate" in {
    val d: java.sql.Date = new java.util.Date().toSqlDate
    d should not be null
  }

  it should "have #toSqlTime" in {
    val d: java.sql.Time = new java.util.Date().toSqlTime
    d should not be null
  }

  it should "have #toSqlTimestamp" in {
    val d: java.sql.Timestamp = new java.util.Date().toSqlTimestamp
    d should not be null
  }

  it should "not drop nano seconds" in {
    val nano = 123456789
    val t1 = java.time.ZonedDateTime.now
      .truncatedTo(ChronoUnit.SECONDS)
      .plusNanos(nano)
    val t2 = java.sql.Timestamp.from(t1.toInstant)
    assert(t2.toLocalDateTime.getNano === nano)
    assert(t2.toZonedDateTime.getNano === nano)
    assert(t2.toOffsetDateTime.getNano === nano)
    assert(t2.toInstant.getNano === nano)
    assert(t2.toLocalTime.getNano === nano)
    assert(t2.toSqlTimestamp.getNanos === nano)
  }

}
