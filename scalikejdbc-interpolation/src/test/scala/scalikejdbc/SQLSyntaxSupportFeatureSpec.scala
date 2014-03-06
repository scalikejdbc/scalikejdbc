package scalikejdbc

import org.scalatest._

class SQLSyntaxSupportFeatureSpec extends FlatSpec with Matchers {

  behavior of "SQLSyntaxSupportFeature"

  it should "verify table name" in {
    SQLSyntaxSupportFeature.verifyTableName("foo.bar")
    SQLSyntaxSupportFeature.verifyTableName(" foo.bar ")
    SQLSyntaxSupportFeature.verifyTableName("foo bar")
    SQLSyntaxSupportFeature.verifyTableName("foo;bar")
  }

}
