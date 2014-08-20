package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object autoConstruct {

  def applyResultName_impl[A: c.WeakTypeTag](c: Context)(rn: c.Expr[ResultName[A]], rs: c.Expr[WrappedResultSet]): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"$rs.get[$fieldType]($rn.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  def applySyntaxProvider_impl[A: c.WeakTypeTag](c: Context)(sp: c.Expr[SyntaxProvider[A]], rs: c.Expr[WrappedResultSet]): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"$rs.get[$fieldType]($sp.resultName.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  private[this] def constructorParams[A: c.WeakTypeTag](c: Context) = {
    import c.universe._
    val declarations = weakTypeTag[A].tpe.decls
    val ctor = declarations.collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }.get
    ctor.paramLists.head
  }

  def apply[A](rn: ResultName[A], rs: WrappedResultSet): A = macro applyResultName_impl[A]

  def apply[A](sp: SyntaxProvider[A], rs: WrappedResultSet): A = macro applySyntaxProvider_impl[A]

}
