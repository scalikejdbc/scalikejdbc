/*
 * Copyright 2014 scalikejdbc.org
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
import scalikejdbc.SQL.Output

private[scalikejdbc] trait OneToManies9Extractor[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3]
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4]
  private[scalikejdbc] def extractTo5: WrappedResultSet => Option[B5]
  private[scalikejdbc] def extractTo6: WrappedResultSet => Option[B6]
  private[scalikejdbc] def extractTo7: WrappedResultSet => Option[B7]
  private[scalikejdbc] def extractTo8: WrappedResultSet => Option[B8]
  private[scalikejdbc] def extractTo9: WrappedResultSet => Option[B9]
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z

  private[scalikejdbc] def processResultSet(
    result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9])]),
    rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9])] = {
    val o = extractOne(rs)
    val (to1, to2, to3, to4, to5, to6, to7, to8, to9) = {
      (extractTo1(rs), extractTo2(rs), extractTo3(rs), extractTo4(rs), extractTo5(rs), extractTo6(rs), extractTo7(rs), extractTo8(rs), extractTo9(rs))
    }
    result.keys.find(_ == o).map { _ =>
      to1.orElse(to2).orElse(to3).orElse(to4).orElse(to5).orElse(to6).orElse(to7).orElse(to8).map { _ =>
        val (ts1, ts2, ts3, ts4, ts5, ts6, ts7, ts8, ts9) = result.apply(o)
        result += (o -> (
          to1.map(t => if (ts1.contains(t)) ts1 else ts1 :+ t).getOrElse(ts1),
          to2.map(t => if (ts2.contains(t)) ts2 else ts2 :+ t).getOrElse(ts2),
          to3.map(t => if (ts3.contains(t)) ts3 else ts3 :+ t).getOrElse(ts3),
          to4.map(t => if (ts4.contains(t)) ts4 else ts4 :+ t).getOrElse(ts4),
          to5.map(t => if (ts5.contains(t)) ts5 else ts5 :+ t).getOrElse(ts5),
          to6.map(t => if (ts6.contains(t)) ts6 else ts6 :+ t).getOrElse(ts6),
          to7.map(t => if (ts7.contains(t)) ts7 else ts7 :+ t).getOrElse(ts7),
          to8.map(t => if (ts8.contains(t)) ts8 else ts8 :+ t).getOrElse(ts8),
          to9.map(t => if (ts9.contains(t)) ts9 else ts9 :+ t).getOrElse(ts9)
        ))
      }.getOrElse(result)
    }.getOrElse {
      result += (
        o -> (
          to1.map(t => Vector(t)).getOrElse(Vector()),
          to2.map(t => Vector(t)).getOrElse(Vector()),
          to3.map(t => Vector(t)).getOrElse(Vector()),
          to4.map(t => Vector(t)).getOrElse(Vector()),
          to5.map(t => Vector(t)).getOrElse(Vector()),
          to6.map(t => Vector(t)).getOrElse(Vector()),
          to7.map(t => Vector(t)).getOrElse(Vector()),
          to8.map(t => Vector(t)).getOrElse(Vector()),
          to9.map(t => Vector(t)).getOrElse(Vector())
        )
      )
    }
  }

  private[scalikejdbc] def toTraversable(
    session: DBSession, sql: String, params: Seq[_],
    extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(
      LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9])]())(processResultSet).map {
        case (one, (t1, t2, t3, t4, t5, t6, t7, t8, t9)) => extractor(one, t1, t2, t3, t4, t5, t6, t7, t8, t9)
      }
  }

}

class OneToManies9SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8],
    to9: WrappedResultSet => Option[B9])(
      extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z): OneToManies9SQL[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, HasExtractor, Z] = {
    new OneToManies9SQL(statement, parameters)(output)(one)(to1, to2, to3, to4, to5, to6, to7, to8, to9)(extractor)
  }
  override def toTraversable(): OneToManies9SQLToTraversable[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = {
    new OneToManies9SQLToTraversable[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z](
      statement, parameters)(one)(to1, to2, to3, to4, to5, to6, to7, to8, to9)(extractor)
  }
  override def toList(): OneToManies9SQLToList[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = {
    new OneToManies9SQLToList[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z](
      statement, parameters)(one)(to1, to2, to3, to4, to5, to6, to7, to8, to9)(extractor)
  }
  override def toOption(): OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = {
    new OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z](
      statement, parameters)(one)(to1, to2, to3, to4, to5, to6, to7, to8, to9)(extractor)
  }

  override def single(): OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = toOption()
  override def headOption(): OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = toOption()
  override def first(): OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = toOption()
  override def list(): OneToManies9SQLToList[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = toList()
  override def traversable(): OneToManies9SQLToTraversable[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] = toTraversable()
}

class OneToManies9SQLToList[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8],
    to9: WrappedResultSet => Option[B9])(
      extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies9Extractor[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] {

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
  private[scalikejdbc] def extractTo6: WrappedResultSet => Option[B6] = to6
  private[scalikejdbc] def extractTo7: WrappedResultSet => Option[B7] = to7
  private[scalikejdbc] def extractTo8: WrappedResultSet => Option[B8] = to8
  private[scalikejdbc] def extractTo9: WrappedResultSet => Option[B9] = to9
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z = extractor
}

class OneToManies9SQLToTraversable[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8],
    to9: WrappedResultSet => Option[B9])(
      extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies9Extractor[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] {

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
  private[scalikejdbc] def extractTo6: WrappedResultSet => Option[B6] = to6
  private[scalikejdbc] def extractTo7: WrappedResultSet => Option[B7] = to7
  private[scalikejdbc] def extractTo8: WrappedResultSet => Option[B8] = to8
  private[scalikejdbc] def extractTo9: WrappedResultSet => Option[B9] = to9
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z = extractor
}

class OneToManies9SQLToOption[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E <: WithExtractor, Z](
  override val statement: String,
  override val parameters: Seq[Any])(one: WrappedResultSet => A)(
    to1: WrappedResultSet => Option[B1],
    to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3],
    to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5],
    to6: WrappedResultSet => Option[B6],
    to7: WrappedResultSet => Option[B7],
    to8: WrappedResultSet => Option[B8],
    to9: WrappedResultSet => Option[B9])(
      extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z)
    extends SQL[Z, E](statement, parameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies9Extractor[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, E, Z] {

  val output = Output.single

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
  private[scalikejdbc] def extractTo6: WrappedResultSet => Option[B6] = to6
  private[scalikejdbc] def extractTo7: WrappedResultSet => Option[B7] = to7
  private[scalikejdbc] def extractTo8: WrappedResultSet => Option[B8] = to8
  private[scalikejdbc] def extractTo9: WrappedResultSet => Option[B9] = to9
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5], Seq[B6], Seq[B7], Seq[B8], Seq[B9]) => Z = extractor
}
