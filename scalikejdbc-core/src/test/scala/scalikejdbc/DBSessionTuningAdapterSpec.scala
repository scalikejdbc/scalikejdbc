package scalikejdbc

import org.scalatest._

import scala.util.control.Exception.ultimately

class DBSessionTuningAdapterSpec extends FlatSpec with Matchers {

  val tableName = "emp_DBSessionTuningAdapterSpec" + System.currentTimeMillis().toString.substring(8)

  behavior of "DBSessionTuningAdapter"

  it should "" in {
    ultimately(TestUtils.deleteTable(tableName)) {
      TestUtils.initialize(tableName)
      var step = 0
      val result = DB readOnly { session =>
        val tuner = new DBSessionTuner {
          override def tune(session: DBSession): session.type = {
            if (step == 0) step += 1
            session
          }

          override def reset(session: DBSession): session.type = {
            if (step == 1) step += 1
            session
          }
        }
        DBSessionTuningAdapter(session, tuner).list("select * from " + tableName + "")(rs => rs.string("name"))
      }
      result.size should be > 0
      step shouldEqual 2
    }
  }
}
