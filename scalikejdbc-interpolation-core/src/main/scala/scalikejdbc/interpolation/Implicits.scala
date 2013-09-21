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
package scalikejdbc.interpolation

import scala.language.implicitConversions

/**
 * object to import.
 */
object Implicits extends Implicits

/**
 * Implicit conversion imports.
 */
trait Implicits {

  /**
   * Enables sql"", sqls"" interpolation.
   *
   * {{{
   *   sql"select * from memebrs"
   *   val whereClause = sqls"where id = ${id}"
   *   sql"select * from members ${whereClause}"
   * }}}
   */
  @inline implicit def scalikejdbcSQLInterpolationImplicitDef(s: StringContext) = new scalikejdbc.SQLInterpolationString(s)

  /**
   * Returns String value when String type is expected for [[scalikejdbc.WrappedResultset]].
   *
   * {{{
   *   val c = Company.syntax("c").resultName
   *   rs.string(c.name)
   * }}}
   */
  @inline implicit def scalikejdbcSQLSyntaxToStringImplicitDef(syntax: scalikejdbc.interpolation.SQLSyntax): String = syntax.value

}

