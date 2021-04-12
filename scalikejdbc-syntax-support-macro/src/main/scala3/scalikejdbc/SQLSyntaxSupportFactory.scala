package scalikejdbc

import scalikejdbc.{SQLSyntaxSupportImpl, TypeBinder}
import scala.quoted._

object SQLSyntaxSupportFactory {

  def camelToSnake(className: String): String = {
    val clazz = className.replaceFirst("\\$$", "").replaceFirst("^.+\\.", "").replaceFirst("^.+\\$", "")
    SQLSyntaxProvider.toColumnName(clazz, Map.empty, true)
  }

  def apply_impl[A](excludes:Expr[Seq[String]])(using quotes:Quotes)(using t:Type[A]):Expr[SQLSyntaxSupportImpl[A]] = {
    import quotes.reflect._
    val tpeSym = TypeTree.of[A].symbol
    val excludeNames:Expr[List[String]] = Expr.ofList(excludes match {
      case Varargs(expr) if expr.exists(_.value.isEmpty) =>
        report.throwError(s"You must use String literal values for field names to exclude from case class ${tpeSym.fullName}", excludes.asTerm.pos)
      case Varargs(expr) =>
        expr
    })
    val fields = EntityUtil.constructorParams(excludes)
    '{
      new SQLSyntaxSupportImpl[A] {

        lazy val tableName:String = scalikejdbc.SQLSyntaxSupportFactory.camelToSnake(${Expr(tpeSym.name)})

        lazy val columns:Seq[String] = ${excludeNames}.map(v => scalikejdbc.autoColumns.camelToSnake(v, nameConverters, useSnakeCaseColumnName))

        def apply(rn: scalikejdbc.ResultName[A], rs: scalikejdbc.WrappedResultSet):A = {
          ${
            Apply(Select.unique(New(TypeTree.of[A]), "<init>"), fields.map{case (name, typeTree) => {
              val typeBinderTree = Implicits.search(TypeRepr.of[TypeBinder].appliedTo(typeTree.tpe)) match {
                case result:ImplicitSearchSuccess => result.tree
                case _ => report.throwError(s"could not find implicit of TypeBinder[${typeTree.show}]")
              }
              val exprs = typeTree.tpe.asType match {
                case '[b] => '{rs.get[b](rn.field(${Expr(name)}))(using ${typeBinderTree.asExprOf[TypeBinder[b]]})}
              }
              NamedArg(name, exprs.asTerm)
            }}).asExprOf[A]
          }
        }
      }
    }
  }

  def debug_impl[A](excludes:Expr[Seq[String]])(using quotes:Quotes)(using t:Type[A]):Expr[SQLSyntaxSupportImpl[A]] = {
    val expr = apply_impl[A](excludes)
    println(expr.show)
    expr
  }

  inline def apply[A](inline excludes:String*):SQLSyntaxSupportImpl[A] = ${apply_impl[A]('excludes)}

  inline def debug[A](inline excludes:String*):SQLSyntaxSupportImpl[A] = ${debug_impl[A]('excludes)}
}
