package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LikeConditionEscapeUtilSpec extends AnyFlatSpec with Matchers {

  behavior of "LikeConditionEscapeUtil"

  it should "be available as singleton" in {
    LikeConditionEscapeUtil.escape("foo%aa_bbb\\ccc") should equal(
      "foo\\%aa\\_bbb\\\\ccc"
    )
    LikeConditionEscapeUtil.beginsWith("foo%aa_bbb\\ccc") should equal(
      "foo\\%aa\\_bbb\\\\ccc%"
    )
    LikeConditionEscapeUtil.endsWith("foo%aa_bbb\\ccc") should equal(
      "%foo\\%aa\\_bbb\\\\ccc"
    )
    LikeConditionEscapeUtil.contains("foo%aa_bbb\\ccc") should equal(
      "%foo\\%aa\\_bbb\\\\ccc%"
    )
  }

  it should "be available" in {
    val util = LikeConditionEscapeUtil("@")
    util.escape("foo%@aa_bbb\\ccc") should equal("foo@%@@aa@_bbb\\ccc")
    util.beginsWith("foo%@aa_bbb\\ccc") should equal("foo@%@@aa@_bbb\\ccc%")
    util.endsWith("foo%@aa_bbb\\ccc") should equal("%foo@%@@aa@_bbb\\ccc")
    util.contains("foo%@aa_bbb\\ccc") should equal("%foo@%@@aa@_bbb\\ccc%")
  }

}
