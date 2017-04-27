package scalikejdbc

import scala.reflect.macros._

import scalikejdbc.interpolation.SQLSyntax

/**
 * Macros for dynamic fields validation
 */
object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[SQLSyntax] = {
    import c.universe._

    val nameOpt: Option[String] = name.tree match {
      case Literal(Constant(value: String)) => Some(value)
      case _ => None
    }

    // primary constructor args of type E
    val expectedNames = c.weakTypeOf[E].declarations.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.map { const =>
      const.paramss.map { symbols: List[Symbol] => symbols.map(s => s.name.encoded.trim) }.flatten
    }.getOrElse(Nil)

    nameOpt.map { _name =>
      if (expectedNames.nonEmpty && !expectedNames.contains(_name)) {
        c.error(c.enclosingPosition, s"${c.weakTypeOf[E]}#${_name} not found. Expected fields are ${expectedNames.mkString("#", ", #", "")}.")
      }
    }

    c.Expr[SQLSyntax](Apply(Select(c.prefix.tree, newTermName("field")), List(name.tree)))
  }

}

