package scalikejdbc.jsr310

import scala.language.implicitConversions
import scalikejdbc.WrappedResultSet

@deprecated("will be removed", "3.0.0")
object Implicits extends Implicits

@deprecated("will be removed", "3.0.0")
trait Implicits {

  @deprecated("use WrappedResultSet", "3.0.0")
  implicit def fromWrappedResultSetToJSR310WrappedResultSet(rs: WrappedResultSet): JSR310WrappedResultSet =
    new JSR310WrappedResultSet(rs)

}
