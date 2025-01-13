package scalikejdbc

import scala.quoted._

object EntityUtil {

  def constructorParams[T](
    excludes: Expr[Seq[String]]
  )(using
    quotes: Quotes
  )(using
    Type[T]
  ): List[
    (String, quotes.reflect.TypeTree, Boolean, Option[quotes.reflect.Ref])
  ] = {
    import quotes.reflect._
    val sym = TypeTree.of[T].symbol
    val primaryConstructor = sym.primaryConstructor
    if primaryConstructor.isNoSymbol then {
      report.errorAndAbort(
        s"Could not find the primary constructor for ${sym.fullName}. type ${sym.fullName} must be a class, not trait or type parameter"
      )
    }
    val excludeNames: Set[String] = (excludes match {
      case Varargs(expr) if (expr.exists(_.value.isEmpty)) =>
        report.errorAndAbort(
          s"You must use String literal values for field names to exclude from case class ${sym.fullName}",
          excludes.asTerm.pos
        )
      case Varargs(expr) =>
        expr.map(_.value.get)
    }).toSet

    val comp = sym.companionClass
    val body = comp.tree.asInstanceOf[ClassDef].body
    val defaultValuePrefix = "$lessinit$greater$default"
    val defaulValues: Map[String, Ref] = body.collect {
      case deff @ DefDef(name, _, _, _)
        if name.startsWith(defaultValuePrefix) =>
        name -> Ref(deff.symbol)
    }.toMap
    primaryConstructor.tree.asInstanceOf[DefDef] match {
      case DefDef(_, params, _, _) =>
        params.head.params.zipWithIndex.map {
          case (ValDef(name, typeTree, _), index) =>
            (
              name,
              typeTree,
              excludeNames.contains(name),
              defaulValues.get(s"$defaultValuePrefix$$${index + 1}")
            )
        }
    }
  }
}
