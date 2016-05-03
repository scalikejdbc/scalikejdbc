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
   * Returns true if this binder is the bypass one.
   */
  def bypass: Boolean = false

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
      case x: ParameterBinderWithValue[_] => BypassParameterBinder.extractValue(x.value)
    }
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

  override lazy val bypass: Boolean = value match {
    case binder: ParameterBinder => binder.bypass
    case _ => false
  }

  def value: A

  // keep this API private because [[scalikejdbc.ParameterBinder#map]] breaks the Functor-law
  private[scalikejdbc] def map[B](f: A => B): ParameterBinderWithValue[B] = new ParameterBinderWithValue[B] {
    lazy val value: B = f(self.value)
    def apply(stmt: PreparedStatement, idx: Int): Unit = self(stmt, idx)
  }

  override def toString: String = s"ParameterBinderWithValue(value=$value, asIs=$bypass)"

}

// ------------------------------------------------------------------------------

/**
 * ParameterBinder which holds SQLSyntax.
 */
case class SQLSyntaxParameterBinder(syntax: SQLSyntax)
    extends ParameterBinderWithValue[SQLSyntax] {

  val value = syntax

  def apply(stmt: PreparedStatement, idx: Int): Unit = ()
}

// ------------------------------------------------------------------------------

/**
 * Type unsafe ParameterBinder which holds any value and bind it as-is.
 */
class BypassParameterBinder(private val underlying: Any)
    extends ParameterBinderWithValue[Any] {

  override lazy val bypass: Boolean = true

  override val value: Any = BypassParameterBinder.extractValue(underlying)

  def apply(stmt: PreparedStatement, idx: Int): Unit = {
    throw new IllegalStateException("Ths method should not be called")
  }

  override def toString: String = s"BypassParameterBinder(value=$value)"

}

object BypassParameterBinder {

  def apply(value: Any): ParameterBinder = {
    new BypassParameterBinder(extractValue(value))
  }

  def unapply(a: Any): Option[Any] = {
    PartialFunction.condOpt(a) {
      case x: BypassParameterBinder => extractValue(x.value)
    }
  }

  def extractValue(maybeBinder: Any): Any = maybeBinder match {
    case null | None => null
    case Some(v) => extractValue(v)
    case BypassParameterBinder(value) => extractValue(value)
    case binder: ParameterBinderWithValue[_] => extractValue(binder.value)
    case value => value
  }
}
