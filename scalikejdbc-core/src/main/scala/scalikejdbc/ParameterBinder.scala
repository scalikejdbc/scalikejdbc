package scalikejdbc

import java.sql.PreparedStatement

/**
 * EssentialParameterBinder which enables customizing StatementExecutor#binParams.
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
trait ParameterBinderWithValue[A] extends ParameterBinder { self =>

  def value: A

  // [[scalikejdbc.ParameterBinder#map]] breaks the Functor-law
  private[scalikejdbc] def map[B](f: A => B): ParameterBinderWithValue[B] = new ParameterBinderWithValue[B] {
    lazy val value: B = f(self.value)
    def apply(stmt: PreparedStatement, idx: Int): Unit = self(stmt, idx)
  }

  override def toString: String = s"ParameterBinder(value=$value)"

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
      case x: ParameterBinderWithValue[_] => x.value
    }
  }

  def NullParameterBinder[A]: ParameterBinderWithValue[A] = new ParameterBinderWithValue[A] {
    val value = null.asInstanceOf[A]
    def apply(stmt: PreparedStatement, idx: Int): Unit = stmt.setObject(idx, null)
    override def toString: String = s"ParameterBinder(value=NULL)"
  }

}

