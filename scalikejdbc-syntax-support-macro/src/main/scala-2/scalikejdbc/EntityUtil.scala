package scalikejdbc

import scala.reflect.macros.blackbox.Context

private[scalikejdbc] object EntityUtil {

  private[scalikejdbc] def constructorParams[A: c.WeakTypeTag](
    c: Context
  )(macroName: String, excludes: c.Expr[String]*) = {
    import c.universe._
    val A = weakTypeTag[A].tpe
    val declarations = A.decls
    val ctor = declarations
      .collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }
      .getOrElse {
        c.abort(
          c.enclosingPosition,
          s"Could not find the primary constructor for $A. type $A must be a class, not trait or type parameter"
        )
      }
    val allParams = ctor.paramLists.head
    val excludeStrs: Set[String] = excludes
      .map(_.tree)
      .flatMap {
        case q"${value: String}" => Some(value)
        case m => {
          c.error(
            c.enclosingPosition,
            s"You must use String literal values for field names to exclude from #$macroName's targets. $m could not resolve at compile time."
          )
          None
        }
      }
      .toSet
    val paramsStrs: Set[String] =
      allParams.map(_.name.decodedName.toString).toSet
    excludeStrs.foreach { ex =>
      if (!paramsStrs(ex))
        c.error(
          c.enclosingPosition,
          s"$ex does not found in ${weakTypeTag[A].tpe}"
        )
    }
    allParams.filterNot(f => excludeStrs(f.name.decodedName.toString))
  }

}
