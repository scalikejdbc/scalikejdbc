/*
 * Copyright 2013 - 2014 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import scala.collection.mutable.LinkedHashMap
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

private[scalikejdbc] trait OneToManies2Extractor[A, B1, B2, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z

  private[scalikejdbc] def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2])]), rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2])] = {
    val o = extractOne(rs)
    val (to1, to2) = (extractTo1(rs), extractTo2(rs))
    result.keys.find(_ == o).map { _ =>
      to1.orElse(to2).map { _ =>
        val (ts1, ts2) = result.apply(o)
        result += (o -> (
          to1.map(t => if (ts1.contains(t)) ts1 else ts1 :+ t).getOrElse(ts1),
          to2.map(t => if (ts2.contains(t)) ts2 else ts2 :+ t).getOrElse(ts2)
        ))
      }.getOrElse(result)
    }.getOrElse {
      result += (o -> (to1.map(t => Vector(t)).getOrElse(Vector()), to2.map(t => Vector(t)).getOrElse(Vector())))
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2]) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2])]())(processResultSet).map {
      case (one, (t1, t2)) => extractor(one, t1, t2)
    }
  }

}

class OneToManies2SQL[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2]) => Z): OneToManies2SQL[A, B1, B2, HasExtractor, Z] = {
    new OneToManies2SQL(statement, parameters)(one)(to1, to2)(extractor)
  }
  override def toTraversable(): OneToManies2SQLToTraversable[A, B1, B2, E, Z] = {
    new OneToManies2SQLToTraversable[A, B1, B2, E, Z](statement, parameters)(one)(to1, to2)(extractor)
  }
  override def toList(): OneToManies2SQLToList[A, B1, B2, E, Z] = {
    new OneToManies2SQLToList[A, B1, B2, E, Z](statement, parameters)(one)(to1, to2)(extractor)
  }
  override def toOption(): OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](statement, parameters)(one)(to1, to2)(extractor)(true)
  }
  override def toCollection: OneToManies2SQLToCollection[A, B1, B2, E, Z] = {
    new OneToManies2SQLToCollection[A, B1, B2, E, Z](statement, parameters)(one)(to1, to2)(extractor)
  }
  override def headOption(): OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](statement, parameters)(one)(to1, to2)(extractor)(false)
  }
  override def single(): OneToManies2SQLToOption[A, B1, B2, E, Z] = toOption()
  override def first(): OneToManies2SQLToOption[A, B1, B2, E, Z] = headOption()
  override def list(): OneToManies2SQLToList[A, B1, B2, E, Z] = toList()
  override def traversable(): OneToManies2SQLToTraversable[A, B1, B2, E, Z] = toTraversable()
  override def collection: OneToManies2SQLToCollection[A, B1, B2, E, Z] = toCollection

}

class OneToManies2SQLToList[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z = extractor
}

final class OneToManies2SQLToCollection[A, B1, B2, E <: WithExtractor, Z] private[scalikejdbc] (
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToCollection[Z, E]
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, Z, C[Z]]): C[Z] = {
    executeQuery(session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).to[C])
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z = extractor
}

class OneToManies2SQLToTraversable[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(val one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z = extractor
}

class OneToManies2SQLToOption[A, B1, B2, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)(protected val isSingle: Boolean = true)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z = extractor
}
