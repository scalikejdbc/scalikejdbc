package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SQLSyntaxSupportFeatureSpec
  extends AnyFlatSpec
  with Matchers
  with SQLInterpolation {

  behavior of "SQLSyntaxSupportFeature"

  it should "verify table name" in {
    SQLSyntaxSupportFeature.verifyTableName("foo.bar")
    SQLSyntaxSupportFeature.verifyTableName(" foo.bar ")
    SQLSyntaxSupportFeature.verifyTableName("foo bar")
    SQLSyntaxSupportFeature.verifyTableName("foo;bar")
  }

}
