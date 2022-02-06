package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

object SQLSyntaxSupportFactory {

  def apply_impl[A: c.WeakTypeTag](
    c: Context
  )(excludes: c.Expr[String]*): c.Expr[SQLSyntaxSupportImpl[A]] = {
    import c.universe._
    val constParams = EntityUtil
      .constructorParams[A](c)("SQLSyntaxSupportFactory", excludes: _*)
      .map { field =>
        val fieldType = field.typeSignature
        val name = field.name.decodedName.toString
        q"${field.name.toTermName} = rs.get[$fieldType](rn.field($name))"
      }
    c.Expr[SQLSyntaxSupportImpl[A]](q"""
      new scalikejdbc.SQLSyntaxSupportImpl[${weakTypeTag[A].tpe}] {
        override lazy val tableName: String = scalikejdbc.SQLSyntaxSupportFactory.camelToSnake(${weakTypeOf[
        A
      ].toString})
        override lazy val columns: collection.Seq[String] = ${autoColumns
        .apply_impl(c)(excludes: _*)}
        def apply(rn: scalikejdbc.ResultName[${weakTypeTag[
        A
      ].tpe}])(rs: scalikejdbc.WrappedResultSet): ${weakTypeTag[
        A
      ].tpe} = new ${weakTypeTag[A].tpe}(..$constParams)
      }
    """)
  }

  def camelToSnake(className: String): String = {
    val clazz = className
      .replaceFirst("\\$$", "")
      .replaceFirst("^.+\\.", "")
      .replaceFirst("^.+\\$", "")
    SQLSyntaxProvider.toColumnName(clazz, Map.empty, true)
  }

  def debug_impl[A: c.WeakTypeTag](
    c: Context
  )(excludes: c.Expr[String]*): c.Expr[SQLSyntaxSupportImpl[A]] = {
    val expr = apply_impl[A](c)(excludes: _*)
    println(expr.tree)
    expr
  }

  def apply[A](excludes: String*): SQLSyntaxSupportImpl[A] = macro apply_impl[A]

  def debug[A](excludes: String*): SQLSyntaxSupportImpl[A] = macro debug_impl[A]

}
