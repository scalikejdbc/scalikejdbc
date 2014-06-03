package scalikejdbc

import org.scalatest._

class PackageObjectSpec extends FlatSpec with Matchers {

  behavior of "package object"

  it should "be available" in {
    import scalikejdbc._
    val timestamp = new java.sql.Timestamp(0L)
    timestamp.toJavaUtilDate should not be (null)
  }

  it should "convert java.math.BigDecimal" in {
    import scalikejdbc._
    val decimal = new java.math.BigDecimal("123")
    decimal.toScalaBigDecimal.isInstanceOf[scala.math.BigDecimal] should be(true)
    val nullDecimal: java.math.BigDecimal = null
    val nullScalaBigDecimal: scala.math.BigDecimal = nullDecimal.toScalaBigDecimal
    nullScalaBigDecimal should be(null)
  }

}
