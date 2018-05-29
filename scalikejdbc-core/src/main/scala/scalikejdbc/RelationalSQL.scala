package scalikejdbc

import scala.language.higherKinds

//------------------------------------
// One-to-one / One-to-many
//------------------------------------

private[scalikejdbc] trait RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def toSingle(rows: Iterable[Z]): Option[Z] = {
    if (rows.size > 1) throw new TooManyRowsException(1, rows.size)
    else rows.headOption
  }

  private[scalikejdbc] def executeQuery[R[Z]](session: DBSession, op: DBSession => R[Z]): R[Z] = try {
    session match {
      case AutoSession | ReadOnlyAutoSession => DB readOnly op
      case NamedAutoSession(name, _) => NamedDB(name, session.settings) readOnly op
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings) readOnly op
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
trait AllOutputDecisionsUnsupported[Z, E <: scalikejdbc.WithExtractor] extends SQL[Z, E] {
  val message = "You should call #toOne, #toMany or #toManies here."
  override def toOption(): SQLToOption[Z, E] = throw new UnsupportedOperationException(message)
  override def single(): SQLToOption[Z, E] = throw new UnsupportedOperationException(message)
  override def headOption(): SQLToOption[Z, E] = throw new UnsupportedOperationException(message)
  override def first(): SQLToOption[Z, E] = throw new UnsupportedOperationException(message)
  override def toList(): SQLToList[Z, E] = throw new UnsupportedOperationException(message)
  override def list(): SQLToList[Z, E] = throw new UnsupportedOperationException(message)
  override def toIterable(): SQLToIterable[Z, E] = throw new UnsupportedOperationException(message)
  override def iterable(): SQLToIterable[Z, E] = throw new UnsupportedOperationException(message)
  override def toCollection: SQLToCollection[Z, E] = throw new UnsupportedOperationException(message)
  override def collection: SQLToCollection[Z, E] = throw new UnsupportedOperationException(message)
}

/**
 * Endpoint of one-to-x APIs
 */
class OneToXSQL[A, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)
  extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-one/one-to-many operation needs toOne(RS => Option[B]).map((A,B) => A) or toMany(RS => Option[B]).map((A,Seq(B) => A)."))
  with AllOutputDecisionsUnsupported[Z, E] {

  def toOne[B](to: WrappedResultSet => B): OneToOneSQL[A, B, E, Z] = {
    new OneToOneSQL(statement, rawParameters)(one)(to.andThen((b: B) => Option(b)))((a, b) => a.asInstanceOf[Z])
  }

  def toOptionalOne[B](to: WrappedResultSet => Option[B]): OneToOneSQL[A, Option[B], E, Z] = {
    new OneToOneSQL(statement, rawParameters)(one)(to.andThen((maybeB: Option[B]) => Option(maybeB)))((a, maybeB) => a.asInstanceOf[Z])
  }

  def toMany[B](to: WrappedResultSet => Option[B]): OneToManySQL[A, B, E, Z] = {
    val q: OneToManySQL[A, B, E, Z] = new OneToManySQL(statement, rawParameters)(one)(to)((a, bs) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
    q
  }

  def toManies[B1, B2](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2]): OneToManies2SQL[A, B1, B2, E, Z] = {
    val q: OneToManies2SQL[A, B1, B2, E, Z] = new OneToManies2SQL(
      statement, rawParameters)(one)(to1, to2)((a, bs1, bs2) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
    q
  }

  def toManies[B1, B2, B3](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3]): OneToManies3SQL[A, B1, B2, B3, E, Z] = {
    val q: OneToManies3SQL[A, B1, B2, B3, E, Z] = new OneToManies3SQL(
      statement, rawParameters)(one)(to1, to2, to3)((a, bs1, bs2, bs3) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
    q
  }

  def toManies[B1, B2, B3, B4](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4]): OneToManies4SQL[A, B1, B2, B3, B4, E, Z] = {
    val q: OneToManies4SQL[A, B1, B2, B3, B4, E, Z] = new OneToManies4SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4)((a, bs1, bs2, bs3, bs4) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

  def toManies[B1, B2, B3, B4, B5](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5]): OneToManies5SQL[A, B1, B2, B3, B4, B5, E, Z] = {
    val q: OneToManies5SQL[A, B1, B2, B3, B4, B5, E, Z] = new OneToManies5SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4, to5)((a, bs1, bs2, bs3, bs4, bs5) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

  def toManies[B1, B2, B3, B4, B5, B6](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6]): OneToManies6SQL[A, B1, B2, B3, B4, B5, B6, E, Z] = {
    val q: OneToManies6SQL[A, B1, B2, B3, B4, B5, B6, E, Z] = new OneToManies6SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4, to5, to6)((a, bs1, bs2, bs3, bs4, bs5, bs6) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

  def toManies[B1, B2, B3, B4, B5, B6, B7](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7]): OneToManies7SQL[A, B1, B2, B3, B4, B5, B6, B7, E, Z] = {
    val q: OneToManies7SQL[A, B1, B2, B3, B4, B5, B6, B7, E, Z] = new OneToManies7SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4, to5, to6, to7)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

  def toManies[B1, B2, B3, B4, B5, B6, B7, B8](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8]): OneToManies8SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, E, Z] = {
    val q: OneToManies8SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, E, Z] = new OneToManies8SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4, to5, to6, to7, to8)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

  def toManies[B1, B2, B3, B4, B5, B6, B7, B8, B9](
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8],
    to9: WrappedResultSet => Option[B9]): OneToManies9SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = {
    val q: OneToManies9SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = new OneToManies9SQL(
      statement, rawParameters)(one)(to1, to2, to3, to4, to5, to6, to7, to8, to9)((a, bs1, bs2, bs3, bs4, bs5, bs6, bs7, bs8, bs9) => a.asInstanceOf[Z])
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags.toSeq: _*)
  }

}

object OneToXSQL {
  def unapply[A, E <: WithExtractor, Z](sqlObject: OneToXSQL[A, E, Z]): Option[(String, scala.collection.Seq[Any], WrappedResultSet => A)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one))
  }
  def handleException(e: Exception) = e match {
    case invalidColumn: InvalidColumnNameException =>
      throw new ResultSetExtractorException(
        "Failed to extract ResultSet because the specified column name (" + invalidColumn.name + ") is invalid." +
          " If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.")
    case e: Exception => throw e
  }
}
