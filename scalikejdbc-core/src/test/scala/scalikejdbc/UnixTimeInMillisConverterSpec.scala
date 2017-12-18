package scalikejdbc

import org.scalatest._

class UnixTimeInMillisConverterSpec extends FlatSpec with Matchers with UnixTimeInMillisConverterImplicits {

  behavior of "UnixTimeInMillisConverter"

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

}
