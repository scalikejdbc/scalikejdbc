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
  with DeprecatedOneToManiesTraversable
  with UnixTimeInMillisConverterImplicits {

  // -----
  // Loan Pattern everywhere

  type Closable = { def close(): Unit }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

  /**
   * Option value converter.
   * @param v nullable raw value
   * @tparam A raw type
   * @return optional value
   */
  def opt[A](v: Any): Option[A] = Option(v.asInstanceOf[A])

  @deprecated(message = "use OneToOneSQLToIterable instead", since = "3.3.0")
  type OneToOneSQLToTraversable[A, B, E <: WithExtractor, Z] = OneToOneSQLToIterable[A, B, E, Z]
  @deprecated(message = "use OneToOneSQLToIterable instead", since = "3.3.0")
  val OneToOneSQLToTraversable = OneToOneSQLToIterable

  @deprecated(message = "use SQLToIterableImpl instead", since = "3.3.0")
  type SQLToTraversableImpl[A, E <: WithExtractor] = SQLToIterableImpl[A, E]
  @deprecated(message = "use SQLToIterableImpl instead", since = "3.3.0")
  val SQLToTraversableImpl = SQLToIterableImpl

  @deprecated(message = "use OneToManySQLToIterable instead", since = "3.3.0")
  type OneToManySQLToTraversable[A, B, E <: WithExtractor, Z] = OneToManySQLToIterable[A, B, E, Z]
  @deprecated(message = "use OneToManySQLToIterable instead", since = "3.3.0")
  val OneToManySQLToTraversable = OneToManySQLToIterable

  @deprecated(message = "use SQLToIterable instead", since = "3.3.0")
  type SQLToTraversable[A, E <: WithExtractor] = SQLToIterable[A, E]
}
