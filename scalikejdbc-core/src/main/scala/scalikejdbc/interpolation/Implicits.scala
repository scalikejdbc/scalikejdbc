package scalikejdbc.interpolation

import scala.language.implicitConversions
import scalikejdbc.SQLInterpolationString

/**
 * object to import.
 */
object Implicits extends Implicits

/**
 * Implicit conversion imports.
 */
trait Implicits {

  /**
   * Enables sql"", sqls"" interpolation.
   *
   * {{{
   *   sql"select * from members"
   *   val whereClause = sqls"where id = ${id}"
   *   sql"select * from members ${whereClause}"
   * }}}
   */
  @inline implicit def scalikejdbcSQLInterpolationImplicitDef(
    s: StringContext
  ): SQLInterpolationString = new SQLInterpolationString(s)

  /**
   * Returns String value when String type is expected for [[scalikejdbc.WrappedResultSet]].
   *
   * {{{
   *   val c = Company.syntax("c").resultName
   *   rs.string(c.name)
   * }}}
   */
  @inline implicit def scalikejdbcSQLSyntaxToStringImplicitDef(
    syntax: scalikejdbc.interpolation.SQLSyntax
  ): String = syntax.value

}
