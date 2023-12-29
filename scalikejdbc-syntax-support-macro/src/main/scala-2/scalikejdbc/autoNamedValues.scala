package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object autoNamedValues {
  def apply_impl[E: c.WeakTypeTag](c: Context)(
    entity: c.Expr[E],
    column: c.Expr[ColumnName[E]],
    excludes: c.Expr[String]*
  ): c.Expr[Map[SQLSyntax, ParameterBinder]] = {
    import c.universe._

    val toMapParams: List[c.universe.Tree] =
      EntityUtil.constructorParams[E](c)("autoNamedValues", excludes*).map {
        field =>
          val fieldName = field.name.toTermName

          q"$column.$fieldName -> $entity.$fieldName"
      }

    c.Expr[Map[SQLSyntax, ParameterBinder]](
      q"_root_.scala.collection.immutable.Map(..$toMapParams)"
    )
  }

  def debug_impl[E: c.WeakTypeTag](c: Context)(
    entity: c.Expr[E],
    column: c.Expr[ColumnName[E]],
    excludes: c.Expr[String]*
  ): c.Expr[Map[SQLSyntax, ParameterBinder]] = {
    val expr = apply_impl[E](c)(entity, column, excludes*)
    println(expr.tree)
    expr
  }

  def debug[E](
    entity: E,
    column: ColumnName[E],
    excludes: String*
  ): Map[SQLSyntax, ParameterBinder] = macro debug_impl[E]

  def apply[E](
    entity: E,
    column: ColumnName[E],
    excludes: String*
  ): Map[SQLSyntax, ParameterBinder] = macro apply_impl[E]
}
