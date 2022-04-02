package scalikejdbc

import scala.reflect.macros.blackbox.Context

/**
 * Macros for dynamic fields validation
 */
object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag](c: Context)(name: c.Tree): c.Tree = {
    import c.universe._

    val nameOpt: Option[String] = name match {
      case Literal(Constant(value: String)) => Some(value)
      case _                                => None
    }

    // primary constructor args of type E
    val expectedNames = c
      .weakTypeOf[E]
      .decls
      .collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }
      .map { const =>
        const.paramLists.flatMap { (symbols: List[Symbol]) =>
          symbols.map(s => s.name.encodedName.toString.trim)
        }
      }
      .getOrElse(Nil)

    nameOpt.foreach { _name =>
      if (expectedNames.nonEmpty && !expectedNames.contains(_name)) {
        c.error(
          c.enclosingPosition,
          s"${c.weakTypeOf[E]}#${_name} not found. Expected fields are ${expectedNames
              .mkString("#", ", #", "")}."
        )
      }
    }

    Apply(Select(c.prefix.tree, TermName("field")), List(name))
  }

}
