package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import scala.util.control.Exception._

class StringSQLRunnerSpec extends FlatSpec with ShouldMatchers with Settings {

  val tableNamePrefix = "emp_StringSQLRunnerSpec" + System.currentTimeMillis()

  behavior of "StringSQLRunner"

  it should "be available" in {

    val tableName = tableNamePrefix + "_beAvailable"
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)

      import scalikejdbc.StringSQLRunner._

      // run insert SQL
      ("insert into " + tableName + " values (3, 'Ben')").run

      // run select SQL
      val result = ("select id,name from " + tableName + " where id = 3").run
      if (result.head.get("ID").isDefined) {
        result.head.get("ID").get should equal(3)
        result.head.get("NAME").get should equal("Ben")
      } else {
        result.head.get("id").get should equal(3)
        result.head.get("name").get should equal("Ben")
      }

      // should be found
      ("select name from " + tableName + " where id = 3").asList[String] should equal(List("Ben"))
      ("select name from " + tableName + " where id = 3").asSingle[String] should equal(Some("Ben"))

      // should not be found
      ("select name from " + tableName + " where id = 999").asList[String] should equal(List())
      ("select name from " + tableName + " where id = 999").asSingle[String] should equal(None)

    }
  }

}
