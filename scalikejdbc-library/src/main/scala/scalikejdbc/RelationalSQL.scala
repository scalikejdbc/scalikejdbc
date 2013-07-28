/*
 * Copyright 2013 Kazuhiro Sera
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

import scalikejdbc.SQL.Output
import scala.collection.mutable.LinkedHashMap

//------------------------------------
// One-to-one / One-to-many
//------------------------------------

private[scalikejdbc] trait RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def toSingle(rows: Traversable[Z]): Option[Z] = {
    if (rows.size > 1) throw new TooManyRowsException(1, rows.size)
    else rows.headOption
  }

  private[scalikejdbc] def executeQuery[R[Z]](session: DBSession, op: DBSession => R[Z]): R[Z] = try {
    session match {
      case AutoSession => DB readOnly (s => op(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => op(s))
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
  override def toTraversable(): SQLToTraversable[Z, E] = throw new UnsupportedOperationException(message)
  override def traversable(): SQLToTraversable[Z, E] = throw new UnsupportedOperationException(message)
}

/**
 * Endpoint of one-to-x APIs
 */
class OneToXSQL[A, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-one/one-to-many operation needs toOne(RS => Option[B]).map((A,B) => A) or toMany(RS => Option[B]).map((A,Seq(B) => A)."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def toOne[B](to: WrappedResultSet => B): OneToOneSQL[A, B, E, Z] = {
    new OneToOneSQL(statement)(parameters: _*)(output)(one)(to.andThen((b: B) => Option(b)))((a, b) => a.asInstanceOf[Z])
  }

  def toOptionalOne[B](to: WrappedResultSet => Option[B]): OneToOneSQL[A, B, E, Z] = {
    new OneToOneSQL(statement)(parameters: _*)(output)(one)(to)((a, b) => a.asInstanceOf[Z])
  }

  def toMany[B](to: WrappedResultSet => Option[B]): OneToManySQL[A, B, E, Z] = {
    new OneToManySQL(statement)(parameters: _*)(output)(one)(to)((a, bs) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2]): OneToManies2SQL[A, B1, B2, E, Z] = {
    new OneToManies2SQL(statement)(parameters: _*)(output)(one)(to1, to2)((a, bs1, bs2) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3]): OneToManies3SQL[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3)((a, bs1, bs2, bs3) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3, B4](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4]): OneToManies4SQL[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3, to4)((a, bs1, bs2, bs3, bs4) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3, B4, B5](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5]): OneToManies5SQL[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3, to4, to5)((a, bs1, bs2, bs3, bs4, bs5) => a.asInstanceOf[Z])
  }
}

object OneToXSQL {
  def handleException(e: Exception) = e match {
    case invalidColumn: InvalidColumnNameException =>
      throw new ResultSetExtractorException(
        "Failed to extract ResultSet because the specified column name (" + invalidColumn.name + ") is invalid." +
          " If you're using SQLInterpolation, you may mistake u.id for u.resultName.id.")
    case e: Exception => throw e
  }
}

//------------------------------------
// One-to-one
//------------------------------------

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

class OneToOneSQL[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, B) => Z): OneToOneSQL[A, B, HasExtractor, Z] = {
    new OneToOneSQL(statement)(parameters: _*)(output)(one)(toOne)(extractor)
  }

  override def toTraversable(): OneToOneSQLToTraversable[A, B, E, Z] = {
    new OneToOneSQLToTraversable[A, B, E, Z](statement)(parameters: _*)(one)(toOne)(extractor)
  }
  override def toList(): OneToOneSQLToList[A, B, E, Z] = {
    new OneToOneSQLToList[A, B, E, Z](statement)(parameters: _*)(one)(toOne)(extractor)
  }
  override def toOption(): OneToOneSQLToOption[A, B, E, Z] = {
    new OneToOneSQLToOption[A, B, E, Z](statement)(parameters: _*)(one)(toOne)(extractor)
  }

  override def single(): OneToOneSQLToOption[A, B, E, Z] = toOption()
  override def headOption(): OneToOneSQLToOption[A, B, E, Z] = toOption()
  override def first(): OneToOneSQLToOption[A, B, E, Z] = toOption()
  override def list(): OneToOneSQLToList[A, B, E, Z] = toList()
  override def traversable(): OneToOneSQLToTraversable[A, B, E, Z] = toTraversable()
}

class OneToOneSQLToTraversable[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(map: (A, B) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
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

class OneToOneSQLToList[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
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

class OneToOneSQLToOption[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToOneExtractor[A, B, E, Z] {

  val output = Output.single

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toOne
  private[scalikejdbc] def transform: (A, B) => Z = extractor
}

//------------------------------------
// One-to-many
//------------------------------------

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
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, (Seq[B])]())(processResultSet).map {
      case (one, (to)) => extractor(one, to)
    }
  }

}

class OneToManySQL[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B]) => Z): OneToManySQL[A, B, HasExtractor, Z] = {
    new OneToManySQL[A, B, HasExtractor, Z](statement)(parameters: _*)(output)(one)(toMany)(extractor)
  }

  override def toTraversable(): OneToManySQLToTraversable[A, B, E, Z] = {
    new OneToManySQLToTraversable[A, B, E, Z](statement)(parameters: _*)(one)(toMany)(extractor)
  }

  override def toList(): OneToManySQLToList[A, B, E, Z] = {
    new OneToManySQLToList[A, B, E, Z](statement)(parameters: _*)(one)(toMany)(extractor)
  }

  override def toOption(): OneToManySQLToOption[A, B, E, Z] = {
    new OneToManySQLToOption[A, B, E, Z](statement)(parameters: _*)(one)(toMany)(extractor)
  }

  override def single(): OneToManySQLToOption[A, B, E, Z] = toOption()
  override def headOption(): OneToManySQLToOption[A, B, E, Z] = toOption()
  override def first(): OneToManySQLToOption[A, B, E, Z] = toOption()
  override def list(): OneToManySQLToList[A, B, E, Z] = toList()
  override def traversable(): OneToManySQLToTraversable[A, B, E, Z] = toTraversable()
}

class OneToManySQLToList[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

class OneToManySQLToTraversable[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

class OneToManySQLToOption[A, B, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManyExtractor[A, B, E, Z] {

  val output = Output.single

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo: WrappedResultSet => Option[B] = toMany
  private[scalikejdbc] def transform: (A, Seq[B]) => Z = extractor
}

//------------------------------------
// One-to-manies 2
//------------------------------------

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

class OneToManies2SQL[A, B1, B2, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2]) => Z): OneToManies2SQL[A, B1, B2, HasExtractor, Z] = {
    new OneToManies2SQL(statement)(parameters: _*)(output)(one)(to1, to2)(extractor)
  }
  override def toTraversable(): OneToManies2SQLToTraversable[A, B1, B2, E, Z] = {
    new OneToManies2SQLToTraversable[A, B1, B2, E, Z](statement)(parameters: _*)(one)(to1, to2)(extractor)
  }
  override def toList(): OneToManies2SQLToList[A, B1, B2, E, Z] = {
    new OneToManies2SQLToList[A, B1, B2, E, Z](statement)(parameters: _*)(one)(to1, to2)(extractor)
  }
  override def toOption(): OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](statement)(parameters: _*)(one)(to1, to2)(extractor)
  }

  override def single(): OneToManies2SQLToOption[A, B1, B2, E, Z] = toOption()
  override def headOption(): OneToManies2SQLToOption[A, B1, B2, E, Z] = toOption()
  override def first(): OneToManies2SQLToOption[A, B1, B2, E, Z] = toOption()
  override def list(): OneToManies2SQLToList[A, B1, B2, E, Z] = toList()
  override def traversable(): OneToManies2SQLToTraversable[A, B1, B2, E, Z] = toTraversable()
}

class OneToManies2SQLToList[A, B1, B2, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
    with SQLToList[Z, E]
    with OutputDecisions[Z, E]
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

class OneToManies2SQLToTraversable[A, B1, B2, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(val one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
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

class OneToManies2SQLToOption[A, B1, B2, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  val output = Output.single

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2]) => Z = extractor
}

//------------------------------------
// One-to-manies 3
//------------------------------------

private[scalikejdbc] trait OneToManies3Extractor[A, B1, B2, B3, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3]
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3]) => Z

  private[scalikejdbc] def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3])]),
    rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3])] = {
    val o = extractOne(rs)
    val (to1, to2, to3) = (extractTo1(rs), extractTo2(rs), extractTo3(rs))
    result.keys.find(_ == o).map { _ =>
      to1.orElse(to2).orElse(to3).map { _ =>
        val (ts1, ts2, ts3) = result.apply(o)
        result += (o -> (
          to1.map(t => if (ts1.contains(t)) ts1 else ts1 :+ t).getOrElse(ts1),
          to2.map(t => if (ts2.contains(t)) ts2 else ts2 :+ t).getOrElse(ts2),
          to3.map(t => if (ts3.contains(t)) ts3 else ts3 :+ t).getOrElse(ts3)
        ))
      }.getOrElse(result)
    }.getOrElse {
      result += (
        o -> (
          to1.map(t => Vector(t)).getOrElse(Vector()),
          to2.map(t => Vector(t)).getOrElse(Vector()),
          to3.map(t => Vector(t)).getOrElse(Vector())
        )
      )
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3])]())(processResultSet).map {
      case (one, (t1, t2, t3)) => extractor(one, t1, t2, t3)
    }
  }

}

class OneToManies3SQL[A, B1, B2, B3, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z): OneToManies3SQL[A, B1, B2, B3, HasExtractor, Z] = {
    new OneToManies3SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3)(extractor)
  }
  override def toTraversable(): OneToManies3SQLToTraversable[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToTraversable[A, B1, B2, B3, E, Z](statement)(parameters: _*)(one)(to1, to2, to3)(extractor)
  }
  override def toList(): OneToManies3SQLToList[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToList[A, B1, B2, B3, E, Z](statement)(parameters: _*)(one)(to1, to2, to3)(extractor)
  }
  override def toOption(): OneToManies3SQLToOption[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToOption[A, B1, B2, B3, E, Z](statement)(parameters: _*)(one)(to1, to2, to3)(extractor)
  }

  override def single(): OneToManies3SQLToOption[A, B1, B2, B3, E, Z] = toOption()
  override def headOption(): OneToManies3SQLToOption[A, B1, B2, B3, E, Z] = toOption()
  override def first(): OneToManies3SQLToOption[A, B1, B2, B3, E, Z] = toOption()
  override def list(): OneToManies3SQLToList[A, B1, B2, B3, E, Z] = toList()
  override def traversable(): OneToManies3SQLToTraversable[A, B1, B2, B3, E, Z] = toTraversable()
}

class OneToManies3SQLToList[A, B1, B2, B3, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3]) => Z = extractor
}

class OneToManies3SQLToTraversable[A, B1, B2, B3, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3]) => Z = extractor
}

class OneToManies3SQLToOption[A, B1, B2, B3, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  val output = Output.single

  import GeneralizedTypeConstraintsForWithExtractor._
  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, statement, parameters, extractor)))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3]) => Z = extractor
}

//------------------------------------
// One-to-manies 4
//------------------------------------

private[scalikejdbc] trait OneToManies4Extractor[A, B1, B2, B3, B4, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  private[scalikejdbc] def extractOne: WrappedResultSet => A
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1]
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2]
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3]
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4]
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z

  private[scalikejdbc] def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4])]),
    rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4])] = {
    val o = extractOne(rs)
    val (to1, to2, to3, to4) = (extractTo1(rs), extractTo2(rs), extractTo3(rs), extractTo4(rs))
    result.keys.find(_ == o).map { _ =>
      to1.orElse(to2).orElse(to3).orElse(to4).map { _ =>
        val (ts1, ts2, ts3, ts4) = result.apply(o)
        result += (o -> (
          to1.map(t => if (ts1.contains(t)) ts1 else ts1 :+ t).getOrElse(ts1),
          to2.map(t => if (ts2.contains(t)) ts2 else ts2 :+ t).getOrElse(ts2),
          to3.map(t => if (ts3.contains(t)) ts3 else ts3 :+ t).getOrElse(ts3),
          to4.map(t => if (ts4.contains(t)) ts4 else ts4 :+ t).getOrElse(ts4)
        ))
      }.getOrElse(result)
    }.getOrElse {
      result += (
        o -> (
          to1.map(t => Vector(t)).getOrElse(Vector()),
          to2.map(t => Vector(t)).getOrElse(Vector()),
          to3.map(t => Vector(t)).getOrElse(Vector()),
          to4.map(t => Vector(t)).getOrElse(Vector())
        )
      )
    }
  }

  private[scalikejdbc] def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z): Traversable[Z] = {
    session.foldLeft(statement, parameters: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4])]())(processResultSet).map {
      case (one, (t1, t2, t3, t4)) => extractor(one, t1, t2, t3, t4)
    }
  }

}

class OneToManies4SQL[A, B1, B2, B3, B4, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z): OneToManies4SQL[A, B1, B2, B3, B4, HasExtractor, Z] = {
    new OneToManies4SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toTraversable(): OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toList(): OneToManies4SQLToList[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToList[A, B1, B2, B3, B4, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toOption(): OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4)(extractor)
  }

  override def single(): OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z] = toOption()
  override def headOption(): OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z] = toOption()
  override def first(): OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z] = toOption()
  override def list(): OneToManies4SQLToList[A, B1, B2, B3, B4, E, Z] = toList()
  override def traversable(): OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E, Z] = toTraversable()
}

class OneToManies4SQLToList[A, B1, B2, B3, B4, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
    with SQLToList[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor).toList)
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4] = to4
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z = extractor
}

class OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
    with SQLToTraversable[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, statement, parameters, extractor))
  }

  private[scalikejdbc] def extractOne: WrappedResultSet => A = one
  private[scalikejdbc] def extractTo1: WrappedResultSet => Option[B1] = to1
  private[scalikejdbc] def extractTo2: WrappedResultSet => Option[B2] = to2
  private[scalikejdbc] def extractTo3: WrappedResultSet => Option[B3] = to3
  private[scalikejdbc] def extractTo4: WrappedResultSet => Option[B4] = to4
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z = extractor
}

class OneToManies4SQLToOption[A, B1, B2, B3, B4, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

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
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z = extractor
}

//------------------------------------
// One-to-manies 5
//------------------------------------

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

class OneToManies5SQL[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, use #map((A,B) =>Z) instead."))(output)
    with AllOutputDecisionsUnsupported[Z, E] {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z): OneToManies5SQL[A, B1, B2, B3, B4, B5, HasExtractor, Z] = {
    new OneToManies5SQL(statement)(parameters: _*)(output)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toTraversable(): OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toList(): OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toOption(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z](statement)(parameters: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }

  override def single(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = toOption()
  override def headOption(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = toOption()
  override def first(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = toOption()
  override def list(): OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z] = toList()
  override def traversable(): OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z] = toTraversable()
}

class OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.list)
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

class OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.traversable)
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

class OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](override val statement: String)(override val parameters: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](statement)(parameters: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, use #map((A,B) =>Z) instead."))(Output.single)
    with SQLToOption[Z, E]
    with AllOutputDecisionsUnsupported[Z, E]
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

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
  private[scalikejdbc] def transform: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z = extractor
}
