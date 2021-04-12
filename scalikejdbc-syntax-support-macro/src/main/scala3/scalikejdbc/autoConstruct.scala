package scalikejdbc
import scala.quoted._
import scala.compiletime._

object autoConstruct {
  def applyResultName_impl[A](rs:Expr[WrappedResultSet], rn:Expr[ResultName[A]], excludes:Expr[Seq[String]])(using quotes:Quotes)(using t:Type[A]):Expr[A] = {
    import quotes.reflect._
    val params = EntityUtil.constructorParams[A](excludes).map {
      case (name,typeTree) =>
        val typeBinderTpe = TypeRepr.of[TypeBinder].appliedTo(typeTree.tpe)
        val d = Implicits.search(typeBinderTpe) match {
          case result:ImplicitSearchSuccess =>
            Apply(Select.unique(result.tree, "apply"), Expr(name).asTerm::Nil)
          case _ => report.throwError(s"could not find implicit of TypeBinder[${typeTree.show}]")
        }
        NamedArg(name, d)
    }
    Apply(Select.unique(New(TypeTree.of[A]), "<init>"), params).asExprOf[A]
  }

  def applySyntaxProvider_impl[A](rs:Expr[WrappedResultSet], sp:Expr[SQLSyntaxProvider[A]], excludes:Expr[Seq[String]])(using quotes:Quotes)(using t:Type[A]):Expr[A] = {
    import quotes.reflect._
    applyResultName_impl(rs, Select.unique(sp.asTerm, "resultNames").asExprOf[ResultName[A]], excludes)
  }

  inline def apply[A](rs: WrappedResultSet, sp: SyntaxProvider[A], inline excludes: String*):A = ${applySyntaxProvider_impl('rs,'sp, 'excludes)}
  inline def apply[A](rs: WrappedResultSet, rn: ResultName[A], inline excludes: String*): A = ${applyResultName_impl('rs,'rn,'excludes)}

}
