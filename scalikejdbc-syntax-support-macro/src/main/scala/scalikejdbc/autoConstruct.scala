/*
 * Copyright 2011 - 2015 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import scalikejdbc.MacroCompatible._

object autoConstruct {

  def applyResultName_impl[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], rn: c.Expr[ResultName[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    val constParams = constructorParams[A](c)(excludes: _*).map { field =>
      val fieldType = field.typeSignature
      val name = field.name.decodedName.toString
      q"${field.name.toTermName} = $rs.get[$fieldType]($rn.field($name))"
    }
    c.Expr[A](q"new ${weakTypeTag[A].tpe}(..$constParams)")
  }

  def applySyntaxProvider_impl[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], sp: c.Expr[SyntaxProvider[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    import c.universe._
    applyResultName_impl(c)(rs, c.Expr[ResultName[A]](q"${sp}.resultName"), excludes: _*)
  }

  def applyResultNameDebug[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], rn: c.Expr[ResultName[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    val expr = applyResultName_impl[A](c)(rs, rn, excludes: _*)
    println(expr.tree)
    expr
  }

  def applySyntaxProviderDebug[A: c.WeakTypeTag](c: Context)(rs: c.Expr[WrappedResultSet], sp: c.Expr[SyntaxProvider[A]], excludes: c.Expr[String]*): c.Expr[A] = {
    val expr = applySyntaxProvider_impl[A](c)(rs, sp, excludes: _*)
    println(expr.tree)
    expr
  }

  private[this] def constructorParams[A: c.WeakTypeTag](c: Context)(excludes: c.Expr[String]*) = {
    import c.universe._
    val A = weakTypeTag[A].tpe
    val declarations = decls(c)(A)
    val ctor = declarations.collectFirst { case m: MethodSymbol if m.isPrimaryConstructor => m }.getOrElse {
      c.abort(c.enclosingPosition, s"Could not find the primary constructor for $A. type $A must be a class, not trait or type parameter")
    }
    val allParams = paramLists(c)(ctor).head
    val excludeStrs: Set[String] = excludes.map(_.tree).flatMap {
      case q"${ value: String }" => Some(value)
      case m => {
        c.error(c.enclosingPosition, s"You must use String literal values for field names to exclude from #autoConstruct's targets. $m could not resolve at compile time.")
        None
      }
    }.toSet
    val paramsStrs: Set[String] = allParams.map(_.name.decodedName.toString).toSet
    excludeStrs.foreach { ex =>
      if (!paramsStrs(ex)) c.error(c.enclosingPosition, s"$ex does not found in ${weakTypeTag[A].tpe}")
    }
    allParams.filterNot(f => excludeStrs(f.name.decodedName.toString))
  }

  def apply[A](rs: WrappedResultSet, rn: ResultName[A], excludes: String*): A = macro applyResultName_impl[A]

  def apply[A](rs: WrappedResultSet, sp: SyntaxProvider[A], excludes: String*): A = macro applySyntaxProvider_impl[A]

  def debug[A](rs: WrappedResultSet, rn: ResultName[A], excludes: String*): A = macro applyResultNameDebug[A]

  def debug[A](rs: WrappedResultSet, sp: SyntaxProvider[A], excludes: String*): A = macro applySyntaxProviderDebug[A]

}
