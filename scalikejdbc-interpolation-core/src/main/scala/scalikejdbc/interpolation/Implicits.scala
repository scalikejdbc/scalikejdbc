package scalikejdbc.interpolation

import scala.language.implicitConversions
import scalikejdbc.SQLInterpolationString

object Implicits {

  @inline implicit def scalikejdbcSQLInterpolationImplicitDef(s: StringContext) = new scalikejdbc.SQLInterpolationString(s)
  @inline implicit def scalikejdbcSQLSyntaxToStringImplicitDef(syntax: scalikejdbc.interpolation.SQLSyntax): String = syntax.value

}

