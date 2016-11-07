package scalikejdbc

import scala.collection.mutable.LinkedHashMap
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

private[scalikejdbc] trait OneToManyExtractor[A, B, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B]
  private[scalikejdbc] def transform: (A, Seq[B]) => Z

  private[scalikejdbc] def processResultSet(oneToMany: (LinkedHashMap[A, Seq[B]]), rs: WrappedResultSet): LinkedHashMap[A, Seq[B]] = {
    val o = extractOne(rs)
    oneToMany.keys.find(_ == o).map { _ =>
      extractTo(rs).map(many => oneToMany += (o -> (oneToMany.apply(o) :+ many))).getOrElse(oneToMany)
    }.getOrElse {
      oneToMany += (o -> extractTo(rs).map(many => Vector(many)).getOrElse(Nil))
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], zExtractor: (A, Seq[B]) => Z): Traversable[Z] = {
    session.foldLeft(statement, rawParameters: _*)(LinkedHashMap[A, (Seq[B])]())(processResultSet).map {
      case (one, (to)) => zExtractor(one, to)
    }
  }

}

class OneToManySQL[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(val zExtractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(zExtractor: (A, Seq[B]) => Z): OneToManySQL[A, B, HasExtractor, Z] = {
    val q = new OneToManySQL[A, B, HasExtractor, Z](statement, rawParameters)(one)(toMany)(zExtractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toTraversable(): OneToManySQLToTraversable[A, B, E, Z] = {
    val q = new OneToManySQLToTraversable[A, B, E, Z](statement, rawParameters)(one)(toMany)(zExtractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toList(): OneToManySQLToList[A, B, E, Z] = {
    val q = new OneToManySQLToList[A, B, E, Z](statement, rawParameters)(one)(toMany)(zExtractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toOption(): OneToManySQLToOption[A, B, E, Z] = {
    val q = new OneToManySQLToOption[A, B, E, Z](statement, rawParameters)(one)(toMany)(zExtractor)(true)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def headOption(): OneToManySQLToOption[A, B, E, Z] = {
    val q = new OneToManySQLToOption[A, B, E, Z](statement, rawParameters)(one)(toMany)(zExtractor)(false)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toCollection: OneToManySQLToCollection[A, B, E, Z] = {
    val q = new OneToManySQLToCollection[A, B, E, Z](statement, rawParameters)(one)(toMany)(zExtractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def single(): OneToManySQLToOption[A, B, E, Z] = toOption()
  override def first(): OneToManySQLToOption[A, B, E, Z] = headOption()
  override def list(): OneToManySQLToList[A, B, E, Z] = toList()
  override def traversable(): OneToManySQLToTraversable[A, B, E, Z] = toTraversable()
  override def collection: OneToManySQLToCollection[A, B, E, Z] = toCollection

}

object OneToManySQL {
  def unapply[A, B, E <: WithExtractor, Z](sqlObject: OneToManySQL[A, B, E, Z]): Option[(String, Seq[Any], WrappedResultSet => A, WrappedResultSet => Option[B], (A, Seq[B]) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, sqlObject.toMany, sqlObject.zExtractor))
  }
}

class OneToManySQLToList[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(val zExtractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, rawParameters, zExtractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = zExtractor
}

object OneToManySQLToList {
  def unapply[A, B, E <: WithExtractor, Z](sqlObject: OneToManySQLToList[A, B, E, Z]): Option[(String, Seq[Any], WrappedResultSet => A, WrappedResultSet => Option[B], (A, Seq[B]) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, sqlObject.toMany, sqlObject.zExtractor))
  }
}

class OneToManySQLToTraversable[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(val zExtractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, rawParameters, zExtractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = zExtractor
}

object OneToManySQLToTraversable {
  def unapply[A, B, E <: WithExtractor, Z](sqlObject: OneToManySQLToTraversable[A, B, E, Z]): Option[(String, Seq[Any], WrappedResultSet => A, WrappedResultSet => Option[B], (A, Seq[B]) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, sqlObject.toMany, sqlObject.zExtractor))
  }
}

class OneToManySQLToCollection[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(val zExtractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToCollection[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, Z, C[Z]]): C[Z] = {
    executeQuery(session, (session: DBSession) => toTraversable(session, statement, rawParameters, zExtractor).to[C])
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = zExtractor
}

object OneToManySQLToCollection {
  def unapply[A, B, E <: WithExtractor, Z](sqlObject: OneToManySQLToCollection[A, B, E, Z]): Option[(String, Seq[Any], WrappedResultSet => A, WrappedResultSet => Option[B], (A, Seq[B]) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, sqlObject.toMany, sqlObject.zExtractor))
  }
}

class OneToManySQLToOption[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(val zExtractor: (A, Seq[B]) => Z)(val isSingle: Boolean = true)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, rawParameters, zExtractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = zExtractor
}

object OneToManySQLToOption {
  def unapply[A, B, E <: WithExtractor, Z](sqlObject: OneToManySQLToOption[A, B, E, Z]): Option[(String, Seq[Any], WrappedResultSet => A, WrappedResultSet => Option[B], (A, Seq[B]) => Z, Boolean)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, sqlObject.toMany, sqlObject.zExtractor, sqlObject.isSingle))
  }
}
