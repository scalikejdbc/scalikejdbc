package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object autoConstruct {

  def applyResultName_impl[A: c.WeakTypeTag](c: Context)(
    rs: c.Expr[WrappedResultSet],
    rn: c.Expr[ResultName[A]],
    excludes: c.Expr[String]*
  ): c.Expr[A] = {
    import c.universe._
    val constParams =
      EntityUtil.constructorParams[A](c)("autoConstruct", excludes*).map {
        field =>
          val fieldType = field.typeSignature
          val name = field.name.decodedName.toString
          q"${field.name.toTermName} = $rs.get[$fieldType]($rn.field($name))"
      }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  def applySyntaxProvider_impl[A: c.WeakTypeTag](c: Context)(
    rs: c.Expr[WrappedResultSet],
    sp: c.Expr[SyntaxProvider[A]],
    excludes: c.Expr[String]*
  ): c.Expr[A] = {
    import c.universe._
    applyResultName_impl(c)(
      rs,
      c.Expr[ResultName[A]](q"${sp}.resultName"),
      excludes*
    )
  }

  def applyResultNameDebug[A: c.WeakTypeTag](c: Context)(
    rs: c.Expr[WrappedResultSet],
    rn: c.Expr[ResultName[A]],
    excludes: c.Expr[String]*
  ): c.Expr[A] = {
    val expr = applyResultName_impl[A](c)(rs, rn, excludes*)
    println(expr.tree)
    expr
  }

  def applySyntaxProviderDebug[A: c.WeakTypeTag](c: Context)(
    rs: c.Expr[WrappedResultSet],
    sp: c.Expr[SyntaxProvider[A]],
    excludes: c.Expr[String]*
  ): c.Expr[A] = {
    val expr = applySyntaxProvider_impl[A](c)(rs, sp, excludes*)
    println(expr.tree)
    expr
  }

  def apply[A](rs: WrappedResultSet, rn: ResultName[A], excludes: String*): A =
    macro applyResultName_impl[A]

  def apply[A](
    rs: WrappedResultSet,
    sp: SyntaxProvider[A],
    excludes: String*
  ): A = macro applySyntaxProvider_impl[A]

  def debug[A](rs: WrappedResultSet, rn: ResultName[A], excludes: String*): A =
    macro applyResultNameDebug[A]

  def debug[A](
    rs: WrappedResultSet,
    sp: SyntaxProvider[A],
    excludes: String*
  ): A = macro applySyntaxProviderDebug[A]

}
