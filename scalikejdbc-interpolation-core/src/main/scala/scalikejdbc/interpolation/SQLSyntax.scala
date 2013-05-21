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

  import Implicits._

  def and(parts: SQLSyntax*): SQLSyntax = join(parts, sqls"and")
  def or(parts: SQLSyntax*): SQLSyntax = join(parts, sqls"or")
  def csv(parts: SQLSyntax*): SQLSyntax = join(parts, sqls",")

  def join(parts: Seq[SQLSyntax], delimiter: SQLSyntax): SQLSyntax = parts.foldLeft(sqls"") {
    case (sql, part) if !sql.isEmpty && !part.isEmpty => sqls"${sql} ${delimiter} ${part}"
    case (sql, part) if sql.isEmpty && !part.isEmpty => part
    case (sql, _) => sql
  }

  def groupBy(columns: SQLSyntax*) = sqls"group by ${csv(columns: _*)}"
  def having(condition: SQLSyntax) = sqls"having ${condition}"

  def orderBy(columns: SQLSyntax*) = sqls"order by ${csv(columns: _*)}"
  def asc = sqls"asc"
  def desc = sqls"desc"

  def limit(n: Int) = sqls"limit ${SQLSyntax(n.toString)}"
  def offset(n: Int) = sqls"offset ${SQLSyntax(n.toString)}"

  def where = sqls"where"
  def where(where: SQLSyntax) = sqls"where ${where}"

  def distinct(column: SQLSyntax) = sqls"distinct ${column}"

  def avg(column: SQLSyntax) = sqls"avg(${column})"
  def count = sqls"count(1)"
  def count(column: SQLSyntax) = sqls"count(${column})"
  def min(column: SQLSyntax) = sqls"min(${column})"
  def max(column: SQLSyntax) = sqls"max(${column})"
  def sum(column: SQLSyntax) = sqls"sum(${column})"

  def eq(column: SQLSyntax, value: Any) = sqls"${column} = ${value}"
  def ne(column: SQLSyntax, value: Any) = sqls"${column} <> ${value}"
  def gt(column: SQLSyntax, value: Any) = sqls"${column} > ${value}"
  def ge(column: SQLSyntax, value: Any) = sqls"${column} >= ${value}"
  def lt(column: SQLSyntax, value: Any) = sqls"${column} < ${value}"
  def le(column: SQLSyntax, value: Any) = sqls"${column} <= ${value}"

  def isNull(column: SQLSyntax) = sqls"${column} is null"
  def isNotNull(column: SQLSyntax) = sqls"${column} is not null"
  def between(a: Any, b: Any) = sqls"between ${a} and ${b}"
  def in(column: SQLSyntax, values: Seq[Any]) = sqls"${column} in (${values})"

  def currentTimestamp = sqls"current_timestamp"

  def dual = sqls"dual"

}

