package scalikejdbc

import scala.collection.compat._

//------------------------------------
// One-to-one / One-to-many
//------------------------------------

private[scalikejdbc] trait RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def toSingle(rows: Iterable[Z]): Option[Z] = {
    if (rows.sizeIs > 1) throw new TooManyRowsException(1, rows.size)
    else rows.headOption
  }

  private[scalikejdbc] def executeQuery[R[Z]](
    session: DBSession,
    op: DBSession => R[Z]
  ): R[Z] = try {
    session match {
      case AutoSession | ReadOnlyAutoSession => DB readOnly op
      case NamedAutoSession(name, _)         =>
        NamedDB(name, session.settings) readOnly op
      case ReadOnlyNamedAutoSession(name, _) =>
        NamedDB(name, session.settings) readOnly op
      case _ => op(session)
    }
  } catch { case e: Exception => OneToXSQL.handleException(e) }

}

/**
 * All output decisions are unsupported by default.
 *
 * @tparam Z return type
 * @tparam E extractor constraint
 */
trait AllOutputDecisionsUnsupported[Z, E <: scalikejdbc.WithExtractor]
  extends SQL[Z, E] {
  val message = "You should call #toOne, #toMany or #toManies here."
  override def toOption: SQLToOption[Z, E] =
    throw new UnsupportedOperationException(message)
  override def single: SQLToOption[Z, E] =
    throw new UnsupportedOperationException(message)
  override def headOption: SQLToOption[Z, E] =
    throw new UnsupportedOperationException(message)
  override def first: SQLToOption[Z, E] =
    throw new UnsupportedOperationException(message)
  override def toList: SQLToList[Z, E] =
    throw new UnsupportedOperationException(message)
  override def list: SQLToList[Z, E] = throw new UnsupportedOperationException(
    message
  )
  override def toIterable: SQLToIterable[Z, E] =
    throw new UnsupportedOperationException(message)
  override def iterable: SQLToIterable[Z, E] =
    throw new UnsupportedOperationException(message)
  override def toCollection: SQLToCollection[Z, E] =
    throw new UnsupportedOperationException(message)
  override def collection: SQLToCollection[Z, E] =
    throw new UnsupportedOperationException(message)
}
