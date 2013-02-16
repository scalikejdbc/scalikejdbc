package scalikejdbc

import scala.language.implicitConversions
import scala.language.reflectiveCalls

/**
 * SQLInterpolation companion object
 */
object SQLInterpolation {

  @inline implicit def interpolation(s: StringContext) = new SQLInterpolation(s)

  private object LastParameter

  /**
   * Value as a part of SQL syntax.
   *
   * This value won't be treated as a binding parameter but will be appended as a part of SQL.
   */
  case class SQLSyntax(underlying: String)

}

/**
 * SQLInterpolation
 */
class SQLInterpolation(val s: StringContext) extends AnyVal {

  import SQLInterpolation.{LastParameter, SQLSyntax}

  def sql(params: Any*) = {
    val query: String = s.parts.zipAll(params, "", LastParameter).foldLeft("") {
      case (query, (previousQueryPart, param)) => query + previousQueryPart + getPlaceholders(param)
    }
    SQL(query).bind(params.flatMap(toSeq): _*)
  }

  private def getPlaceholders(param : Any): String = param match {
    case _: String => "?"
    case t: Traversable[_] => t.map(_ => "?").mkString(", ") // e.g. in clause
    case LastParameter => ""
    case SQLSyntax(s) => s
    case _ => "?"
  }

  private def toSeq(param : Any): Traversable[Any] = param match {
    case s: String => Seq(s)
    case t: Traversable[_] => t
    case SQLSyntax(s) => Nil
    case n => Seq(n)
  }

}
