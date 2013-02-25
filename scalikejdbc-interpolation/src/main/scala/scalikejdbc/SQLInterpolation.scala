package scalikejdbc

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.language.dynamics

/**
 * SQLInterpolation companion object
 */
object SQLInterpolation {

  private object LastParameter

  /**
   * Value as a part of SQL syntax.
   *
   * This value won't be treated as a binding parameter but will be appended as a part of SQL.
   */
  case class SQLSyntax(value: String)

  /**
   * SQLSyntax support utilities
   */
  trait SQLSyntaxSupport[A] {

    def tableName: String
    def columns: Seq[String]

    def forceUpperCase: Boolean = false
    def delimiterForResultName = if (forceUpperCase) "__ON_" else "__on_"
    def nameConverters: Map[String, String] = Map()

    def syntax = {
      val _name = if (forceUpperCase) tableName.toUpperCase else tableName
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }
    def syntax(name: String) = {
      val _name = if (forceUpperCase) name.toUpperCase else name
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }

    def as(provider: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]) = {
      if (tableName == provider.tableAliasName) { SQLSyntax(tableName) }
      else { SQLSyntax(tableName + " " + provider.tableAliasName) }
    }
  }

  /**
   * SQLSyntax Provider
   */
  trait SQLSyntaxProvider extends Dynamic {
    import SQLSyntaxProvider._

    def c(name: String) = column(name)
    def column(name: String): SQLSyntax

    def nameConverters: Map[String, String]
    def forceUpperCase: Boolean
    def delimiterForResultName: String

    def field(name: String): SQLSyntax = {
      val columnName = {
        if (forceUpperCase) toSnakeCase(name, nameConverters).toUpperCase
        else toSnakeCase(name, nameConverters)
      }
      c(columnName)
    }

    def selectDynamic(name: String): SQLSyntax = field(name)
  }

  /**
   * SQLSyntaxProvider companion
   */
  private[scalikejdbc] object SQLSyntaxProvider {

    private val acronymRegExpStr = "[A-Z]{2,}"
    private val acronymRegExp = acronymRegExpStr.r
    private val endsWithAcronymRegExpStr = "[A-Z]{2,}$"
    private val singleUpperCaseRegExp = """[A-Z]""".r

    def toSnakeCase(str: String, nameConverters: Map[String, String] = Map()): String = {
      val convertersApplied = nameConverters.foldLeft(str) { case (s, (from, to)) => s.replaceAll(from, to) }
      var acronymsFiltered = acronymRegExp.replaceAllIn(
        acronymRegExp.findFirstMatchIn(convertersApplied).map { m =>
          convertersApplied.replaceFirst(endsWithAcronymRegExpStr, "_" + m.matched.toLowerCase)
        }.getOrElse(convertersApplied), // might end with an acronym
        { m => "_" + m.matched.init.toLowerCase + "_" + m.matched.last.toString.toLowerCase }
      )
      singleUpperCaseRegExp.replaceAllIn(acronymsFiltered, { m => "_" + m.matched.toLowerCase })
        .replaceFirst("^_", "")
        .replaceFirst("_$", "")
    }
  }

  /**
   * SQLSyntax Provider basic implementation
   */
  private[scalikejdbc] abstract class SQLSyntaxProviderCommonImpl[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String) extends SQLSyntaxProvider {
    def nameConverters = support.nameConverters
    def forceUpperCase = support.forceUpperCase
    def delimiterForResultName = support.delimiterForResultName
    def columns: Seq[SQLSyntax] = support.columns.map { c => if (support.forceUpperCase) c.toUpperCase else c }.map(c => SQLSyntax(c))

    def notFoundInColumns(name: String): IllegalArgumentException = notFoundInColumns(name, columns.map(_.value).mkString(","))

    def notFoundInColumns(name: String, registeredNames: String): IllegalArgumentException = {
      new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})")
    }

  }

  /**
   * SQLSyntax provider for query parts
   */
  case class QuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    def result: ResultSQLSyntaxProvider[S, A] = {
      val table = if (support.forceUpperCase) tableAliasName.toUpperCase else tableAliasName
      ResultSQLSyntaxProvider[S, A](support, table)
    }

    def resultName: ResultNameSQLSyntaxProvider[S, A] = result.name

    def * : SQLSyntax = SQLSyntax(columns.map(c => s"${tableAliasName}.${c.value}").mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${columns.map(_.value).mkString(",")})")
    }

  }

  /**
   * SQLSyntax provider for result parts
   */
  case class ResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    def name: ResultNameSQLSyntaxProvider[S, A] = ResultNameSQLSyntaxProvider[S, A](support, tableAliasName)

    def * : SQLSyntax = SQLSyntax(columns.map { c =>
      s"${tableAliasName}.${c.value} as ${c.value}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value} as ${c.value}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(name))
  }

  /**
   * SQLSyntax provider for result names
   */
  case class ResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    def * : SQLSyntax = SQLSyntax(columns.map { c =>
      s"${c.value}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    def namedColumns: Seq[SQLSyntax] = support.columns.map { columnName: String =>
      SQLSyntax(s"${columnName}${delimiterForResultName}${tableAliasName}")
    }

    def namedColumn(name: String) = namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${namedColumns.map(_.value).mkString(",")})")
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(s"${c.value}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(name))
  }

  // --------------------
  // subquery syntax providers
  // --------------------

  object SubQuery {

    def syntax(name: String, resultNames: ResultNameSQLSyntaxProvider[_, _]*) = {
      SubQuerySQLSyntaxProvider(name, resultNames.head.delimiterForResultName, resultNames)
    }

    def syntax(name: String, delimiterForResultName: String, resultNames: ResultNameSQLSyntaxProvider[_, _]*) = {
      SubQuerySQLSyntaxProvider(name, delimiterForResultName, resultNames)
    }

    def as(subquery: SubQuerySQLSyntaxProvider): SQLSyntax = SQLSyntax(subquery.aliasName)
  }

  case class SubQuerySQLSyntaxProvider(aliasName: String, delimiterForResultName: String, resultNames: Seq[ResultNameSQLSyntaxProvider[_, _]]) {

    def result: SubQueryResultSQLSyntaxProvider = SubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, resultNames)

    def resultName: SubQueryResultNameSQLSyntaxProvider = result.name

    def * : SQLSyntax = SQLSyntax(resultNames.map { resultName =>
      resultName.namedColumns.map { c => s"${aliasName}.${c.value}" }
    }.mkString(", "))

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.value.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${aliasName}.${rn.namedColumn(name).value}")
      }.getOrElse {
        val registeredNames = resultNames.map { rn => rn.columns.map(_.value).mkString(",") }.mkString(",")
        throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name.value}, registered names: ${registeredNames})")
      }
    }

    def apply[S <: SQLSyntaxSupport[A], A](syntax: QuerySQLSyntaxProvider[S, A]): PartialSubQuerySQLSyntaxProvider[S, A] = {
      PartialSubQuerySQLSyntaxProvider(aliasName, delimiterForResultName, syntax.resultName)
    }

  }

  case class SubQueryResultSQLSyntaxProvider(aliasName: String, delimiterForResultName: String, resultNames: Seq[ResultNameSQLSyntaxProvider[_, _]]) {

    def name: SubQueryResultNameSQLSyntaxProvider = SubQueryResultNameSQLSyntaxProvider(aliasName, delimiterForResultName, resultNames)

    def * : SQLSyntax = SQLSyntax(resultNames.map { rn =>
      rn.namedColumns.map { c =>
        s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
      }.mkString(", ")
    }.mkString(", "))

    def column(name: String): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${aliasName}.${rn.column(name)} as ${rn.column(name)}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        val registeredNames = resultNames.map { rn => rn.columns.map(_.value).mkString(",") }.mkString(",")
        throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})")
      }
    }

  }

  case class SubQueryResultNameSQLSyntaxProvider(aliasName: String, delimiterForResultName: String, resultNames: Seq[ResultNameSQLSyntaxProvider[_, _]]) {

    def * : SQLSyntax = SQLSyntax(resultNames.map { rn =>
      rn.namedColumns.map { c =>
        s"${c.value}${delimiterForResultName}${aliasName}"
      }.mkString(", ")
    }.mkString(", "))

    def columns: Seq[SQLSyntax] = resultNames.flatMap { rn =>
      rn.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(name)
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${rn.namedColumn(name).value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(name.value)
      }
    }

    def notFoundInColumns(name: String) = {
      val registeredNames = resultNames.map { rn => rn.namedColumns.map(_.value).mkString(",") }.mkString(",")
      new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})")
    }

  }

  // --------------------
  // partial subquery syntax providers
  // --------------------

  case class PartialSubQuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](aliasName: String, override val delimiterForResultName: String, underlying: ResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    def result: PartialSubQueryResultSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    def resultName: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = result.name

    def * : SQLSyntax = SQLSyntax(resultName.namedColumns.map { c => s"${aliasName}.${c.value}" }.mkString(", "))

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { _ =>
        SQLSyntax(s"${aliasName}.${underlying.namedColumn(name).value}")
      }.getOrElse {
        throw notFoundInColumns(name.value, resultName.columns.map(_.value).mkString(","))
      }
    }

    def column(name: String) = {
      SQLSyntax(s"${aliasName}.${underlying.column(name).value}")
    }

  }

  case class PartialSubQueryResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](aliasName: String, override val delimiterForResultName: String, underlying: ResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    def name: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultNameSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    def * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${aliasName}.${nc.value} as ${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(name, underlying.columns.map(_.value).mkString(","))
      }
    }

  }

  case class PartialSubQueryResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](aliasName: String, override val delimiterForResultName: String, underlying: ResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    def * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      s"${c.value}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    override def columns: Seq[SQLSyntax] = underlying.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }

    def column(name: String): SQLSyntax = underlying.columns.find(_.value.toLowerCase == name.toLowerCase).map { original: SQLSyntax =>
      SQLSyntax(s"${original.value}${delimiterForResultName}${underlying.tableAliasName}${delimiterForResultName}${aliasName}")
    }.getOrElse {
      throw notFoundInColumns(name, underlying.columns.map(_.value).mkString(","))
    }

    def namedColumns: Seq[SQLSyntax] = underlying.namedColumns.map { nc: SQLSyntax =>
      SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
    }

    def namedColumn(name: String) = underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(name, namedColumns.map(_.value).mkString(","))
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(name.value, underlying.columns.map(_.value).mkString(","))
      }
    }

  }

  type ResultName[A] = ResultNameSQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type SubQueryResultName = SubQueryResultNameSQLSyntaxProvider

  @inline implicit def convertSQLSyntaxToString(syntax: SQLSyntax): String = syntax.value
  @inline implicit def interpolation(s: StringContext) = new SQLInterpolation(s)

}

/**
 * SQLInterpolation
 */
class SQLInterpolation(val s: StringContext) extends AnyVal {

  import SQLInterpolation.{ LastParameter, SQLSyntax }

  def sql[A](params: Any*) = {
    val query: String = s.parts.zipAll(params, "", LastParameter).foldLeft("") {
      case (query, (previousQueryPart, param)) => query + previousQueryPart + getPlaceholders(param)
    }
    SQL[A](query).bind(params.flatMap(toSeq): _*)
  }

  private def getPlaceholders(param: Any): String = param match {
    case _: String => "?"
    case t: Traversable[_] => t.map(_ => "?").mkString(", ") // e.g. in clause
    case LastParameter => ""
    case SQLSyntax(s) => s
    case _ => "?"
  }

  private def toSeq(param: Any): Traversable[Any] = param match {
    case s: String => Seq(s)
    case t: Traversable[_] => t
    case SQLSyntax(s) => Nil
    case n => Seq(n)
  }

}
