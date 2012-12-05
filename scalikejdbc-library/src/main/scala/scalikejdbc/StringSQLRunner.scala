/*
 * Copyright 2012 Kazuhiro Sera
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

/**
 * String SQL Runner
 *
 * Basic Usage:
 *
 * {{{
 * import scalikejdbc.StringSQLRunner._
 *
 * val result: List[Map[String, Any]] = "insert into users values (1, 'Alice')".run()
 *
 * val users: List[Map[String, Any]] = "select * from users".run()
 * }}}
 *
 * @param sql SQL value
 */
case class StringSQLRunner(sql: String) {

  /**
   * Runs all SQL and returns result as List[Map[String, Any]]
   * @param session DB Session
   * @return results as List[Map]
   */
  def run()(implicit session: DBSession = AutoSession): List[Map[String, Any]] = try {
    SQL(sql).map(_.toMap()).list.apply()
  } catch {
    case e: java.sql.SQLException =>
      val result = SQL(sql).execute.apply()
      List(Map("RESULT" -> result))
  }

  /**
   * Returns SQL results as List[A]
   *
   * @tparam A value type
   * @return results as List[A]
   */
  def asList[A]: List[A] = run().map(m => m.apply(m.keys.head).asInstanceOf[A])

  /**
   * Returns SQL result as single value
   *
   * @tparam A value type
   * @return a single result as A
   */
  def as[A]: A = asList[A].head

  /**
   * Returns SQL result as single value optionally
   *
   * @tparam A value type
   * @return a single result as A optionally
   */
  def asOption[A]: Option[A] = asList[A].headOption

}

object StringSQLRunner {

  /**
   * Converts String to SQLRunner implicitly
   *
   * @param sql SQL string
   * @return SQLRunner
   */
  implicit def stringToSQLRunner(sql: String) = StringSQLRunner(sql)

}
