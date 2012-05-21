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

  private[scalikejdbc] object Output extends Enumeration {
    val single, first, list, traversable = Value
  }

  def apply[A](sql: String): SQL[A] = createSQL(sql)(Seq(): _*)()()

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
  def apply[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A = (rs: WrappedResultSet) => {
    throw new IllegalStateException(ErrorMessage.NO_EXTRACTOR_SPECIFIED)
  })(output: Output.Value = Output.traversable): SQL[A] = output match {
    case Output.single | Output.first => new SQLToOption(sql)(params: _*)(extractor)(output)
    case Output.list => new SQLToList(sql)(params: _*)(extractor)(output)
    case Output.traversable => new SQLToTraversable(sql)(params: _*)(extractor)(output)
  }

}

/**
 * Name binding [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object createNameBindingSQL {

  private def validateAndConvertToNormalStatement(sql: String, params: Seq[(Symbol, Any)]): (String, Seq[Any]) = {
    val names = ExecutableSQLParser.extractAllParameters(sql)
    // check all the paramters passed by #bindByName are actually used
    params.map {
      param =>
        names.find(_ == param._1).orElse {
          throw new IllegalStateException(ErrorMessage.BINDING_IS_IGNORED + " (" + param._1 + ")")
        }
    }
    val sqlWithPlaceHolders = ExecutableSQLParser.convertToSQLWithPlaceHolders(sql)
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
  def apply[A](sql: String)(params: (Symbol, Any)*)(extractor: WrappedResultSet => A = (rs: WrappedResultSet) => {
    throw new IllegalStateException(ErrorMessage.NO_EXTRACTOR_SPECIFIED)
  })(output: Output.Value = Output.traversable): SQL[A] = output match {
    case Output.single | Output.first => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToOption(_sql)(_params: _*)(extractor)(output)
    }
    case Output.list => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToList(_sql)(_params: _*)(extractor)(output)
    }
    case Output.traversable => {
      val (_sql, _params) = validateAndConvertToNormalStatement(sql, params)
      new SQLToTraversable(_sql)(_params: _*)(extractor)(output)
    }
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
abstract class SQL[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable) {

  /**
   * Bind parameters to SQL template in order.
   * @param params parameters
   * @return SQL instance
   */
  def bind(params: Any*): SQL[A] = createSQL[A](sql)(params: _*)(extractor)(output)

  /**
   * Bind named parameters to SQL template.
   * @param paramsByName named parameters
   * @return SQL instance
   */
  def bindByName(paramsByName: (Symbol, Any)*): SQL[A] = createNameBindingSQL[A](sql)(paramsByName: _*)(extractor)(output)

  /**
   * Maps values from each [[scalikejdbc.WrappedResultSet]] object.
   * @param extractor extractor function
   * @tparam A return type
   * @return SQL instance
   */
  def map[A](extractor: (WrappedResultSet => A)): SQL[A] = createSQL[A](sql)(params: _*)(extractor)(output)

  /**
   * Same as #single.
   * @return SQL instance
   */
  def toOption(): SQLToOption[A] = createSQL(sql)(params: _*)(extractor)(Output.single).asInstanceOf[SQLToOption[A]]

  /**
   * Set execution type as single.
   * @return SQL instance
   */
  def single(): SQLToOption[A] = toOption()

  /**
   * Same as #first.
   * @return SQL instance
   */
  def headOption(): SQLToOption[A] = createSQL(sql)(params: _*)(extractor)(Output.first).asInstanceOf[SQLToOption[A]]

  /**
   * Set execution type as first.
   * @return SQL instance
   */
  def first(): SQLToOption[A] = headOption()

  /**
   * Same as #list
   * @return SQL instance
   */
  def toList(): SQLToList[A] = createSQL(sql)(params: _*)(extractor)(Output.list).asInstanceOf[SQLToList[A]]

  /**
   * Set execution type as list.
   * @return SQL instance
   */
  def list(): SQLToList[A] = toList()

  /**
   * Same as #traversable.
   * @return SQL instance
   */
  def toTraversable(): SQLToTraversable[A] = {
    createSQL[A](sql)(params: _*)(extractor)(Output.traversable).asInstanceOf[SQLToTraversable[A]]
  }

  /**
   * Set execution type as traversable.
   * @return SQL instance
   */
  def traversable(): SQLToTraversable[A] = toTraversable()

  /**
   * Set execution type as execute
   * @return SQL instance
   */
  def execute(): SQLExecution = new SQLExecution(sql)(params: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})

  /**
   * Set execution type as execute with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit) = new SQLExecution(sql)(params: _*)(before)(after)

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
  def executeUpdateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = updateWithFilters(before, after)

  /**
   * Set execution type as executeUpdate
   * @return SQL instance
   */
  def update(): SQLUpdate = new SQLUpdate(sql)(params: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})

  /**
   * Set execution type as executeUpdate with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def updateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = new SQLUpdate(sql)(params: _*)(before)(after)

  /**
   * Set execution type as updateAndreturnGeneratedKey
   * @return SQL instance
   */
  def updateAndReturnGeneratedKey(): SQLUpdateWithGeneratedKey = new SQLUpdateWithGeneratedKey(sql)(params: _*)

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
 * SQL which execute [[java.sql.Statement#exeuteUpdate()]].
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
 * SQL which execute [[java.sql.Statement#exeuteUpdate()]] and get generated key value.
 * @param sql SQL template
 * @param params parameters
 */
class SQLUpdateWithGeneratedKey(sql: String)(params: Any*) {

  def apply()(implicit session: DBSession): Long = session match {
    case AutoSession => DB autoCommit (s => s.updateAndReturnGeneratedKey(sql, params: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.updateAndReturnGeneratedKey(sql, params: _*))
    case _ => session.updateAndReturnGeneratedKey(sql, params: _*)
  }

}

/**
 * SQL which exeute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.collection.Traversable]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToTraversable[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): Traversable[A] = session match {
    case AutoSession => DB readOnly (s => s.traversable(sql, params: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.traversable(sql, params: _*)(extractor))
    case _ => session.traversable(sql, params: _*)(extractor)
  }

}

/**
 * SQL which exeute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.collection.immutable.List]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToList[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): List[A] = session match {
    case AutoSession => DB readOnly (s => s.list(sql, params: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.list(sql, params: _*)(extractor))
    case _ => session.list(sql, params: _*)(extractor)
  }

}

/**
 * SQL which exeute [[java.sql.Statement#executeQuery()]]
 * and returns the result as [[scala.Option]] value.
 * @param sql SQL template
 * @param params parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToOption[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.single)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): Option[A] = output match {
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
