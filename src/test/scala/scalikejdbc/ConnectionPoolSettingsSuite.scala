package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.apache.commons.pool.impl.GenericObjectPool
import org.apache.commons.dbcp._
import javax.sql.DataSource
import java.sql.Connection

@RunWith(classOf[JUnitRunner])
class ConnectionPoolSettingsSuite extends FunSuite with ShouldMatchers {

  type ? = this.type // for IntelliJ IDEA

  test("available") {
    val initialSize: Int = 0
    val maxSize: Int = 0
    val validationQuery: String = ""
    val instance = new ConnectionPoolSettings(initialSize,maxSize,validationQuery)
    instance should not be null
  }

}
