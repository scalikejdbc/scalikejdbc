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
  case class SQLSyntax(value: String)

  trait SQLSyntaxSupport {
    def tableName: String
    def columns: Seq[String]
    def forceUpperCase: Boolean = false
    def nameConverters: Map[String, String] = Map()
    def syntax() = QuerySyntaxProvider(this, this.tableName)
    def syntax(name: String) = QuerySyntaxProvider(this, if(forceUpperCase) name.toUpperCase else name)
    def as(provider: QuerySyntaxProvider[_]) = {
      if (tableName == provider.tableAliasName) { SQLSyntax(tableName) }
      else { SQLSyntax(tableName + " " + provider.tableAliasName) }
    }
  }

  object SQLSyntaxProvider {

    private val acronymRegExpStr = "[A-Z]{2,}"
    private val acronymRegExp = acronymRegExpStr.r
    private val endsWithAcronymRegExpStr = "[A-Z]{2,}$"
    private val singleUpperCaseRegExp = """[A-Z]""".r

    def toSnakeCase(str: String, nameConverters: Map[String, String] = Map()): String = { 

      val convertersApplied = nameConverters.foldLeft(str) { case (s, (from, to)) => s.replaceAll(from, to) }

      var acronymsFiltered = acronymRegExp.replaceAllIn(
        acronymRegExp.findFirstMatchIn(convertersApplied).map { m =>
          convertersApplied.replaceFirst(endsWithAcronymRegExpStr, "_" + m.matched.toLowerCase) }.getOrElse(convertersApplied), // might end with an acronym
        { m => "_" + m.matched.init.toLowerCase + "_" + m.matched.last.toString.toLowerCase }
      )

      singleUpperCaseRegExp.replaceAllIn(acronymsFiltered, { m => "_" + m.matched.toLowerCase })
        .replaceFirst("^_", "")
        .replaceFirst("_$", "")
    }
  }

  import scala.language.dynamics

  trait SQLSyntaxProvider extends Dynamic { 
    import SQLSyntaxProvider._
    def c(name: String) = column(name)
    def column(name: String): SQLSyntax
    def nameConverters: Map[String, String]
    def forceUpperCase: Boolean
    def selectDynamic(name: String): SQLSyntax = { 
      val nameInSQL = { 
        if (forceUpperCase) toSnakeCase(name, nameConverters).toUpperCase
        else toSnakeCase(name, nameConverters)
      }
      c(nameInSQL)
    }
  }

  abstract class SQLSyntaxProviderBase[A <: SQLSyntaxSupport](underlying: A, tableAliasName: String) extends SQLSyntaxProvider {

    def nameConverters = underlying.nameConverters

    def forceUpperCase = underlying.forceUpperCase

    def columns : Seq[SQLSyntax] = underlying.columns.map { c => if (underlying.forceUpperCase) c.toUpperCase else c }.map(c => SQLSyntax(c))
  }

  case class QuerySyntaxProvider[A <: SQLSyntaxSupport](underlying: A, tableAliasName: String) extends SQLSyntaxProviderBase(underlying, tableAliasName) {

    def result(): ResultSyntaxProvider[A] = {
      ResultSyntaxProvider(underlying, if (underlying.forceUpperCase) tableAliasName.toUpperCase else tableAliasName)
    }

    def * : SQLSyntax = SQLSyntax(columns.map { c => s"${tableAliasName}.${c.value}" }.mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value == name).map { c => 
      SQLSyntax(s"${tableAliasName}.${c.value}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
  }

  case class ResultSyntaxProvider[A <: SQLSyntaxSupport](underlying: A, tableAliasName: String) extends SQLSyntaxProviderBase(underlying, tableAliasName) {

    private def delimiter = if (underlying.forceUpperCase) "__ON__" else "__on__"

    def * : SQLSyntax = SQLSyntax(columns.map { c =>
        s"${tableAliasName}.${c.value} as ${c.value}${delimiter}${tableAliasName}"
      }.mkString(", "))

    def column(name: String): SQLSyntax = columns.find(_.value == name).map { c => 
      SQLSyntax(s"${c.value}${delimiter}${tableAliasName}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
  }

  implicit def convertSQLSyntaxToString(syntax: SQLSyntax): String = syntax.value

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
