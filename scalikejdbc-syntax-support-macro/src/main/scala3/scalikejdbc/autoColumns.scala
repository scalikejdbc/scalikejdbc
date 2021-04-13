package scalikejdbc

import scala.quoted._
import scala.compiletime._

object autoColumns {

  def camelToSnake(fieldName: String, nameConverters: Map[String, String], useSnakeCase: Boolean): String = {
    SQLSyntaxProvider.toColumnName(fieldName, nameConverters, useSnakeCase)
  }

  def apply_impl[A](excludes:Expr[Seq[String]])(using quotes:Quotes)(using Type[A]):Expr[Seq[String]] = {
    import quotes.reflect._
    // this.nameConverters
    val nameConverters = Select.unique(This(Symbol.spliceOwner.owner.owner), "nameConverters").asExprOf[Map[String,String]]
    // this.useSnakeCaseColumnName
    val useSnakeCaseColumnName = Select.unique(This(Symbol.spliceOwner.owner.owner), "useSnakeCaseColumnName").asExprOf[Boolean]


    val r = EntityUtil.constructorParams[A](excludes).map{
      case (name, _) =>
        val nameExpr = Expr(name)
        '{
          scalikejdbc.autoColumns.camelToSnake(${nameExpr}, ${
            nameConverters
          }, ${
            useSnakeCaseColumnName
          })
        }
    }
    Expr.ofList(r)
  }

  inline def apply[A](inline excludes: String*): collection.Seq[String] = ${apply_impl[A]('excludes)}


}
