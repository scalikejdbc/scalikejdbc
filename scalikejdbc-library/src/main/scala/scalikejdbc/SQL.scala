/*
 * Copyright 2012 Kazuhiro Sera
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
import java.sql.PreparedStatement
import scala.collection.immutable.ListMap

/**
 * SQL abstraction's companion object.
 *
 * {{{
 *   ConnectionPool.singletion("jdbc:...","user","password")
 *   case class User(id: Int, name: String)
 *
 *   val users = DB readOnly { session =>
 *     SQL("select * from user").map { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }.list.apply()
 *   }
 *
 *   DB autoCommit { session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   DB localTx { session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   using(DB(ConnectionPool.borrow())) { db =>
 *     db.begin()
 *     try {
 *       DB withTx { session =>
 *         SQL("update user set name = ? where id = ?").bind("Alice", 123).update.apply()
 *       }
 *       db.commit()
 *     } catch { case e =>
 *       db.rollbackIfActive()
 *       throw e
 *     }
 *   }
 * }}}
 */
object SQL {

  private[scalikejdbc] def noExtractor[A](message: String): WrappedResultSet => A = { (rs: WrappedResultSet) =>
    throw new IllegalStateException(message)
  }

  private[scalikejdbc] object Output extends Enumeration {
    val single, first, list, traversable = Value
  }

  def apply[A](sql: String): SQL[A, NoExtractor] = createSQL(sql)(Seq(): _*)(noExtractor[A](
    "If you see this message, it's a ScalikeJDBC's bug. Please report us."
  ))()

}

/**
 * Simple [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object createSQL {

  /**
   * Provides a [[scalikejdbc.SQL]] instance.
   * @param sql SQL template
   * @param params   parameters
   * @param extractor extractor function
   * @param output output type
   * @tparam A return type
   * @return SQL instance
   */
  def apply[A, E <: WithExtractor](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable): SQL[A, E] = output match {
    case Output.single | Output.first => new SQLToOption[A, E](sql)(params: _*)(extractor)(output)
    case Output.list => new SQLToList[A, E](sql)(params: _*)(extractor)(output)
    case Output.traversable => new SQLToTraversable[A, E](sql)(params: _*)(extractor)(output)
  }

}

/**
 * Name binding [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object createNameBindingSQL {

  def validateAndConvertToNormalStatement(sql: String, params: Seq[(Symbol, Any)]): (String, Seq[Any]) = {
    val names = SQLTemplateParser.extractAllParameters(sql)
    // check all the parameters passed by #bindByName are actually used
    params.foreach {
      param =>
        names.find(_ == param._1).orElse {
          throw new IllegalStateException(ErrorMessage.BINDING_IS_IGNORED + " (" + param._1 + ")")
        }
    }
    val sqlWithPlaceHolders = SQLTemplateParser.convertToSQLWithPlaceHolders(sql)
    (sqlWithPlaceHolders, names.map {
      name =>
        params.find(_._1 == name).orElse {
          throw new IllegalArgumentException(ErrorMessage.BINDING_PARAMETER_IS_MISSING + " (" + name + ")")
        }.map(_._2).orNull[Any]
    })
  }

  /**
   * Provides a [[scalikejdbc.SQL]] instance.
   * @param sql SQL template
   * @param params named parameters
   * @param extractor extractor function
   * @param output output type
   * @tparam A return type
   * @return SQL instance
   */
  def apply[A, E <: WithExtractor](sql: String)(params: (Symbol, Any)*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable): SQL[A, E] = output match {
    case Output.single | Output.first => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToOption[A, E](_sql)(_params: _*)(extractor)(output)
    }
    case Output.list => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToList[A, E](_sql)(_params: _*)(extractor)(output)
    }
    case Output.traversable => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToTraversable[A, E](_sql)(_params: _*)(extractor)(output)
    }
  }

}

/**
 * Represents an extractor is already specified or not
 */
sealed trait WithExtractor

/**
 * Represents that this SQL already has an extractor
 */
trait HasExtractor extends WithExtractor

/**
 * Represents that this SQL doesn't have an extractor yet
 */
trait NoExtractor extends WithExtractor

/**
 * Generalized type constraints for WithExtractor
 */
object GeneralizedTypeConstraintsForWithExtractor {

  // customized error message
  @annotation.implicitNotFound(msg = "No extractor is specified. You need to call #map((WrappedResultSet) => A) before #apply().")
  sealed abstract class =:=[From, To] extends (From => To) with Serializable
  private[this] final val singleton_=:= = new =:=[WithExtractor, WithExtractor] { def apply(x: WithExtractor): WithExtractor = x }
  object =:= {
    implicit def tpEquals[A]: A =:= A = singleton_=:=.asInstanceOf[A =:= A]
  }

}

/**
 * SQL abstraction.
 *
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
abstract class SQL[A, E <: WithExtractor](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable) {

  type ThisSQL = SQL[A, E]
  type SQLWithExtractor = SQL[A, HasExtractor]

  def one(f: (WrappedResultSet) => A): OneToXRelationalSQL[A, E] = new OneToXRelationalSQL[A, E](sql)(params: _*)(output)(f)

  /**
   * Binds parameters to SQL template in order.
   * @param params parameters
   * @return SQL instance
   */
  def bind(params: Any*): SQL[A, E] = createSQL[A, E](sql)(params: _*)(extractor)(output)

  /**
   * Binds named parameters to SQL template.
   * @param paramsByName named parameters
   * @return SQL instance
   */
  def bindByName(paramsByName: (Symbol, Any)*): SQL[A, E] = {
    createNameBindingSQL(sql)(paramsByName: _*)(extractor)(output)
  }

  /**
   * Binds params for batch
   * @param params params
   * @return SQL for batch
   */
  def batch(params: Seq[Any]*): SQLBatch = {
    new SQLBatch(sql)(params: _*)
  }

  /**
   * Binds params for batch
   * @param params params
   * @return SQL for batch
   */
  def batchByName(params: Seq[(Symbol, Any)]*): SQLBatch = {
    val _sql = createNameBindingSQL.validateAndConvertToNormalStatement(sql, params.head)._1
    val _params: Seq[Seq[Any]] = params.map { p =>
      createNameBindingSQL.validateAndConvertToNormalStatement(sql, p)._2
    }
    new SQLBatch(_sql)(_params: _*)
  }

  /**
   * Aplly the operation to all elements of result set
   * @param op operation
   */
  def foreach(op: WrappedResultSet => Unit)(implicit session: DBSession): Unit = session match {
    case AutoSession => DB autoCommit (s => s.foreach(sql, params: _*)(op))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.foreach(sql, params: _*)(op))
    case _ => session.foreach(sql, params: _*)(op)
  }

  /**
   * folding into one value
   * @param z initial value
   * @param op operation
   */
  def foldLeft[A](z: A)(op: (A, WrappedResultSet) => A)(implicit session: DBSession): A = session match {
    case AutoSession => DB autoCommit (_.foldLeft(sql, params: _*)(z)(op))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (_.foldLeft(sql, params: _*)(z)(op))
    case _ => session.foldLeft(sql, params: _*)(z)(op)
  }

  /**
   * ListMaps values from each [[scalikejdbc.WrappedResultSet]] object.
   * @param extractor extractor function
   * @tparam A return type
   * @return SQL instance
   */
  def map[A](extractor: (WrappedResultSet => A)): SQL[A, HasExtractor] = {
    createSQL[A, HasExtractor](sql)(params: _*)(extractor)(output)
  }

  /**
   * Same as #single.
   * @return SQL instance
   */
  def toOption(): SQLToOption[A, E] = {
    createSQL(sql)(params: _*)(extractor)(Output.single).asInstanceOf[SQLToOption[A, E]]
  }

  /**
   * Set execution type as single.
   * @return SQL instance
   */
  def single(): SQLToOption[A, E] = toOption()

  /**
   * Same as #first.
   * @return SQL instance
   */
  def headOption(): SQLToOption[A, E] = {
    createSQL(sql)(params: _*)(extractor)(Output.first).asInstanceOf[SQLToOption[A, E]]
  }

  /**
   * Set execution type as first.
   * @return SQL instance
   */
  def first(): SQLToOption[A, E] = headOption()

  /**
   * Same as #list
   * @return SQL instance
   */
  def toList(): SQLToList[A, E] = {
    createSQL(sql)(params: _*)(extractor)(Output.list).asInstanceOf[SQLToList[A, E]]
  }

  /**
   * Set execution type as list.
   * @return SQL instance
   */
  def list(): SQLToList[A, E] = toList()

  /**
   * Same as #traversable.
   * @return SQL instance
   */
  def toTraversable(): SQLToTraversable[A, E] = {
    createSQL[A, E](sql)(params: _*)(extractor)(Output.traversable).asInstanceOf[SQLToTraversable[A, E]]
  }

  /**
   * Set execution type as traversable.
   * @return SQL instance
   */
  def traversable(): SQLToTraversable[A, E] = toTraversable()

  /**
   * Set execution type as execute
   * @return SQL instance
   */
  def execute(): SQLExecution = {
    new SQLExecution(sql)(params: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as execute with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit) = {
    new SQLExecution(sql)(params: _*)(before)(after)
  }

  /**
   * Set execution type as executeUpdate
   * @return SQL instance
   */
  def executeUpdate(): SQLUpdate = update()

  /**
   * Set execution type as executeUpdate with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeUpdateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    updateWithFilters(before, after)
  }

  /**
   * Set execution type as executeUpdate
   * @return SQL instance
   */
  def update(): SQLUpdate = {
    new SQLUpdate(sql)(params: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as executeUpdate with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def updateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    new SQLUpdate(sql)(params: _*)(before)(after)
  }

  /**
   * Set execution type as updateAndreturnGeneratedKey
   * @return SQL instance
   */
  def updateAndReturnGeneratedKey(): SQLUpdateWithGeneratedKey = updateAndReturnGeneratedKey(1)

  def updateAndReturnGeneratedKey(name: String): SQLUpdateWithGeneratedKey = new SQLUpdateWithGeneratedKey(sql)(params: _*)(name)

  def updateAndReturnGeneratedKey(index: Int): SQLUpdateWithGeneratedKey = new SQLUpdateWithGeneratedKey(sql)(params: _*)(index)

}

/**
 * SQL which execute [[java.sql.Statement#executeBatch()]].
 * @param sql SQL template
 * @param params parameters
 */
class SQLBatch(sql: String)(params: Seq[Any]*) {

  def apply()(implicit session: DBSession): Seq[Int] = session match {
    case AutoSession => DB autoCommit (s => s.batch(sql, params: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.batch(sql, params: _*))
    case _ => session.batch(sql, params: _*)
  }

}

/**
 * SQL which execute [[java.sql.Statement#execute()]].
 * @param sql SQL template
 * @param params parameters
 * @param before before filter
 * @param after after filter
 */
class SQLExecution(sql: String)(params: Any*)(before: (PreparedStatement) => Unit)(after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Boolean = session match {
    case AutoSession => DB autoCommit (s => s.executeWithFilters(before, after, sql, params: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.executeWithFilters(before, after, sql, params: _*))
    case _ => session.executeWithFilters(before, after, sql, params: _*)
  }

}

/**
 * SQL which execute [[java.sql.Statement#executeUpdate()]].
 * @param sql SQL template
 * @param params parameters
 * @param before before filter
 * @param after after filter
 */
class SQLUpdate(sql: String)(params: Any*)(before: (PreparedStatement) => Unit)(after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Int = session match {
    case AutoSession => DB autoCommit (s => s.updateWithFilters(before, after, sql, params: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.updateWithFilters(before, after, sql, params: _*))
    case _ => session.updateWithFilters(before, after, sql, params: _*)
  }

}

/**
 * SQL which execute [[java.sql.Statement#executeUpdate()]] and get generated key value.
 * @param sql SQL template
 * @param params parameters
 */
class SQLUpdateWithGeneratedKey(sql: String)(params: Any*)(key: Any) {

  def apply()(implicit session: DBSession): Long = session match {
    case AutoSession => DB autoCommit (s => (s.updateAndReturnSpecifiedGeneratedKey(sql, params: _*)(key)))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => (s.updateAndReturnSpecifiedGeneratedKey(sql, params: _*)(key)))
    case _ => (session.updateAndReturnSpecifiedGeneratedKey(sql, params: _*)(key))
  }

}

/**
 * SQL which execute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.collection.Traversable]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToTraversable[A, E <: WithExtractor](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A, E](sql)(params: _*)(extractor)(output) {

  import GeneralizedTypeConstraintsForWithExtractor._

  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[A] = session match {
    case AutoSession => DB readOnly (s => s.traversable(sql, params: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.traversable(sql, params: _*)(extractor))
    case _ => session.traversable(sql, params: _*)(extractor)
  }

}

/**
 * SQL which execute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.collection.immutable.List]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToList[A, E <: WithExtractor](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A, E](sql)(params: _*)(extractor)(output) {

  import GeneralizedTypeConstraintsForWithExtractor._

  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[A] = session match {
    case AutoSession => DB readOnly (s => s.list(sql, params: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.list(sql, params: _*)(extractor))
    case _ => session.list(sql, params: _*)(extractor)
  }

}

/**
 * SQL which execute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.Option]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToOption[A, E <: WithExtractor](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.single)
    extends SQL[A, E](sql)(params: _*)(extractor)(output) {

  import GeneralizedTypeConstraintsForWithExtractor._

  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): Option[A] = output match {
    case Output.single =>
      session match {
        case AutoSession => DB readOnly (s => s.single(sql, params: _*)(extractor))
        case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.single(sql, params: _*)(extractor))
        case _ => session.single(sql, params: _*)(extractor)
      }
    case Output.first =>
      session match {
        case AutoSession => DB readOnly (s => s.first(sql, params: _*)(extractor))
        case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.first(sql, params: _*)(extractor))
        case _ => session.first(sql, params: _*)(extractor)
      }
  }

}

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
