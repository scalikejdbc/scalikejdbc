package scalikejdbc

import scala.quoted._
import scala.compiletime._

object EntityUtil {
  def constructorParams[A](excludes: Expr[Seq[String]])(using quotes:Quotes)(using t:Type[A]):List[(String, quotes.reflect.TypeTree)] = {
    import quotes.reflect._
    val tpeSym = TypeTree.of[A].symbol
    if(!tpeSym.flags.is(Flags.Case)) {
      report.throwError(s"${tpeSym.fullName} is not case class")
    }

    val excludeNames:Set[String] = (excludes match {
      case Varargs(expr) if (expr.exists(_.value.isEmpty)) =>
        report.throwError(s"You must use String literal values for field names to exclude from case class ${tpeSym.fullName}", excludes.asTerm.pos)
      case Varargs(expr) =>
        expr.map(_.value.get)
    }).toSet

    tpeSym.caseFields.map(_.tree).collect {
      case ValDef(name, typeTree, _) if !excludeNames(name) =>
        (name, typeTree)
    }
  }
}