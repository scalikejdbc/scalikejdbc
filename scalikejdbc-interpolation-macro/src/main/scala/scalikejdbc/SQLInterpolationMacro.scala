/*
 * Copyright 2013 Kazuhiro Sera
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
import scala.reflect.macros._

import scalikejdbc.interpolation.SQLSyntax

/**
 * Macros for dynamic fields validation
 */
object SQLInterpolationMacro {

  def selectDynamic[E: c.WeakTypeTag](c: Context)(name: c.Expr[String]): c.Expr[SQLSyntax] = {
    import c.universe._

    val nameOpt: Option[String] = try {
      Some(c.eval(c.Expr[String](c.resetAllAttrs(name.tree.duplicate))))
    } catch {
      case t: Throwable => None
    }

    // primary constructor args of type E
    val expectedNames = c.weakTypeOf[E].declarations.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.map { const =>
      const.paramss.map { symbols: List[Symbol] => symbols.map(s => s.name.encoded.trim) }.flatten
    }.getOrElse(Nil)

    nameOpt.map { _name =>
      if (!expectedNames.isEmpty && !expectedNames.contains(_name)) {
        c.error(c.enclosingPosition, s"${c.weakTypeOf[E]}#${_name} not found. Expected fields are ${expectedNames.mkString("#", ", #", "")}.")
      }
    }

    c.Expr[SQLSyntax](Apply(Select(c.prefix.tree, newTermName("field")), List(name.tree)))
  }

}

