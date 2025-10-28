package scalikejdbc

import scalikejdbc.{ SQLSyntaxSupportImpl, TypeBinder }
import scala.quoted._
import language.`3.0`

object SQLSyntaxSupportFactory {

  def camelToSnake(className: String): String = {
    val clazz = className
      .replaceFirst("\\$$", "")
      .replaceFirst("^.+\\.", "")
      .replaceFirst("^.+\\$", "")
    SQLSyntaxProvider.toColumnName(clazz, Map.empty, true)
  }

  def apply_impl[A](
    excludes: Expr[Seq[String]]
  )(using quotes: Quotes)(using Type[A]): Expr[SQLSyntaxSupportImpl[A]] = {
    import quotes.reflect._
    val typeTree = TypeTree.of[A]
    val tpeSym = typeTree.symbol
    val fields = EntityUtil.constructorParams(excludes)
    val tableNameExpr = Expr(tpeSym.name)
    '{
      new SQLSyntaxSupportImpl[A] {

        override lazy val tableName: String =
          scalikejdbc.SQLSyntaxSupportFactory.camelToSnake(${ tableNameExpr })

        override lazy val columns: Seq[String] = ${
          autoColumns.apply_impl[A](excludes)
        }

        def p(n: String): String = {
          scalikejdbc.autoColumns.camelToSnake(
            n,
            nameConverters,
            useSnakeCaseColumnName
          )
        }
        def apply(rn: ResultName[A])(rs: scalikejdbc.WrappedResultSet): A = {
          ${
            val params = fields.collect {
              case (name, typeTree, false, _) =>
                val typeBinderTree = Implicits.search(
                  TypeRepr.of[TypeBinder].appliedTo(typeTree.tpe)
                ) match {
                  case result: ImplicitSearchSuccess => result.tree
                  case _                             =>
                    report.errorAndAbort(
                      s"could not find implicit of TypeBinder[${typeTree.show}]"
                    )
                }
                val exprs = typeTree.tpe.asType match {
                  case '[b] =>
                    '{
                      ${ typeBinderTree.asExprOf[TypeBinder[b]] }.apply(
                        rs.underlying,
                        rn.field(p(${ Expr(name) })).value
                      )
                    }
                }
                NamedArg(name, exprs.asTerm)
              case (name, _, true, Some(ref)) =>
                NamedArg(name, ref)
            }
            Select.overloaded(New(typeTree), "<init>", Nil, params).asExprOf[A]
          }
        }
      }
    }
  }

  def debug_impl[A](
    excludes: Expr[Seq[String]]
  )(using quotes: Quotes)(using t: Type[A]): Expr[SQLSyntaxSupportImpl[A]] = {
    val expr = apply_impl[A](excludes)
    println(expr.show)
    expr
  }

  inline def apply[A](inline excludes: String*): SQLSyntaxSupportImpl[A] = ${
    apply_impl[A]('excludes)
  }

  inline def debug[A](inline excludes: String*): SQLSyntaxSupportImpl[A] = ${
    debug_impl[A]('excludes)
  }
}
