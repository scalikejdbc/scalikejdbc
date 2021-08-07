package scalikejdbc

import scala.util.control.Exception._
import java.util.NoSuchElementException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class StringSQLRunnerSpec extends AnyFlatSpec with Matchers with Settings {

  val tableNamePrefix = "emp_StringSQLRunnerSpec" + System.currentTimeMillis()

  behavior of "StringSQLRunner"

  it should "be available" in {

    val tableName = tableNamePrefix + "_beAvailable"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      import scalikejdbc.StringSQLRunner._

      // run insert SQL
      ("insert into " + tableName + " values (3, 'Ben')").run()
      ("insert into " + tableName + " values (4, 'Chris')").execute()

      // run select SQL
      val result = ("select id,name from " + tableName + " where id = 3").run()
      if (result.head.get("ID").isDefined) {
        result.head.apply("ID") should equal(3)
        result.head.apply("NAME") should equal("Ben")
      } else {
        result.head.apply("id") should equal(3)
        result.head.apply("name") should equal("Ben")
      }

      // should be found
      ("select name from " + tableName + " where id = 3")
        .asList[String] should equal(List("Ben"))
      ("select name from " + tableName + " where id = 3")
        .asOption[String] should equal(Some("Ben"))
      ("select name from " + tableName + " where id = 3")
        .as[String] should equal("Ben")

      // should not be found
      ("select name from " + tableName + " where id = 999")
        .asList[String] should equal(Nil)
      ("select name from " + tableName + " where id = 999")
        .asOption[String] should equal(None)
      try {
        ("select name from " + tableName + " where id = 999").as[String]
        fail("NoSuchElementException is expected")
      } catch {
        case e: NoSuchElementException =>
      }

    }
  }

  it should "cast number values" in {

    val runner = new StringSQLRunner("")
    val expectedInt: Int = 123
    val expectedLong: Long = 123L
    val expectedString: String = "123"

    val javaInteger: java.lang.Integer = java.lang.Integer.parseInt("123")
    val scalaInt: Int = 123
    val javaShort: java.lang.Short = java.lang.Short.parseShort("123")
    val scalaShort: Short = 123
    val javaBigDecimal: java.math.BigDecimal = new java.math.BigDecimal("123")
    val scalaBigDecimal: scala.math.BigDecimal = scala.math.BigDecimal("123")
    val javaBigInteger: java.math.BigInteger = new java.math.BigInteger("123")
    val scalaBigInt: scala.math.BigInt = scala.math.BigInt("123")

    runner.cast[Int](javaInteger) should equal(expectedInt)
    runner.cast[Int](scalaInt) should equal(expectedInt)
    runner.cast[Int](javaShort) should equal(expectedInt)
    runner.cast[Int](scalaShort) should equal(expectedInt)
    runner.cast[Int](javaBigDecimal) should equal(expectedInt)
    runner.cast[Int](scalaBigDecimal) should equal(expectedInt)
    runner.cast[Int](javaBigInteger) should equal(expectedInt)
    runner.cast[Int](scalaBigInt) should equal(expectedInt)

    runner.cast[Long](javaInteger) should equal(expectedLong)
    runner.cast[Long](scalaInt) should equal(expectedLong)
    runner.cast[Long](javaShort) should equal(expectedLong)
    runner.cast[Long](scalaShort) should equal(expectedLong)
    runner.cast[Long](javaBigDecimal) should equal(expectedLong)
    runner.cast[Long](scalaBigDecimal) should equal(expectedLong)
    runner.cast[Long](javaBigInteger) should equal(expectedLong)
    runner.cast[Long](scalaBigInt) should equal(expectedLong)

    runner.cast[String](javaInteger) should equal(expectedString)
    runner.cast[String](scalaInt) should equal(expectedString)
    runner.cast[String](javaShort) should equal(expectedString)
    runner.cast[String](scalaShort) should equal(expectedString)
    runner.cast[String](javaBigDecimal) should equal(expectedString)
    runner.cast[String](scalaBigDecimal) should equal(expectedString)
    runner.cast[String](javaBigInteger) should equal(expectedString)
    runner.cast[String](scalaBigInt) should equal(expectedString)

  }

}
