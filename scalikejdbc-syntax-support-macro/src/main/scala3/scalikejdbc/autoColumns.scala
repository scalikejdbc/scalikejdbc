package scalikejdbc

import scala.quoted._
import scala.compiletime._

object autoColumns {

  def camelToSnake(fieldName: String, nameConverters: Map[String, String], useSnakeCase: Boolean): String = {
    SQLSyntaxProvider.toColumnName(fieldName, nameConverters, useSnakeCase)
  }

  def apply_impl[A](exclues:Expr[Seq[String]])(using quotes:Quotes)(using Type[A]):Expr[Seq[String]] = {
    import quotes.reflect._
    val r = EntityUtil.constructorParams[A](exclues).map{
      case (name, _) =>
        //TODO: get context of quotes
        scalikejdbc.autoColumns.camelToSnake(name, Map.empty, true)
    }
    Expr(r)
  }

  inline def apply[A](inline excludes: String*): collection.Seq[String] = ${apply_impl[A]('excludes)}


}
