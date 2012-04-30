package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class PackageObjectSpec extends FlatSpec with ShouldMatchers {

  behavior of "package object"

  it should "be available" in {
    import scalikejdbc._
    val timestamp = new java.sql.Timestamp(0L)
    timestamp.toJavaUtilDate should not be (null)
  }

}
