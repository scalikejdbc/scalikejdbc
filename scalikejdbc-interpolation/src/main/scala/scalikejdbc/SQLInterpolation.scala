package scalikejdbc

import scala.language.implicitConversions
import scala.language.reflectiveCalls

object SQLInterpolation {
  @inline implicit def interpolation(s: StringContext) = new SQLInterpolation(s)

  private object LastParameter

  case class SQLSyntax(underlying: String)
}

class SQLInterpolation(val s: StringContext) extends AnyVal {
  import SQLInterpolation.{LastParameter, SQLSyntax}

  def sql(param: Any*) = {
    val query = s.parts.zipAll(param, "", LastParameter).foldLeft("") {
      case (r, (q, p)) => r + q + placeholders(p)
    }
    SQL(query).bind(param.flatMap(bindings): _*)
  }

  private def placeholders(p : Any): String = p match {
    case _: String => "?"
    case t: Traversable[_] => t.map(_ => "?").mkString(", ")
    case LastParameter => ""
    case SQLSyntax(s) => s
    case _ => "?"
  }

  private def bindings(p : Any): Traversable[Any] = p match {
    case s: String => Seq(s)
    case t: Traversable[_] => t
    case SQLSyntax(s) => Seq()
    case n => Seq(n)
  }
}
