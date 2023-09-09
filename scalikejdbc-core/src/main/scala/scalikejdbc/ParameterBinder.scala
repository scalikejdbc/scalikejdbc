package scalikejdbc

import java.sql.PreparedStatement

import scalikejdbc.interpolation.SQLSyntax

// ------------------------------------------------------------------------------

/**
 * Enables customizing StatementExecutor#bindParams behavior.
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
trait ParameterBinder { self =>

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
  def apply(
    value: Any,
    binder: (PreparedStatement, Int) => Unit
  ): ParameterBinderWithValue = {
    val _value = value
    new ParameterBinderWithValue {
      val value: Any = _value
      override def apply(stmt: PreparedStatement, idx: Int): Unit =
        binder(stmt, idx)
    }
  }

  def unapply(a: Any): Option[Any] = {
    PartialFunction.condOpt(a) { case x: ParameterBinderWithValue =>
      nestedExtract(x.value)
    }
  }

  @annotation.tailrec
  private def nestedExtract(p: Any): Any = p match {
    case x: ParameterBinderWithValue => nestedExtract(x.value)
    case _                           => p
  }

  object NullParameterBinder extends ParameterBinderWithValue {
    val value: Any = null
    def apply(stmt: PreparedStatement, idx: Int): Unit =
      stmt.setObject(idx, null)
    override def toString: String = "ParameterBinder(value=NULL)"
  }

}

// ------------------------------------------------------------------------------

/**
 * ParameterBinder which holds a value to bind.
 *
 */
trait ParameterBinderWithValue extends ParameterBinder { self =>

  def value: Any

  override def toString: String = s"ParameterBinder(value=$value)"

}

private[scalikejdbc] case class ContramappedParameterBinder(
  value: Any,
  underlying: ParameterBinder
) extends ParameterBinderWithValue {
  def apply(stmt: PreparedStatement, idx: Int): Unit = underlying(stmt, idx)
}

// ------------------------------------------------------------------------------

/**
 * ParameterBinder which holds SQLSyntax.
 */
private[scalikejdbc] case class SQLSyntaxParameterBinder(syntax: SQLSyntax)
  extends ParameterBinderWithValue {

  val value: SQLSyntax = syntax

  def apply(stmt: PreparedStatement, idx: Int): Unit = ()
}

// ------------------------------------------------------------------------------

/**
 * Type unsafe ParameterBinder which holds any value and binds it as-is to PreparedStatement.
 */
case class AsIsParameterBinder(value: Any) extends ParameterBinderWithValue {

  private[this] def unsupportedError(): Nothing = {
    throw new UnsupportedOperationException(
      "Apply method doesn't work because this is an AsIsParameterBinder"
    )
  }

  override def apply(stmt: PreparedStatement, idx: Int): Unit =
    unsupportedError()

  override def toString: String = s"AsIsParameterBinder(value=$value)"

}
