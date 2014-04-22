package scalikejdbc

import org.scalatest._

class SQLSyntaxSupportFeatureSpec extends FlatSpec with Matchers with SQLInterpolation {

  behavior of "SQLSyntaxSupportFeature"

  it should "verify table name" in {
    SQLSyntaxSupportFeature.verifyTableName("foo.bar")
    SQLSyntaxSupportFeature.verifyTableName(" foo.bar ")
    SQLSyntaxSupportFeature.verifyTableName("foo bar")
    SQLSyntaxSupportFeature.verifyTableName("foo;bar")
  }

}
