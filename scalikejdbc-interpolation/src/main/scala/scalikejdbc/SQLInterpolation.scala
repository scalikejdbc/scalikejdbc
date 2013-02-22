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
    def syntax() = SQLSyntaxProvider(this, this.tableName)
    def syntax(name: String) = SQLSyntaxProvider(this, name)
    def as(provider: SQLSyntaxProvider[_]) = {
      if (tableName == provider.tableAliasName) { SQLSyntax(tableName) }
      else { SQLSyntax(tableName + " " + provider.tableAliasName) }
    }
    def toDBNamingRule(scalaName: String): String = "[A-Z]".r.replaceAllIn(scalaName, "_" + _.matched.toLowerCase)
  }

  import scala.language.dynamics

  case class SQLSyntaxProvider[A <: SQLSyntaxSupport](underlying: A, tableAliasName: String) extends Dynamic {
    def result(): ResultSQLSyntaxProvider[A] = ResultSQLSyntaxProvider(underlying, tableAliasName)
    def * : SQLSyntax = SQLSyntax(underlying.columns.map { name => s"${tableAliasName}.${name}" }.mkString(", "))
    def c(name: String) = column(name)
    def column(name: String): SQLSyntax = underlying.columns.find(_ == name).map {
      _ => SQLSyntax(s"${tableAliasName}.${name}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
    def selectDynamic(name: String): SQLSyntax = c(underlying.toDBNamingRule(name))
  }

  case class ResultSQLSyntaxProvider[A <: SQLSyntaxSupport](underlying: A, tableAliasName: String) extends Dynamic {
    def * : SQLSyntax = SQLSyntax(underlying.columns.map { column =>
        s"${tableAliasName}.${column} as ${column}__on__${tableAliasName}"
      }.mkString(", "))
    def c(name: String) = column(name)
    def column(name: String): SQLSyntax =  underlying.columns.find(_ == name).map{
      _ => SQLSyntax(s"${name}__on__${tableAliasName}")
    }.getOrElse {
      throw new IllegalArgumentException(ErrorMessage.INVALID_COLUMN_NAME + " (" + name + ")")
    }
    def selectDynamic(name: String): SQLSyntax = c(underlying.toDBNamingRule(name))
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
