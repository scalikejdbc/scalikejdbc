package scalikejdbc

import scala.language.implicitConversions
import scala.reflect.ClassTag

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
case class StringSQLRunner(sql: String) extends LogSupport {

  /**
   * Runs all SQL and returns result as `List[Map[String, Any]]`
   * @param session DB Session
   * @return results as List[Map]
   */
  def run()(implicit session: DBSession = AutoSession): List[Map[String, Any]] =
    try {
      SQL(sql).map(_.toMap()).list.apply()
    } catch {
      case e: java.sql.SQLException =>
        val result = List(Map("RESULT" -> SQL(sql).execute.apply()))
        log.warn(
          "The execution failed in read-only mode first, then was rerun in auto-commit mode. Using #execute from the first is highly recommended."
        )
        result
    }

  /**
   * Runs all SQL and returns result as Boolean value
   * @param session DB Session
   * @return results as Boolean
   */
  def execute()(implicit session: DBSession = AutoSession): Boolean =
    SQL(sql).execute.apply()

  /**
   * Shows all the result
   * @param session DB Session
   */
  def show()(implicit session: DBSession = AutoSession): Unit =
    run().foreach(println)

  /**
   * Casts value to expected type value
   *
   * @param v value
   * @param t ClassTag of expected type
   * @tparam A expected type
   * @return casted value
   */
  private[scalikejdbc] def cast[A](v: Any)(implicit t: ClassTag[A]): A = {
    if (t.runtimeClass == classOf[Int]) {
      v match {
        case null                              => 0
        case bigDecimal: java.math.BigDecimal  => bigDecimal.intValue
        case bigDecimal: scala.math.BigDecimal => bigDecimal.toInt
        case bigInteger: java.math.BigInteger  => bigInteger.intValue
        case bigInt: scala.math.BigInt         => bigInt.toInt
        case int: java.lang.Integer => java.lang.Integer.parseInt(int.toString)
        case short: java.lang.Short =>
          java.lang.Integer.parseInt(short.toString)
        case x => x
      }
    } else if (t.runtimeClass == classOf[Long]) {
      v match {
        case null                              => 0L
        case bigDecimal: java.math.BigDecimal  => bigDecimal.longValue
        case bigDecimal: scala.math.BigDecimal => bigDecimal.toLong
        case bigInteger: java.math.BigInteger  => bigInteger.longValue
        case bigInt: scala.math.BigInt         => bigInt.toLong
        case int: java.lang.Integer => java.lang.Long.parseLong(int.toString)
        case short: java.lang.Short => java.lang.Long.parseLong(short.toString)
        case x                      => x
      }
    } else if (t.runtimeClass == classOf[String]) {
      v match {
        case null => null
        case v    => String.valueOf(v)
      }
    } else {
      v
    }
  }.asInstanceOf[A]

  /**
   * Returns SQL results as List[A]
   *
   * @tparam A value type
   * @return results as List[A]
   */
  def asList[A](implicit t: ClassTag[A]): List[A] =
    run().map(m => cast[A](m.apply(m.keys.head)))

  /**
   * Returns SQL result as single value optionally
   *
   * @tparam A value type
   * @return a single result as A optionally
   */
  def asOption[A](implicit t: ClassTag[A]): Option[A] = asList[A].headOption

  /**
   * Returns SQL result as single value
   *
   * @tparam A value type
   * @return a single result as A
   */
  def as[A](implicit t: ClassTag[A]): A = asOption[A].get

}

object StringSQLRunner {

  /**
   * Converts String to SQLRunner implicitly
   *
   * @param sql SQL string
   * @return SQLRunner
   */
  implicit def stringToSQLRunner(sql: String): StringSQLRunner =
    StringSQLRunner(sql)

}
