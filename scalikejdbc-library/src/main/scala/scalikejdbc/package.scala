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
/**
 * ScalikeJDBC - SQL-Based DB Access Library for Scala
 *
 * Just write SQL:
 *
 * ScalikeJDBC is a SQL-based DB access library for Scala developers.
 * This library naturally wraps JDBC APIs and provides you easy-to-use APIs.
 * Users do nothing other than writing SQL and mapping from java.sql.ResultSet objects to Scala values.
 *
 * Basic Usage:
 *
 * Using [[scalikejdbc.DBSession]]:
 *
 * {{{
 * import scalikejdbc._
 * import org.joda.time.DateTime
 * case class User(id: Long, name: String, birthday: Option[DateTime])
 *
 * val activeUsers: List[User] = DB readOnly { session =>
 *   session.list("select * from users where active = ?", true) { rs =>
 *     User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toJodaDateTime))
 *   }
 * }
 * }}}
 *
 * Using [[scalikejdbc.SQL]]:
 *
 * {{{
 * import scalikejdbc._
 * import org.joda.time.DateTime
 * case class User(id: Long, name: String, birthday: Option[DateTime])
 *
 * val activeUsers: List[User] = DB readOnly { implicit session =>
 *   SQL("select * from users where active = ?")
 *     .bind(true)
 *     .map { rs => User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toJodaDateTime)) }.list.apply()
 * }
 * }}}
 *
 * or
 *
 * {{{
 * val activeUsers: List[User] = DB readOnly { implicit session =>
 *   SQL("select * from users where active = /*'active*/true")
 *     .bindByName('active -> true)
 *     .map { rs => User(id = rs.long("id"), name = rs.string("name"), birthday = Option(rs.date("birthday")).map(_.toJodaDateTime)) }.list.apply()
 * }
 * }}}
 */
package object scalikejdbc
    extends SQLInterpolation
    with ScalaBigDecimalConverterImplicits
    with UnixTimeInMillisConverterImplicits {

  // -----
  // Loan Pattern everywhere

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

  /**
   * Option value converter.
   * @param v nullable raw value
   * @tparam A raw type
   * @return optional value
   */
  def opt[A](v: Any): Option[A] = Option(v.asInstanceOf[A])

}
