package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scalikejdbc.MacroCompatible._

object autoConstruct {

  def applyResultName_impl[A: c.WeakTypeTag](c: Context)(rn: c.Expr[ResultName[A]], rs: c.Expr[WrappedResultSet], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c)(excludes:_*).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"${field.name.toTermName} = $rs.get[$fieldType]($rn.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  def applySyntaxProvider_impl[A: c.WeakTypeTag](c: Context)(sp: c.Expr[SyntaxProvider[A]], rs: c.Expr[WrappedResultSet], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c)(excludes:_*).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"${field.name.toTermName} = $rs.get[$fieldType]($sp.resultName.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  private[this] def constructorParams[A: c.WeakTypeTag](c: Context)(excludes: c.Expr[String]*) = {
    import c.universe._
    val declarations = decls(c)(weakTypeTag[A].tpe)
    val ctor = declarations.collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }.get
    val allParams = paramLists(c)(ctor).head
    val excludeStrs: Set[String] = excludes.map(_.tree).flatMap {
      case Literal(Constant(value: String)) => Some(value)
      case _                                => None
    }.toSet
    val paramsStrs: Set[String] = allParams.map(_.name.decodedName.toString).toSet
    excludeStrs.foreach { ex =>
      if (!paramsStrs(ex)) c.error(c.enclosingPosition, s"$ex does not found in ${weakTypeTag[A].tpe}")
    }
    allParams.filterNot(f => excludeStrs(f.name.decodedName.toString))
  }

  def apply[A](rn: ResultName[A], rs: WrappedResultSet, excludes: String*): A = macro applyResultName_impl[A]

  def apply[A](sp: SyntaxProvider[A], rs: WrappedResultSet, excludes: String*): A = macro applySyntaxProvider_impl[A]

}
