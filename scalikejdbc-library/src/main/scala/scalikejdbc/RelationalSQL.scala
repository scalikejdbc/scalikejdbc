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

  protected def toSingle(rows: Traversable[Z]): Option[Z] = {
    if (rows.size > 1) throw new TooManyRowsException(1, rows.size)
    else rows.headOption
  }

  protected def executeQuery[R[Z]](session: DBSession, op: DBSession => R[Z]): R[Z] = try {
    session match {
      case AutoSession => DB readOnly (s => op(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => op(s))
      case _ => op(session)
    }
  } catch { case e: Exception => OneToXSQL.handleException(e) }

}

object OneToXSQL {

  def handleException(e: Exception) = e match {
    case invalidColumn: InvalidColumnNameException =>
      throw new ResultSetExtractorException("Failed to extract ResultSet because the specified column name (" + invalidColumn.name + ") is invalid.")
    case e: Exception => throw e
  }

}

class OneToXSQL[A, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-one/one-to-many operation needs toOne(RS => Option[B]).map((A,B) => A) or toMany(RS => Option[B]).map((A,Seq(B) => A)."))(output) {

  def toOne[B](to: WrappedResultSet => Option[B]): OneToOneSQL[A, B, E, Z] = {
    new OneToOneSQL(sql)(params: _*)(output)(one)(to)((a, b) => a.asInstanceOf[Z])
  }

  def toMany[B](to: WrappedResultSet => Option[B]): OneToManySQL[A, B, E, Z] = {
    new OneToManySQL(sql)(params: _*)(output)(one)(to)((a, bs) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2]): OneToManies2SQL[A, B1, B2, E, Z] = {
    new OneToManies2SQL(sql)(params: _*)(output)(one)(to1, to2)((a, bs1, bs2) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3]): OneToManies3SQL[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQL(sql)(params: _*)(output)(one)(to1, to2, to3)((a, bs1, bs2, bs3) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3, B4](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4]): OneToManies4SQL[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQL(sql)(params: _*)(output)(one)(to1, to2, to3, to4)((a, bs1, bs2, bs3, bs4) => a.asInstanceOf[Z])
  }

  def toManies[B1, B2, B3, B4, B5](to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2],
    to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4],
    to5: WrappedResultSet => Option[B5]): OneToManies5SQL[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQL(sql)(params: _*)(output)(one)(to1, to2, to3, to4, to5)((a, bs1, bs2, bs3, bs4, bs5) => a.asInstanceOf[Z])
  }

}

//------------------------------------
// One-to-one
//------------------------------------

private[scalikejdbc] trait OneToOneExtractor[A, B, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo: WrappedResultSet => Option[B]

  protected def processResultSet(oneToOne: (LinkedHashMap[A, Option[B]]), rs: WrappedResultSet): LinkedHashMap[A, Option[B]] = {
    val o = extractOne(rs)
    oneToOne.keys.find(_ == o).map {
      case Some(found) => throw new IllegalRelationshipException(ErrorMessage.INVALID_ONE_TO_ONE_RELATION)
    }.getOrElse {
      extractTo(rs).map { that => oneToOne += (o -> Some(that)) }.getOrElse(oneToOne += (o -> None))
    }
  }

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, B) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, Option[B]]())(processResultSet).map {
      case (one, Some(to)) => extractor(one, to)
      case (one, None) => one.asInstanceOf[Z]
    }
  }

}

class OneToOneSQL[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, B) => Z): OneToOneSQL[A, B, HasExtractor, Z] = {
    new OneToOneSQL(sql)(params: _*)(output)(one)(toOne)(extractor)
  }

  override def toTraversable(): OneToOneSQLToTraversable[A, B, E, Z] = {
    new OneToOneSQLToTraversable[A, B, E, Z](sql)(params: _*)(one)(toOne)(extractor)
  }

  override def toList(): OneToOneSQLToList[A, B, E, Z] = {
    new OneToOneSQLToList[A, B, E, Z](sql)(params: _*)(one)(toOne)(extractor)
  }

  override def toOption(): OneToOneSQLToOption[A, B, E, Z] = {
    new OneToOneSQLToOption[A, B, E, Z](sql)(params: _*)(one)(toOne)(extractor)
  }

}

class OneToOneSQLToTraversable[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toOne
}

class OneToOneSQLToList[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toOne
}

class OneToOneSQLToOption[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-one extractor(one(RS => A).toOne(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToOneExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toOne
}

//------------------------------------
// One-to-many
//------------------------------------

private[scalikejdbc] trait OneToManyExtractor[A, B, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo: WrappedResultSet => Option[B]

  protected def processResultSet(oneToMany: (LinkedHashMap[A, Seq[B]]), rs: WrappedResultSet): LinkedHashMap[A, Seq[B]] = {
    val o = extractOne(rs)
    oneToMany.keys.find(_ == o).map { _ =>
      extractTo(rs).map(many => oneToMany += (o -> (oneToMany.apply(o) :+ many))).getOrElse(oneToMany)
    }.getOrElse {
      extractTo(rs).map(many => oneToMany += (o -> Vector(many))).getOrElse(oneToMany += (o -> Nil))
    }
  }

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B]) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, (Seq[B])]())(processResultSet).map {
      case (one, (to)) => extractor(one, to)
    }
  }

}

class OneToManySQL[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, Seq[B]) => Z): OneToManySQL[A, B, HasExtractor, Z] = {
    new OneToManySQL[A, B, HasExtractor, Z](sql)(params: _*)(output)(one)(toMany)(extractor)
  }

  override def toTraversable(): OneToManySQLToTraversable[A, B, E, Z] = {
    new OneToManySQLToTraversable[A, B, E, Z](sql)(params: _*)(one)(toMany)(extractor)
  }

  override def toList(): OneToManySQLToList[A, B, E, Z] = {
    new OneToManySQLToList[A, B, E, Z](sql)(params: _*)(one)(toMany)(extractor)
  }

  override def toOption(): OneToManySQLToOption[A, B, E, Z] = {
    new OneToManySQLToOption[A, B, E, Z](sql)(params: _*)(one)(toMany)(extractor)
  }

}

class OneToManySQLToList[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toMany
}

class OneToManySQLToTraversable[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toMany
}

class OneToManySQLToOption[A, B, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, Seq[B]) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToManyExtractor[A, B, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo: WrappedResultSet => Option[B] = toMany
}

//------------------------------------
// One-to-manies 2
//------------------------------------

private[scalikejdbc] trait OneToManies2Extractor[A, B1, B2, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo1: WrappedResultSet => Option[B1]
  protected def extractTo2: WrappedResultSet => Option[B2]

  protected def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2])]), rs: WrappedResultSet): LinkedHashMap[A, (Seq[B1], Seq[B2])] = {
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

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2]) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2])]())(processResultSet).map {
      case (one, (t1, t2)) => extractor(one, t1, t2)
    }
  }

}

class OneToManies2SQL[A, B1, B2, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1], RS => Option[B2])) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, Seq[B1], Seq[B2]) => Z): OneToManies2SQL[A, B1, B2, HasExtractor, Z] = {
    new OneToManies2SQL(sql)(params: _*)(output)(one)(to1, to2)(extractor)
  }
  override def toTraversable(): OneToManies2SQLToTraversable[A, B1, B2, E, Z] = {
    new OneToManies2SQLToTraversable[A, B1, B2, E, Z](sql)(params: _*)(one)(to1, to2)(extractor)
  }
  override def toList(): OneToManies2SQLToList[A, B1, B2, E, Z] = {
    new OneToManies2SQLToList[A, B1, B2, E, Z](sql)(params: _*)(one)(to1, to2)(extractor)
  }
  override def toOption(): OneToManies2SQLToOption[A, B1, B2, E, Z] = {
    new OneToManies2SQLToOption[A, B1, B2, E, Z](sql)(params: _*)(one)(to1, to2)(extractor)
  }

}

class OneToManies2SQLToList[A, B1, B2, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
}

class OneToManies2SQLToTraversable[A, B1, B2, E <: WithExtractor, Z](sql: String)(params: Any*)(val one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
}

class OneToManies2SQLToOption[A, B1, B2, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2])(extractor: (A, Seq[B1], Seq[B2]) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToManies2Extractor[A, B1, B2, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
}

//------------------------------------
// One-to-manies 3
//------------------------------------

private[scalikejdbc] trait OneToManies3Extractor[A, B1, B2, B3, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo1: WrappedResultSet => Option[B1]
  protected def extractTo2: WrappedResultSet => Option[B2]
  protected def extractTo3: WrappedResultSet => Option[B3]

  protected def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3])]),
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

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3])]())(processResultSet).map {
      case (one, (t1, t2, t3)) => extractor(one, t1, t2, t3)
    }
  }

}

class OneToManies3SQL[A, B1, B2, B3, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z): OneToManies3SQL[A, B1, B2, B3, HasExtractor, Z] = {
    new OneToManies3SQL(sql)(params: _*)(output)(one)(to1, to2, to3)(extractor)
  }
  override def toTraversable(): OneToManies3SQLToTraversable[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToTraversable[A, B1, B2, B3, E, Z](sql)(params: _*)(one)(to1, to2, to3)(extractor)
  }
  override def toList(): OneToManies3SQLToList[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToList[A, B1, B2, B3, E, Z](sql)(params: _*)(one)(to1, to2, to3)(extractor)
  }
  override def toOption(): OneToManies3SQLToOption[A, B1, B2, B3, E, Z] = {
    new OneToManies3SQLToOption[A, B1, B2, B3, E, Z](sql)(params: _*)(one)(to1, to2, to3)(extractor)
  }

}

class OneToManies3SQLToList[A, B1, B2, B3, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
}

class OneToManies3SQLToTraversable[A, B1, B2, B3, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
}

class OneToManies3SQLToOption[A, B1, B2, B3, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3])(extractor: (A, Seq[B1], Seq[B2], Seq[B3]) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToManies3Extractor[A, B1, B2, B3, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
}

//------------------------------------
// One-to-manies 4
//------------------------------------

private[scalikejdbc] trait OneToManies4Extractor[A, B1, B2, B3, B4, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo1: WrappedResultSet => Option[B1]
  protected def extractTo2: WrappedResultSet => Option[B2]
  protected def extractTo3: WrappedResultSet => Option[B3]
  protected def extractTo4: WrappedResultSet => Option[B4]

  protected def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4])]),
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

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4])]())(processResultSet).map {
      case (one, (t1, t2, t3, t4)) => extractor(one, t1, t2, t3, t4)
    }
  }

}

class OneToManies4SQL[A, B1, B2, B3, B4, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z): OneToManies4SQL[A, B1, B2, B3, B4, HasExtractor, Z] = {
    new OneToManies4SQL(sql)(params: _*)(output)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toTraversable(): OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toList(): OneToManies4SQLToList[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToList[A, B1, B2, B3, B4, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4)(extractor)
  }
  override def toOption(): OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z] = {
    new OneToManies4SQLToOption[A, B1, B2, B3, B4, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4)(extractor)
  }

}

class OneToManies4SQLToList[A, B1, B2, B3, B4, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
}

class OneToManies4SQLToTraversable[A, B1, B2, B3, B4, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
}

class OneToManies4SQLToOption[A, B1, B2, B3, B4, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4]) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToManies4Extractor[A, B1, B2, B3, B4, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
}

//------------------------------------
// One-to-manies 5
//------------------------------------

private[scalikejdbc] trait OneToManies5Extractor[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z]
    extends SQL[Z, E]
    with RelationalSQLResultSetOperations[Z] {

  protected def extractOne: WrappedResultSet => A
  protected def extractTo1: WrappedResultSet => Option[B1]
  protected def extractTo2: WrappedResultSet => Option[B2]
  protected def extractTo3: WrappedResultSet => Option[B3]
  protected def extractTo4: WrappedResultSet => Option[B4]
  protected def extractTo5: WrappedResultSet => Option[B5]

  protected def processResultSet(result: (LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5])]),
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

  protected def toTraversable(session: DBSession, sql: String, params: Seq[_], extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z): Traversable[Z] = {
    session.foldLeft(sql, params: _*)(LinkedHashMap[A, (Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5])]())(processResultSet).map {
      case (one, (t1, t2, t3, t4, t5)) => extractor(one, t1, t2, t3, t4, t5)
    }
  }

}

class OneToManies5SQL[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQL[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1]...)) is specified, please use #map((A,B) =>Z) instead."))(output) {

  def map(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z): OneToManies5SQL[A, B1, B2, B3, B4, B5, HasExtractor, Z] = {
    new OneToManies5SQL(sql)(params: _*)(output)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toTraversable(): OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toList(): OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }
  override def toOption(): OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z] = {
    new OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E, Z](sql)(params: _*)(one)(to1, to2, to3, to4, to5)(extractor)
  }

}

class OneToManies5SQLToList[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQLToList[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toManies(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.list)
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[Z] = {
    executeQuery[List](session, (session: DBSession) => toTraversable(session, sql, params, extractor).toList)
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
  protected def extractTo5: WrappedResultSet => Option[B5] = to5
}

class OneToManies5SQLToTraversable[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQLToTraversable[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.traversable)
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[Z] = {
    executeQuery[Traversable](session, (session: DBSession) => toTraversable(session, sql, params, extractor))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
  protected def extractTo5: WrappedResultSet => Option[B5] = to5
}

class OneToManies5SQLToOption[A, B1, B2, B3, B4, B5, E <: WithExtractor, Z](sql: String)(params: Any*)(one: WrappedResultSet => A)(to1: WrappedResultSet => Option[B1], to2: WrappedResultSet => Option[B2], to3: WrappedResultSet => Option[B3], to4: WrappedResultSet => Option[B4], to5: WrappedResultSet => Option[B5])(extractor: (A, Seq[B1], Seq[B2], Seq[B3], Seq[B4], Seq[B5]) => Z)
    extends SQLToOption[Z, E](sql)(params: _*)(SQL.noExtractor[Z]("one-to-many extractor(one(RS => A).toMany(RS => Option[B1])) is specified, please use #map((A,B) =>Z) instead."))(Output.single)
    with OneToManies5Extractor[A, B1, B2, B3, B4, B5, E, Z] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[Z] = {
    executeQuery[Option](session, (session: DBSession) => toSingle(toTraversable(session, sql, params, extractor)))
  }

  protected def extractOne: WrappedResultSet => A = one
  protected def extractTo1: WrappedResultSet => Option[B1] = to1
  protected def extractTo2: WrappedResultSet => Option[B2] = to2
  protected def extractTo3: WrappedResultSet => Option[B3] = to3
  protected def extractTo4: WrappedResultSet => Option[B4] = to4
  protected def extractTo5: WrappedResultSet => Option[B5] = to5
}
