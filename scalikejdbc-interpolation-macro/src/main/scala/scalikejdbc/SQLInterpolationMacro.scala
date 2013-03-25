package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros._

object SQLInterpolationMacro {

  def validateField[E](name: String): String = macro validateFieldImpl[E]

  def validateFieldImpl[E: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[String] = {
    import c.universe._

    val _name = c.eval(c.Expr[String](c.resetAllAttrs(name.tree.duplicate)))

    // primary constructor args of type E
    val expectedNames = c.weakTypeOf[E].declarations.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.map { const =>
      const.paramss.map { symbols: List[Symbol] => symbols.map(s => s.name.encoded.trim) }.flatten
    }.getOrElse(Nil)

    if (!expectedNames.contains(_name)) {
      c.error(c.enclosingPosition, s"Invalid field name - '${_name}' is not found in ${c.weakTypeOf[E]}'s fields [${expectedNames.mkString("'", ", ", "'")}].")
    }
    name
  }

}
