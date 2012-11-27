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
      using(new DB(ConnectionPool.borrow())) { db =>
        implicit val session = db.autoCommitSession()

        import scalikejdbc.StringSQLRunner._

        ("insert into " + tableName + " values (3, 'Ben')").run
        val result = ("select id,name from " + tableName + " where id = 3").run
        if (result.head.get("ID").isDefined) {
          result.head.get("ID").get should equal(3)
          result.head.get("NAME").get should equal("Ben")
        } else {
          result.head.get("id").get should equal(3)
          result.head.get("name").get should equal("Ben")
        }
      }
    }
  }

}
