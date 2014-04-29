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

import scalikejdbc.SQL.Output
import scala.collection.mutable.LinkedHashMap
import scala.language.higherKinds

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
