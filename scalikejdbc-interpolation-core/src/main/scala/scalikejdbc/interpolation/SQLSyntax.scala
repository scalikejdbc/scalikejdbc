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

/**
 * Value as a part of SQL syntax.
 *
 * This value won't be treated as a binding parameter but will be appended as a part of SQL.
 *
 * Note: The constructor should NOT be used by library users at the considerable risk of SQL injection vulnerability.
 * https://github.com/seratch/scalikejdbc/issues/116
 */
class SQLSyntax private[scalikejdbc] (val value: String, val parameters: Seq[Any] = Vector())

/*
 * SQLSyntax companion object
 */
object SQLSyntax {

  // #apply method should NOT be used by library users at the considerable risk of SQL injection vulnerability.
  // https://github.com/seratch/scalikejdbc/issues/116
  private[scalikejdbc] def apply(value: String, parameters: Seq[Any] = Nil) = new SQLSyntax(value, parameters)

  def unapply(syntax: SQLSyntax): Option[(String, Seq[Any])] = Some((syntax.value, syntax.parameters))

}

