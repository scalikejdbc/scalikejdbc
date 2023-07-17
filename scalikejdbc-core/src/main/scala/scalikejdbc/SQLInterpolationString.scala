package scalikejdbc

import scala.jdk.CollectionConverters._

private[scalikejdbc] object LastParameter

/**
 * SQLInterpolation definition
 */
class SQLInterpolationString(private val s: StringContext) extends AnyVal {

  import scalikejdbc.interpolation.SQLSyntax

  def sql[A](params: Any*): SQL[A, NoExtractor] = {
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.rawParameters.toSeq: _*)
  }

  def sqls(params: Any*): SQLSyntax = {
    // Convert mutable collections to immutable Lists here to avoid
    // mutation from another thread, which might cause a mismatch of
    // the number of placeholders ("?") and parameters.
    val fixedParams = params.map {
      case t: Traversable[_]          => t.toList
      case c: java.util.Collection[_] => c.asScala.toList
      case other                      => other
    }

    SQLSyntax(buildQuery(fixedParams), buildParams(fixedParams))
  }

  private def buildQuery(params: collection.Seq[Any]): String =
    s.parts
      .zipAll(params, "", LastParameter)
      .foldLeft(new StringBuilder) { case (sb, (previousQueryPart, param)) =>
        sb ++= previousQueryPart
        addPlaceholders(sb, param)
      }
      .result()

  private def addPlaceholders(sb: StringBuilder, param: Any): StringBuilder =
    param match {
      case _: String => sb += '?'
      case traversable: Traversable[_] => {
        // e.g. in clause
        traversable
          .map {
            case SQLSyntax(s, _)                           => s
            case SQLSyntaxParameterBinder(SQLSyntax(s, _)) => s
            case _                                         => "?"
          }
          .addString(sb, ", ")
      }
      case LastParameter                             => sb
      case SQLSyntax(s, _)                           => sb ++= s
      case SQLSyntaxParameterBinder(SQLSyntax(s, _)) => sb ++= s
      case _                                         => sb += '?'
    }

  private def buildParams(params: collection.Seq[Any]): collection.Seq[Any] =
    params
      .foldLeft(Seq.newBuilder[Any]) {
        case (builder, strParam: String) => builder += strParam
        case (builder, traversable: Traversable[_]) =>
          traversable.foldLeft(builder) {
            case (builder, SQLSyntax(_, params)) => builder ++= params
            case (builder, SQLSyntaxParameterBinder(SQLSyntax(_, params))) =>
              builder ++= params
            case (builder, paramInTraversable) => builder += paramInTraversable
          }
        case (builder, SQLSyntax(_, params)) => builder ++= params
        case (builder, SQLSyntaxParameterBinder(SQLSyntax(_, params))) =>
          builder ++= params
        case (builder, param) => builder += param
      }
      .result()

}
