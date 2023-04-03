package scalikejdbc

import scala.quoted._
import java.sql.ResultSet
import language.`3.0`

object autoConstruct {
  def applyResultName_impl[A](
    rs: Expr[WrappedResultSet],
    rn: Expr[ResultName[A]],
    excludes: Expr[Seq[String]]
  )(using quotes: Quotes)(using t: Type[A]): Expr[A] = {
    import quotes.reflect._
    val params = EntityUtil.constructorParams[A](excludes).collect {
      case (name, typeTree, false, _) =>
        val d = Implicits
          .search(TypeRepr.of[TypeBinder].appliedTo(typeTree.tpe)) match {
          case result: ImplicitSearchSuccess =>
            val resultSet = '{ ${ rs }.underlying }.asTerm
            val fieldName = '{ ${ rn }.field(${ Expr(name) }).value }.asTerm
            Select.overloaded(
              result.tree,
              "apply",
              Nil,
              resultSet :: fieldName :: Nil,
              typeTree.tpe
            )
          case _ =>
            report.errorAndAbort(
              s"could not find implicit of TypeBinder[${typeTree.show}]"
            )
        }
        NamedArg(name, d)
      case (name, _, true, Some(ref)) =>
        NamedArg(name, ref)
    }
    val typeTree = TypeTree.of[A]
    Select.overloaded(New(typeTree), "<init>", Nil, params).asExprOf[A]
  }

  def applySyntaxProvider_impl[A](
    rs: Expr[WrappedResultSet],
    sp: Expr[SQLSyntaxProvider[A]],
    excludes: Expr[Seq[String]]
  )(using quotes: Quotes)(using t: Type[A]): Expr[A] = {
    import quotes.reflect._
    applyResultName_impl(
      rs,
      Select.unique(sp.asTerm, "resultName").asExprOf[ResultName[A]],
      excludes
    )
  }

  inline def apply[A](
    rs: WrappedResultSet,
    sp: SyntaxProvider[A],
    inline excludes: String*
  ): A = ${ applySyntaxProvider_impl('rs, 'sp, 'excludes) }
  inline def apply[A](
    rs: WrappedResultSet,
    rn: ResultName[A],
    inline excludes: String*
  ): A = ${ applyResultName_impl('rs, 'rn, 'excludes) }

}
