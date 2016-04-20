package scalikejdbc

import scala.collection.JavaConverters._

private[scalikejdbc] object LastParameter

/**
 * SQLInterpolation definition
 */
class SQLInterpolationString(val s: StringContext) extends AnyVal {

  import scalikejdbc.interpolation.SQLSyntax

  def sql[A](params: Any*): SQL[A, NoExtractor] = {
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.rawParameters: _*)
  }

  def sqls(params: Any*): SQLSyntax = {
    // Convert mutable collections to immutable Lists here to avoid
    // mutation from another thread, which might cause a mismatch of
    // the number of placeholders ("?") and parameters.
    val fixedParams = params.map {
      case t: Traversable[_] => t.toList
      case c: java.util.Collection[_] => c.asScala.toList
      case other => other
    }

    SQLSyntax(buildQuery(fixedParams), buildParams(fixedParams))
  }

  private def buildQuery(params: Seq[Any]): String =
    s.parts.zipAll(params, "", LastParameter).foldLeft(new StringBuilder) {
      case (sb, (previousQueryPart, param)) =>
        sb ++= previousQueryPart
        addPlaceholders(sb, param)
    }.result()

  private def addPlaceholders(sb: StringBuilder, param: Any): StringBuilder = param match {
    case _: String => sb += '?'
    case t: Traversable[_] => t.map {
      case SQLSyntax(s, _) => s
      case SQLSyntaxParameterBinder(SQLSyntax(s, _)) => s
      case _ => "?"
    }.addString(sb, ", ") // e.g. in clause
    case LastParameter => sb
    case SQLSyntax(s, _) => sb ++= s
    case SQLSyntaxParameterBinder(SQLSyntax(s, _)) => sb ++= s
    case _ => sb += '?'
  }

  private def buildParams(params: Seq[Any]): Seq[Any] = params.foldLeft(Seq.newBuilder[Any]) {
    case (b, s: String) => b += s
    case (b, t: Traversable[_]) => t.foldLeft(b) {
      case (b, SQLSyntax(_, params)) => b ++= params
      case (b, SQLSyntaxParameterBinder(SQLSyntax(_, params))) => b ++= params
      case (b, e) => b += e
    }
    case (b, SQLSyntax(_, params)) => b ++= params
    case (b, SQLSyntaxParameterBinder(SQLSyntax(_, params))) => b ++= params
    case (b, n) => b += n
  }.result()

}
