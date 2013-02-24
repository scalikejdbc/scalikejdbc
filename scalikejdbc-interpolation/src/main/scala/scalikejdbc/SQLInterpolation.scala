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
    def delimiterForResultName = if (forceUpperCase) "__ON__" else "__on__"
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

    def * : SQLSyntax = SQLSyntax(columns.map(c => s"${tableAliasName}.${c.value}").mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value == name).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
  }

  /**
   * SQLSyntax provider for result parts
   */
  case class ResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    def names: ResultNameSQLSyntaxProvider[S, A] = ResultNameSQLSyntaxProvider[S, A](support, tableAliasName)

    def * : SQLSyntax = SQLSyntax(columns.map { c =>
      s"${tableAliasName}.${c.value} as ${c.value}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value == name).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value} as ${c.value}${delimiterForResultName}${tableAliasName}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
  }

  /**
   * SQLSyntax provider for result names
   */
  case class ResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {

    def column(name: String): SQLSyntax = columns.find(_.value == name).map { c =>
      SQLSyntax(s"${c.value}${delimiterForResultName}${tableAliasName}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
  }

  type ResultName[A] = ResultNameSQLSyntaxProvider[SQLSyntaxSupport[A], A]

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
