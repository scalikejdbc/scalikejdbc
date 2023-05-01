package scalikejdbc

import scala.util.control.Exception.ultimately
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DBSessionWrapperSpec extends AnyFlatSpec with Matchers {

  val tableName = "emp_DBSessionWrapperSpec" + System
    .currentTimeMillis()
    .toString
    .substring(8)

  behavior of "DBSessionWrapper"

  it should "be called #overwrite -> #recover" in {
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      var step = 0
      val result = DB readOnly { session =>
        val sql = SQL("")
        val attributesSwitcher = new DBSessionAttributesSwitcher(sql) {
          override protected def overwriteAttributes(): Unit = {
            if (step == 0) step += 1
          }
          override protected def recoverOriginalAttributes(): Unit = {
            if (step == 1) step += 1
          }
        }
        (new DBSessionWrapper(session, attributesSwitcher)).list(
          "select * from " + tableName + ""
        )(_.string("name"))
      }
      result.size should be > 0
      step shouldEqual 2
    }
  }
}
