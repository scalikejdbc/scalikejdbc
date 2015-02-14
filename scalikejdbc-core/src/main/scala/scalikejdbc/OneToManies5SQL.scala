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
import SQLTypeAliases._

private[scalikejdbc] trait OneToManies5Extractor[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3]
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4]
  private[scalikejdbc] def extractTo5: WrappedResultSet => Option[B5]
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z

  private[scalikejdbc] def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5])]),
    rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5])] = {
    val o = extractOne(rs)
    val (to1, to2, to3, to4, to5) = (extractTo1(rs), extractTo2(rs), extractTo3(rs), extractTo4(rs), extractTo5(rs))
    result.keys.find(_ == o).map { _ =>
      to1.orElse(to2).orElse(to3).orElse(to4).orElse(to5).map { _ =>
        val (ts1, ts2, ts3, ts4, ts5) = result.apply(o)
        result += (o -> (
          to1.map(t => if (ts1.contains(t)) ts1 else ts1 :+ t).getOrElse(ts1),
          to2.map(t => if (ts2.contains(t)) ts2 else ts2 :+ t).getOrElse(ts2),
          to3.map(t => if (ts3.contains(t)) ts3 else ts3 :+ t).getOrElse(ts3),
          to4.map(t => if (ts4.contains(t)) ts4 else ts4 :+ t).getOrElse(ts4),
          to5.map(t => if (ts5.contains(t)) ts5 else ts5 :+ t).getOrElse(ts5)
        ))
      }.getOrElse(result)
    }.getOrElse {
      result += (
        o -> (
          to1.map(t => Vector(t)).getOrElse(Vector()),
          to2.map(t => Vector(t)).getOrElse(Vector()),
          to3.map(t => Vector(t)).getOrElse(Vector()),
          to4.map(t => Vector(t)).getOrElse(Vector()),
          to5.map(t => Vector(t)).getOrElse(Vector())
        )
      )
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5])]())(processResultSet).map {
      case (one, (t1, t2, t3, t4, t5)) => extractor(one, t1, t2, t3, t4, t5)
    }
  }

}

class OneToManies5SQL[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z): OneToManies5SQL[A, B1, B2, B3, B4, B5, HasExtractor, Z] = {
    new OneToManies5SQL(statement, parameters)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toTraversable(): OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z](statement, parameters)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toList(): OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z](statement, parameters)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toOption(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z](statement, parameters)(one)(to1, to2, to3, to4, to5)(extractor)(true)
  }

  override def single(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = toOption()
  override def headOption(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z](statement, parameters)(one)(to1, to2, to3, to4, to5)(extractor)(false)
  }
  override def first(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = headOption()
  override def list(): OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z] = toList()
  override def traversable(): OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z] = toTraversable()
}

class OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4] = to4
  private[scalikejdbc] def extractTo5: WrappedResultSet => Option[B5] = to5
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z = extractor
}

class OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4] = to4
  private[scalikejdbc] def extractTo5: WrappedResultSet => Option[B5] = to5
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z = extractor
}

class OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)(protected val isSingle: Boolean = true)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4] = to4
  private[scalikejdbc] def extractTo5: WrappedResultSet => Option[B5] = to5
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z = extractor
}
