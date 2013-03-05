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
  case class SQLSyntax(value: String, parameters: Seq[Any] = Vector())

  private[scalikejdbc] val SQLSyntaxSupportLoadedColumns = new scala.collection.concurrent.TrieMap[String, Seq[String]]()

  /**
   * SQLSyntax support utilities
   */
  trait SQLSyntaxSupport[A] {

    def tableName: String
    def columns: Seq[String] = SQLSyntaxSupportLoadedColumns.getOrElseUpdate(tableName, DB.getColumnNames(tableName).map(_.toLowerCase))

    def forceUpperCase: Boolean = false
    def useShortenedResultName: Boolean = true
    def delimiterForResultName = if (forceUpperCase) "_ON_" else "_on_"
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

    val nameConverters: Map[String, String]
    val forceUpperCase: Boolean
    val delimiterForResultName: String

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
      val acronymsFiltered = acronymRegExp.replaceAllIn(
        acronymRegExp.findFirstMatchIn(convertersApplied).map { m =>
          convertersApplied.replaceFirst(endsWithAcronymRegExpStr, "_" + m.matched.toLowerCase)
        }.getOrElse(convertersApplied), // might end with an acronym
        { m => "_" + m.matched.init.toLowerCase + "_" + m.matched.last.toString.toLowerCase }
      )
      singleUpperCaseRegExp.replaceAllIn(acronymsFiltered, { m => "_" + m.matched.toLowerCase })
        .replaceFirst("^_", "")
        .replaceFirst("_$", "")
    }

    def toShortenedName(name: String, columns: Seq[String]): String = {
      val shortenedName = name.split("_").map(word => word.take(1)).mkString
      val shortenedNames = columns.map(_.split("_").map(word => word.take(1)).mkString)
      if (shortenedNames.filter(_ == shortenedName).size > 1) {
        val (n, found) = columns.zip(shortenedNames).foldLeft((1, false)) {
          case ((n, found), (column, shortened)) =>
            if (found) (n, found)
            else if (column == name) (n, true)
            else if (shortened == shortenedName) (n + 1, false)
            else (n, found)
        }
        if (!found) throw new IllegalStateException("This must be a library bug.")
        else shortenedName + n
      } else {
        shortenedName
      }
    }
  }

  /**
   * SQLSyntax Provider basic implementation
   */
  private[scalikejdbc] abstract class SQLSyntaxProviderCommonImpl[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProvider {

    val nameConverters = support.nameConverters
    val forceUpperCase = support.forceUpperCase
    val delimiterForResultName = support.delimiterForResultName
    val columns: Seq[SQLSyntax] = support.columns.map { c => if (support.forceUpperCase) c.toUpperCase else c }.map(c => SQLSyntax(c))

    def notFoundInColumns(aliasName: String, name: String): InvalidColumnNameException = notFoundInColumns(aliasName, name, columns.map(_.value).mkString(","))

    def notFoundInColumns(aliasName: String, name: String, registeredNames: String): InvalidColumnNameException = {
      new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})")
    }

  }

  /**
   * SQLSyntax provider for query parts
   */
  case class QuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    val result: ResultSQLSyntaxProvider[S, A] = {
      val table = if (support.forceUpperCase) tableAliasName.toUpperCase else tableAliasName
      ResultSQLSyntaxProvider[S, A](support, table)
    }

    val resultName: BasicResultNameSQLSyntaxProvider[S, A] = result.name

    val * : SQLSyntax = SQLSyntax(columns.map(c => s"${tableAliasName}.${c.value}").mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value}")
    }.getOrElse {
      throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME +
        s" (name: ${tableAliasName}.${name}, registered names: ${columns.map(_.value).mkString(",")})")
    }

  }

  /**
   * SQLSyntax provider for result parts
   */
  case class ResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {
    import SQLSyntaxProvider._

    val name: BasicResultNameSQLSyntaxProvider[S, A] = BasicResultNameSQLSyntaxProvider[S, A](support, tableAliasName)

    val * : SQLSyntax = SQLSyntax(columns.map { c =>
      val name = if (support.useShortenedResultName) toShortenedName(c.value, support.columns) else c.value
      s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      val name = if (support.useShortenedResultName) toShortenedName(c.value, support.columns) else c.value
      SQLSyntax(s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(tableAliasName, name))
  }

  /**
   * SQLSyntax provider for result names
   */
  trait ResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A] extends SQLSyntaxProvider {
    def * : SQLSyntax
    def namedColumns: Seq[SQLSyntax]
    def namedColumn(name: String): SQLSyntax
    def column(name: String): SQLSyntax
  }

  /**
   * Basic Query SQLSyntax Provider for result names
   */
  case class BasicResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) with ResultNameSQLSyntaxProvider[S, A] {
    import SQLSyntaxProvider._

    val * : SQLSyntax = SQLSyntax(columns.map { c =>
      val name = if (support.useShortenedResultName) toShortenedName(c.value, support.columns) else c.value
      s"${name}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    val namedColumns: Seq[SQLSyntax] = support.columns.map { columnName: String =>
      val name = if (support.useShortenedResultName) toShortenedName(columnName, support.columns) else columnName
      SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
    }

    def namedColumn(name: String) = namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME +
        s" (name: ${name}, registered names: ${namedColumns.map(_.value).mkString(",")})")
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      val name = if (support.useShortenedResultName) toShortenedName(c.value, support.columns) else c.value
      SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(tableAliasName, name))
  }

  // --------------------
  // subquery syntax providers
  // --------------------

  object SubQuery {

    def syntax(name: String, resultNames: BasicResultNameSQLSyntaxProvider[_, _]*) = {
      SubQuerySQLSyntaxProvider(name, resultNames.head.delimiterForResultName, resultNames)
    }

    def syntax(name: String, delimiterForResultName: String, resultNames: BasicResultNameSQLSyntaxProvider[_, _]*) = {
      SubQuerySQLSyntaxProvider(name, delimiterForResultName, resultNames)
    }

    def as(subquery: SubQuerySQLSyntaxProvider): SQLSyntax = SQLSyntax(subquery.aliasName)
  }

  case class SubQuerySQLSyntaxProvider(
      aliasName: String,
      delimiterForResultName: String,
      resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]]) {

    val result: SubQueryResultSQLSyntaxProvider = SubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, resultNames)
    val resultName: SubQueryResultNameSQLSyntaxProvider = result.name

    val * : SQLSyntax = SQLSyntax(resultNames.map { resultName =>
      resultName.namedColumns.map { c =>
        s"${aliasName}.${c.value}"
      }
    }.mkString(", "))

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.value.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${aliasName}.${rn.namedColumn(name).value}")
      }.getOrElse {
        val registeredNames = resultNames.map { rn => rn.columns.map(_.value).mkString(",") }.mkString(",")
        throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name.value}, registered names: ${registeredNames})")
      }
    }

    def apply[S <: SQLSyntaxSupport[A], A](syntax: QuerySQLSyntaxProvider[S, A]): PartialSubQuerySQLSyntaxProvider[S, A] = {
      PartialSubQuerySQLSyntaxProvider(aliasName, delimiterForResultName, syntax.resultName)
    }

  }

  case class SubQueryResultSQLSyntaxProvider(
      aliasName: String,
      delimiterForResultName: String,
      resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]]) {

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
        throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})")
      }
    }

  }

  case class SubQueryResultNameSQLSyntaxProvider(
      aliasName: String,
      delimiterForResultName: String,
      resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]]) {

    val * : SQLSyntax = SQLSyntax(resultNames.map { rn =>
      rn.namedColumns.map { c =>
        s"${c.value}${delimiterForResultName}${aliasName}"
      }.mkString(", ")
    }.mkString(", "))

    val columns: Seq[SQLSyntax] = resultNames.flatMap { rn =>
      rn.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(aliasName, name)
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${rn.namedColumn(name).value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value)
      }
    }

    def notFoundInColumns(aliasName: String, name: String) = {
      val registeredNames = resultNames.map { rn => rn.namedColumns.map(_.value).mkString(",") }.mkString(",")
      new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})")
    }

  }

  // --------------------
  // partial subquery syntax providers
  // --------------------

  case class PartialSubQuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    val result: PartialSubQueryResultSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    val resultName: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = result.name

    val * : SQLSyntax = SQLSyntax(resultName.namedColumns.map { c => s"${aliasName}.${c.value}" }.mkString(", "))

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { _ =>
        SQLSyntax(s"${aliasName}.${underlying.namedColumn(name).value}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value, resultName.columns.map(_.value).mkString(","))
      }
    }

    def column(name: String) = {
      SQLSyntax(s"${aliasName}.${underlying.column(name).value}")
    }

  }

  case class PartialSubQueryResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    val name: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultNameSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    val * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${aliasName}.${nc.value} as ${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name, underlying.columns.map(_.value).mkString(","))
      }
    }

  }

  case class PartialSubQueryResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) with ResultNameSQLSyntaxProvider[S, A] {
    import SQLSyntaxProvider._

    val * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      val name = if (underlying.support.useShortenedResultName) toShortenedName(c.value, underlying.support.columns) else c.value
      s"${name}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    override val columns: Seq[SQLSyntax] = underlying.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }

    def column(name: String): SQLSyntax = underlying.columns.find(_.value.toLowerCase == name.toLowerCase).map { original: SQLSyntax =>
      val name = if (underlying.support.useShortenedResultName) toShortenedName(original.value, underlying.support.columns) else original.value
      SQLSyntax(s"${name}${delimiterForResultName}${underlying.tableAliasName}${delimiterForResultName}${aliasName}")
    }.getOrElse {
      throw notFoundInColumns(aliasName, name, underlying.columns.map(_.value).mkString(","))
    }

    val namedColumns: Seq[SQLSyntax] = underlying.namedColumns.map { nc: SQLSyntax =>
      SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
    }

    def namedColumn(name: String) = underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(aliasName, name, namedColumns.map(_.value).mkString(","))
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value, underlying.columns.map(_.value).mkString(","))
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
    val syntax = sqls(params: _*)
    SQL[A](syntax.value).bind(syntax.parameters: _*)
  }

  def sqls(params: Any*) = {
    val query: String = s.parts.zipAll(params, "", LastParameter).foldLeft("") {
      case (query, (previousQueryPart, param)) => query + previousQueryPart + getPlaceholders(param)
    }
    SQLSyntax(query, params.flatMap(toSeq))
  }

  private def getPlaceholders(param: Any): String = param match {
    case _: String => "?"
    case t: Traversable[_] => t.map(_ => "?").mkString(", ") // e.g. in clause
    case LastParameter => ""
    case SQLSyntax(s, _) => s
    case _ => "?"
  }

  private def toSeq(param: Any): Traversable[Any] = param match {
    case s: String => Seq(s)
    case t: Traversable[_] => t
    case SQLSyntax(_, params) => params
    case n => Seq(n)
  }

}
