package scalikejdbc

import scala.quoted._

object autoColumns {

  def camelToSnake(
    fieldName: String,
    nameConverters: Map[String, String],
    useSnakeCase: Boolean
  ): String = {
    SQLSyntaxProvider.toColumnName(fieldName, nameConverters, useSnakeCase)
  }

  def apply_impl[A](
    excludes: Expr[Seq[String]]
  )(using quotes: Quotes)(using Type[A]): Expr[Seq[String]] = {
    import quotes.reflect._

    // SQLSyntaxSupportImpl
    val thi = if (Symbol.spliceOwner.owner.isClassDef) {
      This(Symbol.spliceOwner.owner) // use in SQLSyntaxSupportFactory
    } else {
      This(Symbol.spliceOwner.owner.owner)
    }
    // this.nameConverters
    val nameConverters =
      Select.unique(thi, "nameConverters").asExprOf[Map[String, String]]
    // val nameConverters = Select.unique(This(Symbol.spliceOwner.owner.owner), "nameConverters").asExprOf[Map[String,String]]
    // this.useSnakeCaseColumnName
    val useSnakeCaseColumnName =
      Select.unique(thi, "useSnakeCaseColumnName").asExprOf[Boolean]
    // val useSnakeCaseColumnName = Select.unique(This(Symbol.spliceOwner.owner.owner), "useSnakeCaseColumnName").asExprOf[Boolean]

    val r = EntityUtil.constructorParams[A](excludes).collect {
      case (name, _, false, _) =>
        '{
          scalikejdbc.autoColumns.camelToSnake(
            ${ Expr(name) },
            ${ nameConverters },
            ${ useSnakeCaseColumnName }
          )
        }
    }
    Expr.ofList(r)
  }

  inline def apply[A](inline excludes: String*): collection.Seq[String] = ${
    apply_impl[A]('excludes)
  }

}
