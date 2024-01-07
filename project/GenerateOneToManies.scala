object GenerateOneToManies {

  def apply(n: Int): String = {
    val A = "A"
    val B = "B"
    val tparams = (1 to n).map(B + _)
    val bs = tparams.mkString(", ")
    val seq = tparams.map("scala.collection.Seq[" + _ + "]").mkString(", ")
    val extractTo = "extractTo"
    val extractToN = (1 to n)
      .map { i =>
        s"  private[scalikejdbc] def $extractTo$i: WrappedResultSet => Option[B$i] = to$i"
      }
      .mkString("\n")
    val extractOne =
      "  private[scalikejdbc] def extractOne: WrappedResultSet => A = one"
    val transform =
      s"  private[scalikejdbc] def transform: (A, $seq) => Z = zExtractor"
    val resultSetToOptions = (1 to n)
      .map { i => s"val to$i: WrappedResultSet => Option[B$i]" }
      .mkString(", ")
    val to = (1 to n).map("to" + _).mkString(", ")
    val resultSetToOptionsType =
      tparams.map("WrappedResultSet => Option[" + _ + "]").mkString(", ")
    val sqlTo = (1 to n).map("sqlObject.to" + _).mkString(", ")

    s"""/*
 * Copyright 2013 - 2015 scalikejdbc.org
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
import scala.collection.compat._

private[scalikejdbc] trait OneToManies${n}Extractor[$A, $bs, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => $A
${(1 to n)
        .map { i =>
          s"  private[scalikejdbc] def $extractTo$i: WrappedResultSet => Option[B$i]"
        }
        .mkString("\n")}
  private[scalikejdbc] def transform: ($A, $seq) => Z

  private[scalikejdbc] def processResultSet(result: (LinkedHashMap[$A, ($seq)]),
    rs: WrappedResultSet): LinkedHashMap[A, ($seq)] = {
    val o = extractOne(rs)
    val (${(1 to n).map("to" + _).mkString(", ")}) = (${(1 to n)
        .map(extractTo + _ + "(rs)")
        .mkString(", ")})
    if (result.contains(o)) {
      ${(1 to n).map("to" + _).mkString("(", " orElse ", ")")}.map { _ =>
        val (${(1 to n).map("ts" + _).mkString(", ")}) = result.apply(o)
        result += (o -> ((
${(1 to n)
        .map { i =>
          s"          to$i.map(t => if (ts$i.contains(t)) ts$i else ts$i :+ t).getOrElse(ts$i)"
        }
        .mkString(",\n")}
        )))
      }.getOrElse(result)
    } else {
      result += (
        o -> ((
${(1 to n)
        .map { i =>
          s"          to$i.map(t => Vector(t)).getOrElse(Vector.empty)"
        }
        .mkString(",\n")}
        ))
      )
    }
  }

  private[scalikejdbc] def toIterable(session: DBSession, sql: String, params: scala.collection.Seq[?], zExtractor: (A, $seq) => Z): Iterable[Z] = {
    val attributesSwitcher = createDBSessionAttributesSwitcher
    DBSessionWrapper(session, attributesSwitcher).foldLeft(statement, rawParameters.toSeq*)(LinkedHashMap[A, ($seq)]())(processResultSet).map {
      case (one, (${(1 to n)
        .map("t" + _)
        .mkString(", ")})) => zExtractor(one, ${(1 to n)
        .map("t" + _)
        .mkString(", ")})
    }
  }

}

class OneToManies${n}SQL[A, $bs, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)($resultSetToOptions)(val zExtractor: (A, $seq) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(zExtractor: (A, $seq) => Z): OneToManies${n}SQL[A, $bs, HasExtractor, Z] = {
    new OneToManies${n}SQL(statement, rawParameters)(one)($to)(zExtractor)
  }
  override def toIterable: OneToManies${n}SQLToIterable[A, $bs, E, Z] = {
    new OneToManies${n}SQLToIterable[A, $bs, E, Z](statement, rawParameters)(one)($to)(zExtractor)
  }
  override def toList: OneToManies${n}SQLToList[A, $bs, E, Z] = {
    new OneToManies${n}SQLToList[A, $bs, E, Z](statement, rawParameters)(one)($to)(zExtractor)
  }
  override def toOption: OneToManies${n}SQLToOption[A, $bs, E, Z] = {
    new OneToManies${n}SQLToOption[A, $bs, E, Z](statement, rawParameters)(one)($to)(zExtractor)(true)
  }
  override def headOption: OneToManies${n}SQLToOption[A, $bs, E, Z] = {
    new OneToManies${n}SQLToOption[A, $bs, E, Z](statement, rawParameters)(one)($to)(zExtractor)(false)
  }
  override def toCollection: OneToManies${n}SQLToCollection[A, $bs, E, Z] = {
    new OneToManies${n}SQLToCollection[A, ${bs}, E, Z](statement, rawParameters)(one)($to)(zExtractor)
  }

  override def single: OneToManies${n}SQLToOption[A, $bs, E, Z] = toOption
  override def first: OneToManies${n}SQLToOption[A, $bs, E, Z] = headOption
  override def list: OneToManies${n}SQLToList[A, $bs, E, Z] = toList
  override def iterable: OneToManies${n}SQLToIterable[A, $bs, E, Z] = toIterable
  override def collection: OneToManies${n}SQLToCollection[A, $bs, E, Z] = toCollection
}

object OneToManies${n}SQL {
  def unapply[A, $bs, E <: WithExtractor, Z](sqlObject: OneToManies${n}SQL[A, $bs, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A, ($resultSetToOptionsType), (A, $seq) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, ($sqlTo), sqlObject.zExtractor))
  }
}

class OneToManies${n}SQLToList[A, $bs, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)($resultSetToOptions)(val zExtractor: (A, $seq) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies${n}Extractor[A, $bs, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toIterable(session, statement, rawParameters, zExtractor).toList)
  }

$extractOne
$extractToN
$transform
}

object OneToManies${n}SQLToList {
  def unapply[A, $bs, E <: WithExtractor, Z](sqlObject: OneToManies${n}SQLToList[A, $bs, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A, ($resultSetToOptionsType), (A, $seq) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, ($sqlTo), sqlObject.zExtractor))
  }
}

final class OneToManies${n}SQLToCollection[A, $bs, E <: WithExtractor, Z] private[scalikejdbc](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)($resultSetToOptions)(val zExtractor: (A, $seq) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToCollection[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies${n}Extractor[A, $bs, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, f: Factory[Z, C[Z]]): C[Z] = {
    executeQuery(session, (session: DBSession) => f.fromSpecific(toIterable(session, statement, rawParameters, zExtractor)))
  }

$extractOne
$extractToN
$transform
}

object OneToManies${n}SQLToCollection {
  def unapply[A, $bs, E <: WithExtractor, Z](sqlObject: OneToManies${n}SQLToCollection[A, $bs, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A, ($resultSetToOptionsType), (A, $seq) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, ($sqlTo), sqlObject.zExtractor))
  }
}

class OneToManies${n}SQLToIterable[A, $bs, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)($resultSetToOptions)(val zExtractor: (A, $seq) => Z)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToIterable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies${n}Extractor[A, $bs, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Iterable[Z] = {
    executeQuery[Iterable](session, (session: DBSession) => toIterable(session, statement, rawParameters, zExtractor))
  }

$extractOne
$extractToN
$transform
}

object OneToManies${n}SQLToIterable {
  def unapply[A, $bs, E <: WithExtractor, Z](sqlObject: OneToManies${n}SQLToIterable[A, $bs, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A, ($resultSetToOptionsType), (A, $seq) => Z)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, ($sqlTo), sqlObject.zExtractor))
  }
}

class OneToManies${n}SQLToOption[A, $bs, E <: WithExtractor, Z](
  override val statement: String,
  override val rawParameters: scala.collection.Seq[Any])(val one: WrappedResultSet => A)($resultSetToOptions)(val zExtractor: (A, $seq) => Z)(val isSingle: Boolean = true)
    extends SQL[Z, E](statement, rawParameters)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies${n}Extractor[A, $bs, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toIterable(session, statement, rawParameters, zExtractor)))
  }

$extractOne
$extractToN
$transform
}

object OneToManies${n}SQLToOption {
  def unapply[A, $bs, E <: WithExtractor, Z](sqlObject: OneToManies${n}SQLToOption[A, $bs, E, Z]): Some[(String, scala.collection.Seq[Any], WrappedResultSet => A, ($resultSetToOptionsType), (A, $seq) => Z, Boolean)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.one, ($sqlTo), sqlObject.zExtractor, sqlObject.isSingle))
  }
}
"""
  }

}
