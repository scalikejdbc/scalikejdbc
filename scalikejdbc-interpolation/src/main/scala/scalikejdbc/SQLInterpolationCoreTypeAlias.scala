package scalikejdbc

/**
 * Alias for interpolation core elements.
 */
trait SQLInterpolationCoreTypeAlias {

  type SQLSyntax = scalikejdbc.interpolation.SQLSyntax

  val SQLSyntax = scalikejdbc.interpolation.SQLSyntax

  val sqls = scalikejdbc.interpolation.SQLSyntax

  type ResultAllProvider = scalikejdbc.interpolation.ResultAllProvider

  type AsteriskProvider = scalikejdbc.interpolation.AsteriskProvider

}
