package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class JDBCUrlSpec extends AnyFlatSpec with Matchers {

  behavior of "JDBCUrl"

  it should "parse a database name before JDBC query parameters" in {
    JDBCUrl("jdbc:postgresql://localhost:64161/test?loggerLevel=OFF") shouldBe
      JDBCUrl("localhost", 64161, "test")
  }

}
