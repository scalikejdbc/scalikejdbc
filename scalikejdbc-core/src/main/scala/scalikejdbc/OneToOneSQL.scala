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

private[scalikejdbc] trait OneToOneExtractor[A, B, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B]
  private[scalikejdbc] def transform: (A, B) => Z

  private[scalikejdbc] def processResultSet(oneToOne: (LinkedHashMap[A, Option[B]]), rs: WrappedResultSet): LinkedHashMap[A, Option[B]] = {
    val o = extractOne(rs)
    oneToOne.keys.find(_ == o).map {
      case Some(found) => throw new IllegalRelationshipException(ErrorMessage.INVALID_ONE_TO_ONE_RELATION)
    }.getOrElse {
      oneToOne += (o -> extractTo(rs))
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, B) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, Option[B]]())(processResultSet).map {
      case (one, Some(to)) => extractor(one, to)
      case (one, None) => one.asInstanceOf[Z]
    }
  }

}

class OneToOneSQL[A, B, E <: WithExtractor, Z](
  override val statement: String, override val parameters: Seq[Any])(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, B) => Z): OneToOneSQL[A, B, HasExtractor, Z] = {
    new OneToOneSQL(statement, parameters)(one)(toOne)(extractor)
  }

  override def toTraversable(): OneToOneSQLToTraversable[A, B, E, Z] = {
    new OneToOneSQLToTraversable[A, B, E, Z](statement, parameters)(one)(toOne)(extractor)
  }
  override def toList(): OneToOneSQLToList[A, B, E, Z] = {
    new OneToOneSQLToList[A, B, E, Z](statement, parameters)(one)(toOne)(extractor)
  }
  override def toOption(): OneToOneSQLToOption[A, B, E, Z] = {
    new OneToOneSQLToOption[A, B, E, Z](statement, parameters)(one)(toOne)(extractor)(true)
  }
  override def headOption(): OneToOneSQLToOption[A, B, E, Z] = {
    new OneToOneSQLToOption[A, B, E, Z](statement, parameters)(one)(toOne)(extractor)(false)
  }
  override def toCollection: OneToOneSQLToCollection[A, B, E, Z] = {
    new OneToOneSQLToCollection[A, B, E, Z](statement, parameters)(one)(toOne)(extractor)
  }

  override def single(): OneToOneSQLToOption[A, B, E, Z] = toOption()
  override def first(): OneToOneSQLToOption[A, B, E, Z] = headOption()
  override def list(): OneToOneSQLToList[A, B, E, Z] = toList()
  override def traversable(): OneToOneSQLToTraversable[A, B, E, Z] = toTraversable()
  override def collection: OneToOneSQLToCollection[A, B, E, Z] = toCollection
}

class OneToOneSQLToTraversable[A, B, E <: WithExtractor, Z](
  override val statement: String, override val parameters: Seq[Any])(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(map: (A, B) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, transform))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toOne
  private[scalikejdbc] def transform: (A, B) => Z = map
}

class OneToOneSQLToList[A, B, E <: WithExtractor, Z](
  override val statement: String, override val parameters: Seq[Any])(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toOne
  private[scalikejdbc] def transform: (A, B) => Z = extractor
}

class OneToOneSQLToCollection[A, B, E <: WithExtractor, Z](
  override val statement: String, override val parameters: Seq[Any])(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToCollection[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, Z, C[Z]]): C[Z] = {
    executeQuery(session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).to[C])
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toOne
  private[scalikejdbc] def transform: (A, B) => Z = extractor
}

class OneToOneSQLToOption[A, B, E <: WithExtractor, Z](
  override val statement: String, override val parameters: Seq[Any])(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)(protected val isSingle: Boolean = true)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toOne
  private[scalikejdbc] def transform: (A, B) => Z = extractor
}
