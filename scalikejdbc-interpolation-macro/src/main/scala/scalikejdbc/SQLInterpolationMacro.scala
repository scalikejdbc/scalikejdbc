package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros._

case class SQLSyntax(value: String, parameters: Seq[Any] = Vector())

object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag, P: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[SQLSyntax] = {
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
        c.error(c.enclosingPosition, s"${c.weakTypeOf[E]}#${_name} not found. Expected: [${expectedNames.mkString("#", ", #", "")}]")
      }
    }

    reify(SQLSyntax(name.splice))

    //def typeIndent[A: TypeTag] = Ident(typeTag[A].tpe.typeSymbol)
    //c.Expr[SQLSyntax](Apply(TypeApply(Select(This(tpnme.EMPTY), newTermName("field")), List(typeIndent[String])), List(name.tree)))
    //c.Expr[SQLSyntax](Apply(TypeApply(Select(Ident(c.mirror.staticClass("SQLSyntaxProvider")), newTermName("field")), List(typeIndent[String])), List(name.tree)))
  }

}

