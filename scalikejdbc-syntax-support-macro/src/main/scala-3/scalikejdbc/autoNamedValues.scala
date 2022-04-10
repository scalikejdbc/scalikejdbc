package scalikejdbc

import scala.quoted._
import scalikejdbc.{ ParameterBinderFactory, ParameterBinder }
import language.`3.0`

object autoNamedValues {

  def apply_impl[E](
    entity: Expr[E],
    column: Expr[ColumnName[E]],
    excludes: Expr[Seq[String]]
  )(using
    quotes: Quotes
  )(using t: Type[E]): Expr[Map[SQLSyntax, ParameterBinder]] = {
    import quotes.reflect._
    val toMapParams =
      Expr.ofList(
        EntityUtil.constructorParams(excludes).collect {
          case (name, typeTree, false, _) =>
            val parameterBinderExpr =
              Implicits.search(
                TypeRepr.of[ParameterBinderFactory].appliedTo(typeTree.tpe)
              ) match {
                case result: ImplicitSearchSuccess =>
                  Apply(
                    Select.unique(result.tree, "apply"),
                    Select.unique(entity.asTerm, name) :: Nil
                  ).asExprOf[ParameterBinder]
                case _ =>
                  report.errorAndAbort(
                    s"could not find ParameterBinderFactory[${typeTree.show}]"
                  )
              }

            '{
              ($column.selectDynamic(${ Expr(name) }), ${ parameterBinderExpr })
            }

        }
      )
    '{ ${ toMapParams }.toMap }
  }

  inline def apply[E](
    entity: E,
    column: ColumnName[E],
    inline excludes: String*
  ): Map[SQLSyntax, ParameterBinder] =
    ${ apply_impl('entity, 'column, 'excludes) }
}
