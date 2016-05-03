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
  def apply[A](value: A, binder: (PreparedStatement, Int) => Unit): ParameterBinderWithValue[A] = {
    val _value = value
    new ParameterBinderWithValue[A] {
      val value: A = _value
      override def apply(stmt: PreparedStatement, idx: Int): Unit = binder(stmt, idx)
    }
  }

  def unapply(a: Any): Option[Any] = {
    PartialFunction.condOpt(a) {
      case x: ParameterBinderWithValue[_] => nestedExtract(x.value)
    }
  }

  @annotation.tailrec
  private def nestedExtract(p: Any): Any = p match {
    case x: ParameterBinderWithValue[_] => nestedExtract(x.value)
    case _ => p
  }

  def NullParameterBinder[A]: ParameterBinderWithValue[A] = new ParameterBinderWithValue[A] {
    val value = null.asInstanceOf[A]
    def apply(stmt: PreparedStatement, idx: Int): Unit = stmt.setObject(idx, null)
    override def toString: String = s"ParameterBinder(value=NULL)"
  }

}

// ------------------------------------------------------------------------------

/**
 * ParameterBinder which holds a value to bind.
 *
 * @tparam A value's type
 */
trait ParameterBinderWithValue[A] extends ParameterBinder { self =>

  def value: A

  // keep this API private because [[scalikejdbc.ParameterBinder#map]] breaks the Functor-law
  private[scalikejdbc] def map[B](f: A => B): ParameterBinderWithValue[B] = new ParameterBinderWithValue[B] {
    lazy val value: B = f(self.value)
    def apply(stmt: PreparedStatement, idx: Int): Unit = self(stmt, idx)
  }

  override def toString: String = s"ParameterBinder(value=$value)"

}

// ------------------------------------------------------------------------------

/**
 * ParameterBinder which holds SQLSyntax.
 */
private[scalikejdbc] case class SQLSyntaxParameterBinder(syntax: SQLSyntax)
    extends ParameterBinderWithValue[SQLSyntax] {

  val value = syntax

  def apply(stmt: PreparedStatement, idx: Int): Unit = ()
}

// ------------------------------------------------------------------------------

/**
 * Type unsafe ParameterBinder which holds any value and binds it as-is to PreparedStatement.
 */
case class AsIsParameterBinder(value: Any) extends ParameterBinderWithValue[Any] {

  private[this] def unsupportedError(): Nothing = {
    throw new UnsupportedOperationException("Apply method doesn't work because this is an AsIsParameterBinder")
  }

  override def apply(stmt: PreparedStatement, idx: Int): Unit = unsupportedError
  override private[scalikejdbc] def map[B](f: Any => B): ParameterBinderWithValue[B] = unsupportedError

  override def toString: String = s"AsIsParameterBinder(value=$value)"

}
