package scalikejdbc
import scala.quoted._
import scala.compiletime._
object autoNamedValues {


  //def debug[E](entity: E, column: ColumnName[E], excludes: String*): Map[SQLSyntax, ParameterBinder] = ???

  def apply_impl[E](entity:Expr[E], column:Expr[ColumnName[E]],excludes:Expr[Seq[String]])(using quotes:Quotes)(using t:Type[E]):Expr[Map[SQLSyntax,ParameterBinder]] = {
    import quotes.reflect._
    val toMapParams = EntityUtil.constructorParams(excludes).map {
      case (name,typeTree) =>
        val parameterBinderTree =
          Implicits.search(TypeRepr.of[Conversion].appliedTo(List(typeTree.tpe, TypeRepr.of[ParameterBinder]))) match {
            case result:ImplicitSearchSuccess => result.tree
            case _ => report.throwError(s"could not find implicit of TypeBinder[${typeTree.show}]")
          }
        val implicitExpr = Apply(Select.unique(parameterBinderTree, "apply"), Select.unique(entity.asTerm, name)::Nil).asExprOf[ParameterBinder]
        typeTree.tpe.asType match {
          case '[b] => '{($column.$name, $implicitExpr)}
        }
    }
    '{${Expr.ofList(toMapParams)}.toMap}
  }
  inline def apply[E](entity: E, column: ColumnName[E], inline excludes: String*): Map[SQLSyntax, ParameterBinder] =
    ${apply_impl('entity,'column, 'excludes)}
}
