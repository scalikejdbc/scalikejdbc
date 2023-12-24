package scalikejdbc

import scala.collection.mutable.LinkedHashMap
import scala.collection.compat._

private[scalikejdbc] trait OneToManies2Extractor[
  A,
  B1,
  B2,
  E <: WithExtractor,
  Z
] extends SQL[Z, E]
  with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def transform
    : (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z

  private[scalikejdbc] def processResultSet(
    result: LinkedHashMap[A, (Seq[B1], scala.collection.Seq[B2])],
    rs: WrappedResultSet
  ): LinkedHashMap[A, (Seq[B1], scala.collection.Seq[B2])] = {
    val o = extractOne(rs)
    val (to1, to2) = (extractTo1(rs), extractTo2(rs))
    if (result.contains(o)) {
      to1
        .orElse(to2)
        .map { _ =>
          val (ts1, ts2) = result.apply(o)
          result += ((o -> (
            (
              to1
                .map(t => if (ts1.contains(t)) ts1 else ts1 :+ t)
                .getOrElse(ts1),
              to2
                .map(t => if (ts2.contains(t)) ts2 else ts2 :+ t)
                .getOrElse(ts2)
            )
          )))
        }
        .getOrElse(result)
    } else {
      result += ((o -> (
        (
          to1.map(t => Vector(t)).getOrElse(Vector()),
          to2.map(t => Vector(t)).getOrElse(Vector())
        )
      )))
    }
  }

  private[scalikejdbc] def toIterable(
    session: DBSession,
    sql: String,
    params: scala.collection.Seq[?],
    zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
  ): Iterable[Z] = {
    val attributesSwitcher = createDBSessionAttributesSwitcher
    DBSessionWrapper(session, attributesSwitcher)
      .foldLeft(statement, rawParameters.toSeq: _*)(
        LinkedHashMap[A, (Seq[B1], scala.collection.Seq[B2])]()
      )(processResultSet)
      .map { case (one, (t1, t2)) =>
        zExtractor(one, t1, t2)
      }
  }

}

class OneToManies2SQL[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any]
)(val one: WrappedResultSet => A)(
  val to1: WrappedResultSet => Option[B1],
  val to2: WrappedResultSet => Option[B2]
)(val zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z)
  extends SQL[Z, E](statement, rawParameters)(
    SQL.noExtractor[Z](
      "one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."
    )
  )
  with AllOutputDecisionsUnsupported[Z, E] {

  def map(
    zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
  ): OneToManies2SQL[A, B1, B2, HasExtractor, Z] = {
    new OneToManies2SQL(statement, rawParameters)(one)(to1, to2)(zExtractor)
  }
  override def toIterable: OneToManies2SQLToIterable[A, B1, B2, E, Z] = {
    new OneToManies2SQLToIterable[A, B1, B2, E, Z](statement, rawParameters)(
      one
    )(to1, to2)(zExtractor)
  }
  override def toList: OneToManies2SQLToList[A, B1, B2, E, Z] = {
    new OneToManies2SQLToList[A, B1, B2, E, Z](statement, rawParameters)(one)(
      to1,
      to2
    )(zExtractor)
  }
  override def toOption: OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](statement, rawParameters)(one)(
      to1,
      to2
    )(zExtractor)(true)
  }
  override def toCollection: OneToManies2SQLToCollection[A, B1, B2, E, Z] = {
    new OneToManies2SQLToCollection[A, B1, B2, E, Z](statement, rawParameters)(
      one
    )(to1, to2)(zExtractor)
  }
  override def headOption: OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](statement, rawParameters)(one)(
      to1,
      to2
    )(zExtractor)(false)
  }
  override def single: OneToManies2SQLToOption[A, B1, B2, E, Z] = toOption
  override def first: OneToManies2SQLToOption[A, B1, B2, E, Z] = headOption
  override def list: OneToManies2SQLToList[A, B1, B2, E, Z] = toList
  override def iterable: OneToManies2SQLToIterable[A, B1, B2, E, Z] = toIterable
  override def collection: OneToManies2SQLToCollection[A, B1, B2, E, Z] =
    toCollection

}

object OneToManies2SQL {
  def unapply[A, B1, B2, E <: WithExtractor, Z](
    sqlObject: OneToManies2SQL[A, B1, B2, E, Z]
  ): Some[
    (
      String,
      scala.collection.Seq[Any],
      WrappedResultSet => A,
      WrappedResultSet => Option[B1],
      WrappedResultSet => Option[B2],
      (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
    )
  ] = {
    Some(
      (
        sqlObject.statement,
        sqlObject.rawParameters,
        sqlObject.one,
        sqlObject.to1,
        sqlObject.to2,
        sqlObject.zExtractor
      )
    )
  }
}

class OneToManies2SQLToList[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any]
)(val one: WrappedResultSet => A)(
  val to1: WrappedResultSet => Option[B1],
  val to2: WrappedResultSet => Option[B2]
)(val zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z)
  extends SQL[Z, E](statement, rawParameters)(
    SQL.noExtractor[Z](
      "one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."
    )
  )
  with SQLToList[Z, E]
  with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit
    session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor
  ): List[Z] = {
    executeQuery[List](
      session,
      (session: DBSession) =>
        toIterable(session, statement, rawParameters, zExtractor).toList
    )
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform
    : (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z = zExtractor
}

object OneToManies2SQLToList {
  def unapply[A, B1, B2, E <: WithExtractor, Z](
    sqlObject: OneToManies2SQLToList[A, B1, B2, E, Z]
  ): Some[
    (
      String,
      scala.collection.Seq[Any],
      WrappedResultSet => A,
      WrappedResultSet => Option[B1],
      WrappedResultSet => Option[B2],
      (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
    )
  ] = {
    Some(
      (
        sqlObject.statement,
        sqlObject.rawParameters,
        sqlObject.one,
        sqlObject.to1,
        sqlObject.to2,
        sqlObject.zExtractor
      )
    )
  }
}

final class OneToManies2SQLToCollection[
  A,
  B1,
  B2,
  E <: WithExtractor,
  Z
] private[scalikejdbc] (
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any]
)(val one: WrappedResultSet => A)(
  val to1: WrappedResultSet => Option[B1],
  val to2: WrappedResultSet => Option[B2]
)(val zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z)
  extends SQL[Z, E](statement, rawParameters)(
    SQL.noExtractor[Z](
      "one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."
    )
  )
  with SQLToCollection[Z, E]
  with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply[C[_]]()(implicit
    session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor,
    f: Factory[Z, C[Z]]
  ): C[Z] = {
    executeQuery(
      session,
      (session: DBSession) =>
        f.fromSpecific(
          toIterable(session, statement, rawParameters, zExtractor)
        )
    )
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform
    : (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z = zExtractor
}

object OneToManies2SQLToCollection {
  def unapply[A, B1, B2, E <: WithExtractor, Z](
    sqlObject: OneToManies2SQLToCollection[A, B1, B2, E, Z]
  ): Some[
    (
      String,
      scala.collection.Seq[Any],
      WrappedResultSet => A,
      WrappedResultSet => Option[B1],
      WrappedResultSet => Option[B2],
      (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
    )
  ] = {
    Some(
      (
        sqlObject.statement,
        sqlObject.rawParameters,
        sqlObject.one,
        sqlObject.to1,
        sqlObject.to2,
        sqlObject.zExtractor
      )
    )
  }
}

class OneToManies2SQLToIterable[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any]
)(val one: WrappedResultSet => A)(
  val to1: WrappedResultSet => Option[B1],
  val to2: WrappedResultSet => Option[B2]
)(val zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z)
  extends SQL[Z, E](statement, rawParameters)(
    SQL.noExtractor[Z](
      "one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."
    )
  )
  with SQLToIterable[Z, E]
  with AllOutputDecisionsUnsupported[Z, E]
  with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit
    session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor
  ): Iterable[Z] = {
    executeQuery[Iterable](
      session,
      (session: DBSession) =>
        toIterable(session, statement, rawParameters, zExtractor)
    )
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform
    : (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z = zExtractor
}

object OneToManies2SQLToIterable {
  def unapply[A, B1, B2, E <: WithExtractor, Z](
    sqlObject: OneToManies2SQLToIterable[A, B1, B2, E, Z]
  ): Some[
    (
      String,
      scala.collection.Seq[Any],
      WrappedResultSet => A,
      WrappedResultSet => Option[B1],
      WrappedResultSet => Option[B2],
      (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z
    )
  ] = {
    Some(
      (
        sqlObject.statement,
        sqlObject.rawParameters,
        sqlObject.one,
        sqlObject.to1,
        sqlObject.to2,
        sqlObject.zExtractor
      )
    )
  }
}

class OneToManies2SQLToOption[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any]
)(val one: WrappedResultSet => A)(
  val to1: WrappedResultSet => Option[B1],
  val to2: WrappedResultSet => Option[B2]
)(val zExtractor: (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z)(
  val isSingle: Boolean = true
) extends SQL[Z, E](statement, rawParameters)(
    SQL.noExtractor[Z](
      "one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."
    )
  )
  with SQLToOption[Z, E]
  with AllOutputDecisionsUnsupported[Z, E]
  with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit
    session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor
  ): Option[Z] = {
    executeQuery[Option](
      session,
      (session: DBSession) =>
        toSingle(toIterable(session, statement, rawParameters, zExtractor))
    )
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform
    : (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z = zExtractor
}

object OneToManies2SQLToOption {
  def unapply[A, B1, B2, E <: WithExtractor, Z](
    sqlObject: OneToManies2SQLToOption[A, B1, B2, E, Z]
  ): Some[
    (
      String,
      scala.collection.Seq[Any],
      WrappedResultSet => A,
      WrappedResultSet => Option[B1],
      WrappedResultSet => Option[B2],
      (A, scala.collection.Seq[B1], scala.collection.Seq[B2]) => Z,
      Boolean
    )
  ] = {
    Some(
      (
        sqlObject.statement,
        sqlObject.rawParameters,
        sqlObject.one,
        sqlObject.to1,
        sqlObject.to2,
        sqlObject.zExtractor,
        sqlObject.isSingle
      )
    )
  }
}
