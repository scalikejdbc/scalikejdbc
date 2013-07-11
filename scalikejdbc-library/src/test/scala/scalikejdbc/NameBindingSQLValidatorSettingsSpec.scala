package scalikejdbc

import org.scalatest._
import org.scalatest.matchers._
import scalikejdbc.globalsettings._

class NameBindingSQLValidatorSettingsSpec extends FlatSpec with ShouldMatchers {

  behavior of "NameBindingSQLValidatorSettings"

  it should "be available" in {
    val ignoredParams: IgnoredParamsValidation = null
    val instance = new NameBindingSQLValidatorSettings(ignoredParams)
    instance should not be null
  }

}
