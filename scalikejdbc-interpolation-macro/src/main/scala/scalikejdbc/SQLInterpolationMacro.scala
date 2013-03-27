package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros._

import scalikejdbc.interpolation.SQLSyntax

object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[SQLSyntax] = {
    import c.universe._

    val nameOpt: Option[String] = try {
      Some(c.eval(c.Expr[String](c.resetAllAttrs(name.tree.duplicate))))
    } catch {
      case t: Throwable => None
    }

    // primary constructor args of type E
    val expectedNames = c.weakTypeOf[E].declarations.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.map { const =>
      const.paramss.map { symbols: List[Symbol] => symbols.map(s => s.name.encoded.trim) }.flatten
    }.getOrElse(Nil)

    nameOpt.map { _name =>
      if (!expectedNames.isEmpty && !expectedNames.contains(_name)) {
        c.error(c.enclosingPosition, s"${c.weakTypeOf[E]}#${_name} not found. Expected fields are ${expectedNames.mkString("#", ", #", "")}.")
      }
    }

    c.Expr[SQLSyntax](Apply(Select(c.prefix.tree, newTermName("field")), List(name.tree)))
  }

}

