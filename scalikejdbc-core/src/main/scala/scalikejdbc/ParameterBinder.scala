package scalikejdbc

import java.sql.PreparedStatement

/**
 * ParameterBinder which enables customizing StatementExecutor#binParams.
 *
 * {{{
 * val bytes = Array[Byte](1,2,3, ...)
 * val in = ByteArrayInputStream(bytes)
 * val bin = ParameterBinder(
 *   value = in,
 *   binder = (stmt, idx) => stmt.setBinaryStream(idx, in, bytes.length)
 * )
 * sql"insert into table (bin) values (${bin})".update.apply()
 * }}}
 */
trait ParameterBinder[A] {

  /**
   * Parameter value.
   */
  @deprecated("This unused field will be removed", since = "2.2.4")
  def value: A

  /**
   * Applies parameter to PreparedStatement.
   */
  def apply(stmt: PreparedStatement, idx: Int): Unit

}

/**
 * ParameterBinder factory.
 */
object ParameterBinder {

  /**
   * Factory method for ParameterBinder.
   */
  def apply[A](value: A, binder: (PreparedStatement, Int) => Unit): ParameterBinder[A] = {
    val _v = value
    new ParameterBinder[A] {
      override def value: A = _v
      override def apply(stmt: PreparedStatement, idx: Int): Unit = binder.apply(stmt, idx)
    }
  }

}

