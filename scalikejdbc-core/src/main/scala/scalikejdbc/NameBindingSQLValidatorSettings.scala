package scalikejdbc

import scalikejdbc.globalsettings._

/**
 * Settings for Name binding SQL validator
 */
case class NameBindingSQLValidatorSettings(
  ignoredParams: IgnoredParamsValidation = ExceptionForIgnoredParams
)
