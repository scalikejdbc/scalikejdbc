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

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B]) => Z): Traversable[Z] = {
    session.foldLeft(statement, rawParameters: _*)(LinkedHashMap[A, (Seq[B])]())(processResultSet).map {
      case (one, (to)) => extractor(one, to)
    }
  }

}

class OneToManySQL[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def copy(statement: String = statement, rawParameters: Seq[Any] = rawParameters, one: WrappedResultSet => A = one, toMany: WrappedResultSet => Option[B] = toMany, extractor: (A, Seq[B]) => Z = extractor): OneToManySQL[A, B, E, Z] = {
    new OneToManySQL(statement, rawParameters)(one)(toMany)(extractor)
  }

  def map(extractor: (A, Seq[B]) => Z): OneToManySQL[A, B, HasExtractor, Z] = {
    val q = new OneToManySQL[A, B, HasExtractor, Z](statement, rawParameters)(one)(toMany)(extractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toTraversable(): OneToManySQLToTraversable[A, B, E, Z] = {
    val q = new OneToManySQLToTraversable[A, B, E, Z](statement, rawParameters)(one)(toMany)(extractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toList(): OneToManySQLToList[A, B, E, Z] = {
    val q = new OneToManySQLToList[A, B, E, Z](statement, rawParameters)(one)(toMany)(extractor)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toOption(): OneToManySQLToOption[A, B, E, Z] = {
    val q = new OneToManySQLToOption[A, B, E, Z](statement, rawParameters)(one)(toMany)(extractor)(true)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def headOption(): OneToManySQLToOption[A, B, E, Z] = {
    val q = new OneToManySQLToOption[A, B, E, Z](statement, rawParameters)(one)(toMany)(extractor)(false)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  override def toCollection: OneToManySQLToCollection[A, B, E, Z] = {
    val q = new OneToManySQLToCollection[A, B, E, Z](statement, rawParameters)(one)(toMany)(extractor)
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

class OneToManySQLToList[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, rawParameters, extractor).toList)
  }
  def copy(statement: String = statement, rawParameters: Seq[Any] = rawParameters, one: WrappedResultSet => A = one, toMany: WrappedResultSet => Option[B] = toMany, extractor: (A, Seq[B]) => Z = extractor): OneToManySQLToList[A, B, E, Z] = {
    new OneToManySQLToList(statement, rawParameters)(one)(toMany)(extractor)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

class OneToManySQLToTraversable[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, rawParameters, extractor))
  }
  def copy(statement: String = statement, rawParameters: Seq[Any] = rawParameters, one: WrappedResultSet => A = one, toMany: WrappedResultSet => Option[B] = toMany, extractor: (A, Seq[B]) => Z = extractor): OneToManySQLToTraversable[A, B, E, Z] = {
    new OneToManySQLToTraversable(statement, rawParameters)(one)(toMany)(extractor)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

class OneToManySQLToCollection[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToCollection[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, Z, C[Z]]): C[Z] = {
    executeQuery(session, (session: DBSession) => toTraversable(session, statement, rawParameters, extractor).to[C])
  }
  def copy(statement: String = statement, rawParameters: Seq[Any] = rawParameters, one: WrappedResultSet => A = one, toMany: WrappedResultSet => Option[B] = toMany, extractor: (A, Seq[B]) => Z = extractor): OneToManySQLToCollection[A, B, E, Z] = {
    new OneToManySQLToCollection(statement, rawParameters)(one)(toMany)(extractor)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

class OneToManySQLToOption[A, B, E <: WithExtractor, Z](
  override val statement: String, override val rawParameters: Seq[Any]
)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)(protected val isSingle: Boolean = true)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, rawParameters, extractor)))
  }
  def copy(statement: String = statement, rawParameters: Seq[Any] = rawParameters, one: WrappedResultSet => A = one, toMany: WrappedResultSet => Option[B] = toMany, extractor: (A, Seq[B]) => Z = extractor, isSingle: Boolean = isSingle): OneToManySQLToOption[A, B, E, Z] = {
    new OneToManySQLToOption(statement, rawParameters)(one)(toMany)(extractor)(isSingle)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}
