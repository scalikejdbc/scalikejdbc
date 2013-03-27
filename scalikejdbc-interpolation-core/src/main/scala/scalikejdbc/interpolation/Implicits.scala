package scalikejdbc.interpolation

import scala.language.implicitConversions
import scalikejdbc.SQLInterpolationString

object Implicits {

  @inline implicit def convertSQLSyntaxToString(syntax: SQLSyntax): String = syntax.value
  @inline implicit def interpolation(s: StringContext) = new SQLInterpolationString(s)

}

