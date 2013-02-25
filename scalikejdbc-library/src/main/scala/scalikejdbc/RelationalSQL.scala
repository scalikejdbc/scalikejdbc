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
import scala.collection.immutable.ListMap

//------------------------------------
// One-to-one / One-to-many
//------------------------------------

/**
 * One-to-X relationship extractor builder
 * @param sql SQL template
 * @param params parameters
 * @param output output type
 * @param one one extractor
 * @tparam A return type
 */
class OneToXRelationalSQL[A, E <: WithExtractor](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)
    extends SQL[A, E](sql)(params: _*)(SQL.noExtractor[A]("TODO"))(output) {

  def toOne[B](to: WrappedResultSet => Option[B]): OneToOneRelationalSQL[A, B, E] = {
    new OneToOneRelationalSQL(sql)(params: _*)(output)(one)(to)((a, b) => a)
  }

  def toMany[B](to: WrappedResultSet => Option[B]): OneToManyRelationalSQL[A, B, E] = {
    new OneToManyRelationalSQL(sql)(params: _*)(output)(one)(to)((a, bs) => a)
  }

}

private[scalikejdbc] trait RelationalSQLExtractor[A, B, E <: WithExtractor] { self: SQL[A, E] =>
  def extractOne: WrappedResultSet => A
  def extractTo: WrappedResultSet => Option[B]
}

//------------------------------------
// One-to-one
//------------------------------------

/**
 * One-to-one relationship extractor builder
 * @param sql SQL template
 * @param params parameters
 * @param output output type
 * @param one one1
 * @param toOne one2
 * @param extractor  extractor function
 * @tparam A return type for one1
 * @tparam B return type for one2
 */
class OneToOneRelationalSQL[A, B, E <: WithExtractor](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => A)
    extends SQL[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-one extractor(one(A).toOne(B)) is specified, please use #map((A,B) =>A) instead."))(output) {

  def map(extractor: (A, B) => A): OneToOneRelationalSQL[A, B, HasExtractor] = {
    new OneToOneRelationalSQL(sql)(params: _*)(output)(one)(toOne)(extractor)
  }

  override def toTraversable(): OneToOneRelationalSQLToTraversable[A, B, E] = {
    new OneToOneRelationalSQLToTraversable[A, B, E](sql)(params: _*)(one)(toOne)(extractor)
  }

  override def toList(): OneToOneRelationalSQLToList[A, B, E] = {
    new OneToOneRelationalSQLToList[A, B, E](sql)(params: _*)(one)(toOne)(extractor)
  }

  override def toOption(): OneToOneRelationalSQLToOption[A, B, E] = {
    new OneToOneRelationalSQLToOption[A, B, E](sql)(params: _*)(one)(toOne)(extractor)
  }

}

private[scalikejdbc] trait OneToOneResultSetOperation[A, B, E <: WithExtractor] { self: RelationalSQLExtractor[A, B, E] =>
  def processResultSet(oneToOne: (ListMap[A, Option[B]]), rs: WrappedResultSet): ListMap[A, Option[B]] = {
    val o = extractOne(rs)
    oneToOne.keys.find(_ == o).map {
      case Some(found) =>
        throw new IllegalStateException(ErrorMessage.INVALID_ONE_TO_ONE_RELATION)
    }.getOrElse {
      extractTo(rs).map { that => oneToOne.updated(o, Some(that)) }.getOrElse(oneToOne.updated(o, None))
    }
  }
}

class OneToOneRelationalSQLToTraversable[A, B, E <: WithExtractor](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => A)
    extends SQLToTraversable[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-one extractor(one(A).toOne(B)) is specified, please use #map((A,B) =>A) instead."))(Output.traversable)
    with RelationalSQLExtractor[A, B, E]
    with OneToOneResultSetOperation[A, B, E] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[A] = {
    def operate(session: DBSession): Traversable[A] = {
      session.foldLeft(sql, params: _*)(ListMap[A, Option[B]]())(processResultSet).map {
        case (one, Some(toOne)) => extractor(one, toOne)
        case (one, None) => one
      }
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

  def extractOne: WrappedResultSet => A = one
  def extractTo: WrappedResultSet => Option[B] = toOne
}

class OneToOneRelationalSQLToList[A, B, E <: WithExtractor](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => A)
    extends SQLToList[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-one extractor(one(A).toOne(B)) is specified, please use #map((A,B) =>A) instead."))(Output.list)
    with RelationalSQLExtractor[A, B, E]
    with OneToOneResultSetOperation[A, B, E] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): List[A] = {
    def operate(session: DBSession): List[A] = {
      session.foldLeft(sql, params: _*)(ListMap[A, Option[B]]())(processResultSet).map {
        case (one, Some(toOne)) => extractor(one, toOne)
        case (one, None) => one
      }.toList
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

  def extractOne: WrappedResultSet => A = one
  def extractTo: WrappedResultSet => Option[B] = toOne
}

class OneToOneRelationalSQLToOption[A, B, E <: WithExtractor](sql: String)(params: Any*)(one: WrappedResultSet => A)(toOne: WrappedResultSet => Option[B])(extractor: (A, B) => A)
    extends SQLToOption[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-one extractor(one(A).toOne(B)) is specified, please use #map((A,B) =>A) instead."))(Output.single)
    with RelationalSQLExtractor[A, B, E]
    with OneToOneResultSetOperation[A, B, E] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[A] = {
    def operate(session: DBSession): Option[A] = {
      val rows = session.foldLeft(sql, params: _*)(ListMap[A, Option[B]]())(processResultSet).map {
        case (one, Some(toOne)) => extractor(one, toOne)
        case (one, None) => one
      }
      if (rows.size > 1) {
        throw new TooManyRowsException(1, rows.size)
      } else {
        rows.headOption
      }
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

  def extractOne: WrappedResultSet => A = one
  def extractTo: WrappedResultSet => Option[B] = toOne
}
//------------------------------------
// One-to-many
//------------------------------------

/**
 * One-to-one relationship extractor builder
 * @param sql SQL template
 * @param params parameters
 * @param output output type
 * @param one one
 * @param toMany many
 * @param extractor  extractor function
 * @tparam A return type for one
 * @tparam B return type for many
 */
class OneToManyRelationalSQL[A, B, E <: WithExtractor](sql: String)(params: Any*)(output: Output.Value = Output.traversable)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, List[B]) => A)
    extends SQL[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-many extractor(one(A).toMany(B)) is specified, please use #map((A,B) =>A) instead."))(output) {

  def map(extractor: (A, List[B]) => A): OneToManyRelationalSQL[A, B, HasExtractor] = {
    new OneToManyRelationalSQL(sql)(params: _*)(output)(one)(toMany)(extractor)
  }

  override def toTraversable(): OneToManyRelationalSQLToTraversable[A, B, E] = {
    new OneToManyRelationalSQLToTraversable[A, B, E](sql)(params: _*)(one)(toMany)(extractor)
  }

  override def toList(): OneToManyRelationalSQLToList[A, B, E] = {
    new OneToManyRelationalSQLToList[A, B, E](sql)(params: _*)(one)(toMany)(extractor)
  }

  override def toOption(): OneToManyRelationalSQLToOption[A, B, E] = {
    new OneToManyRelationalSQLToOption[A, B, E](sql)(params: _*)(one)(toMany)(extractor)
  }

}

private[scalikejdbc] trait OneToManyResultSetOperation[A, B, E <: WithExtractor] { self: RelationalSQLExtractor[A, B, E] =>
  def processResultSet(oneToMany: (ListMap[A, List[B]]), rs: WrappedResultSet): ListMap[A, List[B]] = {
    val o = self.extractOne(rs)
    oneToMany.keys.find(_ == o).map { _ =>
      self.extractTo(rs).map(many => oneToMany.updated(o, oneToMany.apply(o) :+ many)).getOrElse(oneToMany)
    }.getOrElse {
      self.extractTo(rs).map(many => oneToMany.updated(o, List(many))).getOrElse(oneToMany)
    }
  }
}

class OneToManyRelationalSQLToList[A, B, E <: WithExtractor](sql: String)(params: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, List[B]) => A)
    extends SQLToList[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-many extractor(one(A).toMany(B)) is specified, please use #map((A,B) =>A) instead."))(Output.list)
    with RelationalSQLExtractor[A, B, E]
    with OneToManyResultSetOperation[A, B, E] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[A] = {
    def operate(session: DBSession): List[A] = {
      session.foldLeft(sql, params: _*)(ListMap[A, List[B]]())(processResultSet).map { case (one, many) => extractor(one, many) }.toList
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

  def extractOne: WrappedResultSet => A = one
  def extractTo: WrappedResultSet => Option[B] = toMany
}

class OneToManyRelationalSQLToTraversable[A, B, E <: WithExtractor](sql: String)(params: Any*)(val one: WrappedResultSet => A)(val toMany: WrappedResultSet => Option[B])(extractor: (A, List[B]) => A)
    extends SQLToTraversable[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-many extractor(one(A).toMany(B)) is specified, please use #map((A,B) =>A) instead."))(Output.traversable)
    with RelationalSQLExtractor[A, B, E]
    with OneToManyResultSetOperation[A, B, E] {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[A] = {
    def operate(session: DBSession): Traversable[A] = {
      session.foldLeft(sql, params: _*)(ListMap[A, List[B]]())(processResultSet).map { case (one, many) => extractor(one, many) }
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

  def extractOne: WrappedResultSet => A = one
  def extractTo: WrappedResultSet => Option[B] = toMany
}

class OneToManyRelationalSQLToOption[A, B, E <: WithExtractor](sql: String)(params: Any*)(one: WrappedResultSet => A)(toMany: WrappedResultSet => Option[B])(extractor: (A, List[B]) => A)
    extends SQLToOption[A, E](sql)(params: _*)(SQL.noExtractor[A]("one-to-many extractor(one(A).toMany(B)) is specified, please use #map((A,B) =>A) instead."))(Output.single) {

  import GeneralizedTypeConstraintsForWithExtractor._

  override def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Option[A] = {
    def operate(session: DBSession): Option[A] = {
      def processResultSet(oneToMany: (ListMap[A, List[B]]), rs: WrappedResultSet): ListMap[A, List[B]] = {
        val o = one(rs)
        if (oneToMany.contains(o)) throw new IllegalStateException(ErrorMessage.INVALID_ONE_TO_ONE_RELATION)
        toMany(rs).map(many => oneToMany.updated(o, List(many))).getOrElse(oneToMany)
      }
      session.foldLeft(sql, params: _*)(ListMap[A, List[B]]())(processResultSet).map { case (one, many) => extractor(one, many) }.headOption
    }
    session match {
      case AutoSession => DB readOnly (s => operate(s))
      case NamedAutoSession(name) => NamedDB(name) readOnly (s => operate(s))
      case _ => operate(session)
    }
  }

}
