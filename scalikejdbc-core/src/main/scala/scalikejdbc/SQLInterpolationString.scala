package scalikejdbc

private[scalikejdbc] object LastParameter

/**
 * SQLInterpolation definition
 */
class SQLInterpolationString(val s: StringContext) extends AnyVal {

  import scalikejdbc.interpolation.SQLSyntax

  def sql[A](params: Any*): SQL[A, NoExtractor] = {
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.parameters: _*)
  }

  def sqls(params: Any*): SQLSyntax = SQLSyntax(buildQuery(params), buildParams(params))

  private def buildQuery(params: Seq[Any]): String =
    s.parts.zipAll(params, "", LastParameter).foldLeft(new StringBuilder) {
      case (sb, (previousQueryPart, param)) =>
        sb ++= previousQueryPart
        addPlaceholders(sb, param)
    }.result()

  private def addPlaceholders(sb: StringBuilder, param: Any): StringBuilder = param match {
    case _: String => sb += '?'
    // to fix issue #215 due to unexpected Stream#addString behavior
    case s: Stream[_] => addPlaceholders(sb, s.toList) // e.g. in clause
    // Need to convert a Set to a List before mapping to "?", otherwise we end up with a 1-element Set
    case s: scala.collection.Set[_] => addPlaceholders(sb, s.toList) // e.g. in clause
    case t: Traversable[_] => t.map {
      case SQLSyntax(s, _) => s
      case _ => "?"
    }.addString(sb, ", ") // e.g. in clause
    case LastParameter => sb
    case SQLSyntax(s, _) => sb ++= s
    case _ => sb += '?'
  }

  private def buildParams(params: Seq[Any]): Seq[Any] = params.foldLeft(Seq.newBuilder[Any]) {
    case (b, s: String) => b += s
    case (b, t: Traversable[_]) => t.foldLeft(b) {
      case (b, SQLSyntax(_, params)) => b ++= params
      case (b, e) => b += e
    }
    case (b, SQLSyntax(_, params)) => b ++= params
    case (b, n) => b += n
  }.result()

}
