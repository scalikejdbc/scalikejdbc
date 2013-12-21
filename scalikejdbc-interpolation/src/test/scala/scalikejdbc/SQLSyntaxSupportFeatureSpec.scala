package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._

class SQLSyntaxSupportFeatureSpec extends FlatSpec with ShouldMatchers {

  behavior of "SQLSyntaxSupportFeature"

  it should "verify table name" in {
    SQLSyntaxSupportFeature.verifyTableName("foo.bar")
    SQLSyntaxSupportFeature.verifyTableName(" foo.bar ")
    SQLSyntaxSupportFeature.verifyTableName("foo bar")
    SQLSyntaxSupportFeature.verifyTableName("foo;bar")
  }

}
