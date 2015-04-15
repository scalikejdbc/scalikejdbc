package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scalikejdbc.MacroCompatible._

object SQLSyntaxSupportFactory {

  def apply_impl[A: c.WeakTypeTag](c: Context)(excludes: c.Expr[String]*): c.Expr[SQLSyntaxSupportImpl[A]] = {
    import c.universe._
    val constParams = EntityUtil.constructorParams[A](c)("SQLSyntaxSupportFactory", excludes: _*).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"${field.name.toTermName} = rs.get[$fieldType](rn.field($name))"
    }
    c.Expr[SQLSyntaxSupportImpl[A]](q"""
      new SQLSyntaxSupportImpl[${weakTypeTag[A].tpe}] {
        override lazy val tableName: String = {
          SQLSyntaxProvider.toColumnName(
            ${weakTypeOf[A].toString}.replaceFirst("\\$$$$", "").replaceFirst("^.+\\.", "").replaceFirst("^.+\\$$", ""),
            nameConverters, useSnakeCaseColumnName)
        }
        def apply(rn: ResultName[${weakTypeTag[A].tpe}])(rs: WrappedResultSet): ${weakTypeTag[A].tpe} = new ${weakTypeTag[A].tpe}(..$constParams)
      }
    """)
  }

  def debug_impl[A: c.WeakTypeTag](c: Context)(excludes: c.Expr[String]*): c.Expr[SQLSyntaxSupportImpl[A]] = {
    val expr = apply_impl[A](c)(excludes: _*)
    println(expr.tree)
    expr
  }

  def apply[A](excludes: String*): SQLSyntaxSupportImpl[A] = macro apply_impl[A]

  def debug[A](excludes: String*): SQLSyntaxSupportImpl[A] = macro debug_impl[A]

}
