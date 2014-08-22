package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scalikejdbc.MacroCompatible._

object autoConstruct {

  def applyResultName_impl[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], rn: c.Expr[ResultName[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c)(excludes:_*).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"${field.name.toTermName} = $rs.get[$fieldType]($rn.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  def applySyntaxProvider_impl[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], sp: c.Expr[SyntaxProvider[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    applyResultName_impl(c)(rs, c.Expr[ResultName[A]](q"${sp}.resultName"), excludes:_*)
  }

  private[this] def constructorParams[A: c.WeakTypeTag](c: Context)(excludes: c.Expr[String]*) = {
    import c.universe._
    val declarations = decls(c)(weakTypeTag[A].tpe)
    val ctor = declarations.collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }.get
    val allParams = paramLists(c)(ctor).head
    val excludeStrs: Set[String] = excludes.map(_.tree).flatMap {
      case q"${value: String}" => Some(value)
      case m                   => {
        c.error(c.enclosingPosition, s"You must use String literal values for field names to exclude from #autoConstruct's targets. $m could not resolve at compile time.")
        None
      }
    }.toSet
    val paramsStrs: Set[String] = allParams.map(_.name.decodedName.toString).toSet
    excludeStrs.foreach { ex =>
      if (!paramsStrs(ex)) c.error(c.enclosingPosition, s"$ex does not found in ${weakTypeTag[A].tpe}")
    }
    allParams.filterNot(f => excludeStrs(f.name.decodedName.toString))
  }

  def apply[A](rs: WrappedResultSet, rn: ResultName[A], excludes: String*): A = macro applyResultName_impl[A]

  def apply[A](rs: WrappedResultSet, sp: SyntaxProvider[A], excludes: String*): A = macro applySyntaxProvider_impl[A]

}
