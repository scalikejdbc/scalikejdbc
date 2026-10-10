package scalikejdbc

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scalikejdbc.globalsettings._

class NameBindingSQLValidatorSettingsSpec extends AnyFlatSpec with Matchers {

  behavior of "NameBindingSQLValidatorSettings"

  it should "be available" in {
    val ignoredParams: IgnoredParamsValidation = null
    val instance = new NameBindingSQLValidatorSettings(ignoredParams)
    instance should not be null
  }

}
