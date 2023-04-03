package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object autoColumns {

  def apply_impl[A: c.WeakTypeTag](
    c: Context
  )(excludes: c.Expr[String]*): c.Expr[Seq[String]] = {
    import c.universe._
    val columns =
      EntityUtil.constructorParams[A](c)("autoColumns", excludes: _*).map {
        field =>
          q"scalikejdbc.autoColumns.camelToSnake(${field.name.decodedName.toString}, nameConverters, useSnakeCaseColumnName)"
      }
    c.Expr[Seq[String]](q"Seq(..$columns)")
  }

  def camelToSnake(
    fieldName: String,
    nameConverters: Map[String, String],
    useSnakeCase: Boolean
  ): String = {
    SQLSyntaxProvider.toColumnName(fieldName, nameConverters, useSnakeCase)
  }

  def debug_impl[A: c.WeakTypeTag](
    c: Context
  )(excludes: c.Expr[String]*): c.Expr[Seq[String]] = {
    val expr = apply_impl[A](c)(excludes: _*)
    println(expr.tree)
    expr
  }

  def apply[A](excludes: String*): collection.Seq[String] = macro apply_impl[A]

  def debug[A](excludes: String*): collection.Seq[String] = macro debug_impl[A]

}
