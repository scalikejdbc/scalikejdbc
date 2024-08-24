package scalikejdbc

import scalikejdbc.interpolation.SQLSyntax
import scala.quoted.*

trait SelectDynamicMacro[A] {
  self: SQLSyntaxSupportFeature#SQLSyntaxProvider[A] =>
  inline def selectDynamic(inline name: String): SQLSyntax =
    select[A](self, name)

  inline def select[E](
    ref: SQLSyntaxSupportFeature#SQLSyntaxProvider[A],
    inline name: String
  ): SQLSyntax =
    ${ SelectDynamicMacroImpl.selectImpl[E]('ref, 'name) }
}

object SelectDynamicMacroImpl {
  def selectImpl[E: Type](
    ref: Expr[SQLSyntaxSupportFeature#SQLSyntaxProvider[?]],
    name: Expr[String]
  )(using quotes: Quotes): Expr[SQLSyntax] = {
    import quotes.reflect.*

    val typeSymbol = TypeRepr.of[E].typeSymbol
    val expectedNames = typeSymbol.caseFields.map(_.name)
    name.value.foreach { _name =>
      if (expectedNames.nonEmpty && !expectedNames.contains(_name)) {
        report.error(
          s"${typeSymbol.fullName}#${_name} not found. Expected fields are ${expectedNames
              .mkString("#", ", #", "")}",
          name.asTerm.pos
        )
      }
    }
    '{ $ref.field($name) }
  }
}
