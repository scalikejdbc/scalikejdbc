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
 *   ConnectionPool.singleton("jdbc:...","user","password")
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
   * @param parameters   parameters
   * @param f extractor function
   * @param output output type
   * @tparam A return type
   * @return SQL instance
   */
  def apply[A, E <: WithExtractor](sql: String)(parameters: Any*)(f: WrappedResultSet => A)(output: Output.Value = Output.traversable): SQL[A, E] = output match {
    case Output.single | Output.first => new SQLToOptionImpl[A, E](sql)(parameters: _*)(f)(output)
    case Output.list => new SQLToListImpl[A, E](sql)(parameters: _*)(f)(output)
    case Output.traversable => new SQLToTraversableImpl[A, E](sql)(parameters: _*)(f)(output)
  }

}

/**
 * Name binding [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object createNameBindingSQL extends LogSupport {

  def validateAndConvertToNormalStatement(sql: String, parameters: Seq[(Symbol, Any)]): (String, Seq[Any]) = {
    val names = SQLTemplateParser.extractAllParameters(sql)

    // check all the parameters passed by #bindByName are actually used
    import scalikejdbc.globalsettings._
    GlobalSettings.nameBindingSQLValidator.ignoredParams match {
      case NoCheckForIgnoredParams => // no op
      case validation =>
        parameters.foreach {
          param =>
            if (!names.contains(param._1)) {
              validation match {
                case NoCheckForIgnoredParams => // no op
                case InfoLoggingForIgnoredParams => log.info(ErrorMessage.BINDING_IS_IGNORED + " (" + param._1 + ")")
                case WarnLoggingForIgnoredParams => log.warn(ErrorMessage.BINDING_IS_IGNORED + " (" + param._1 + ")")
                case ExceptionForIgnoredParams => throw new IllegalStateException(ErrorMessage.BINDING_IS_IGNORED + " (" + param._1 + ")")
              }
            }
        }
    }

    val sqlWithPlaceHolders = SQLTemplateParser.convertToSQLWithPlaceHolders(sql)
    (sqlWithPlaceHolders, names.map {
      name =>
        parameters.find(_._1 == name).orElse {
          throw new IllegalArgumentException(ErrorMessage.BINDING_PARAMETER_IS_MISSING + " (" + name + ")")
        }.map(_._2).orNull[Any]
    })
  }

  /**
   * Provides a [[scalikejdbc.SQL]] instance.
   * @param sql SQL template
   * @param parameters named parameters
   * @param f extractor function
   * @param output output type
   * @tparam A return type
   * @return SQL instance
   */
  def apply[A, E <: WithExtractor](sql: String)(parameters: (Symbol, Any)*)(f: WrappedResultSet => A)(output: Output.Value = Output.traversable): SQL[A, E] = output match {
    case Output.single | Output.first => {
      val (_sql, _parameters) = validateAndConvertToNormalStatement(sql, parameters)
      new SQLToOptionImpl[A, E](_sql)(_parameters: _*)(f)(output)
    }
    case Output.list => {
      val (_sql, _parameters) = validateAndConvertToNormalStatement(sql, parameters)
      new SQLToListImpl[A, E](_sql)(_parameters: _*)(f)(output)
    }
    case Output.traversable => {
      val (_sql, _parameters) = validateAndConvertToNormalStatement(sql, parameters)
      new SQLToTraversableImpl[A, E](_sql)(_parameters: _*)(f)(output)
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
  @annotation.implicitNotFound(msg = "No extractor is specified. You have forgotten call #map(...) before #apply().")
  sealed abstract class =:=[From, To] extends (From => To) with Serializable
  private[this] final val singleton_=:= = new =:=[WithExtractor, WithExtractor] { def apply(x: WithExtractor): WithExtractor = x }
  object =:= {
    implicit def tpEquals[A]: A =:= A = singleton_=:=.asInstanceOf[A =:= A]
  }

}

/**
 * Extractor
 */
private[scalikejdbc] trait Extractor[A] {

  def extractor: (WrappedResultSet) => A

}

/**
 * SQL abstraction.
 *
 * @param statement SQL template
 * @param parameters parameters
 * @param f  extractor function
 * @param output output type
 * @tparam A return type
 */
abstract class SQL[A, E <: WithExtractor](val statement: String)(val parameters: Any*)(f: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends Extractor[A] {

  override def extractor: (WrappedResultSet) => A = f

  private[this] var _fetchSize: Option[Int] = None

  type ThisSQL = SQL[A, E]
  type SQLWithExtractor = SQL[A, HasExtractor]

  /**
   * Set fetchSize for this query.
   * @param fetchSize fetch size
   * @return this
   */
  def fetchSize(fetchSize: Int): SQL[A, E] = {
    this._fetchSize = Some(fetchSize)
    this
  }

  def fetchSize(fetchSize: Option[Int]): SQL[A, E] = {
    this._fetchSize = fetchSize
    this
  }

  /**
   * Returns fetchSize for this query.
   * @return fetch size
   */
  def fetchSize: Option[Int] = this._fetchSize

  /**
   * Returns One-to-X API builder.
   */
  def one[Z](f: (WrappedResultSet) => A): OneToXSQL[A, E, Z] = new OneToXSQL[A, E, Z](statement)(parameters: _*)(output)(f)

  /**
   * Binds parameters to SQL template in order.
   * @param parameters parameters
   * @return SQL instance
   */
  def bind(parameters: Any*): SQL[A, E] = {
    createSQL[A, E](statement)(parameters: _*)(f)(output)
      .fetchSize(fetchSize)
  }

  /**
   * Binds named parameters to SQL template.
   * @param parametersByName named parameters
   * @return SQL instance
   */
  def bindByName(parametersByName: (Symbol, Any)*): SQL[A, E] = {
    createNameBindingSQL(statement)(parametersByName: _*)(f)(output)
      .fetchSize(fetchSize)
  }

  /**
   * Binds parameters for batch
   * @param parameters parameters
   * @return SQL for batch
   */
  def batch(parameters: Seq[Any]*): SQLBatch = {
    new SQLBatch(statement)(parameters: _*)
  }

  /**
   * Binds parameters for batch
   * @param parameters parameters
   * @return SQL for batch
   */
  def batchByName(parameters: Seq[(Symbol, Any)]*): SQLBatch = {
    val _sql = createNameBindingSQL.validateAndConvertToNormalStatement(statement, parameters.head)._1
    val _parameters: Seq[Seq[Any]] = parameters.map { p =>
      createNameBindingSQL.validateAndConvertToNormalStatement(statement, p)._2
    }
    new SQLBatch(_sql)(_parameters: _*)
  }

  /**
   * Aplly the operation to all elements of result set
   * @param op operation
   */
  def foreach(op: WrappedResultSet => Unit)(implicit session: DBSession): Unit = session match {
    case AutoSession => DB autoCommit (s => s.foreach(statement, parameters: _*)(op))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.foreach(statement, parameters: _*)(op))
    case ReadOnlyAutoSession => DB readOnly (s => s.foreach(statement, parameters: _*)(op))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.foreach(statement, parameters: _*)(op))
    case _ => session.foreach(statement, parameters: _*)(op)
  }

  /**
   * folding into one value
   * @param z initial value
   * @param op operation
   */
  def foldLeft[A](z: A)(op: (A, WrappedResultSet) => A)(implicit session: DBSession): A = session match {
    case AutoSession => DB autoCommit (_.foldLeft(statement, parameters: _*)(z)(op))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (_.foldLeft(statement, parameters: _*)(z)(op))
    case ReadOnlyAutoSession => DB readOnly (_.foldLeft(statement, parameters: _*)(z)(op))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (_.foldLeft(statement, parameters: _*)(z)(op))
    case _ => session.foldLeft(statement, parameters: _*)(z)(op)
  }

  /**
   * Maps values from each [[scalikejdbc.WrappedResultSet]] object.
   * @param f extractor function
   * @tparam A return type
   * @return SQL instance
   */
  def map[A](f: (WrappedResultSet => A)): SQL[A, HasExtractor] = {
    createSQL[A, HasExtractor](statement)(parameters: _*)(f)(output).fetchSize(fetchSize)
  }

  /**
   * Maps values as a Map value from each [[scalikejdbc.WrappedResultSet]] object.
   * @return SQL instance
   */
  def toMap(): SQL[Map[String, Any], HasExtractor] = map(_.toMap)

  /**
   * Same as #single.
   * @return SQL instance
   */
  def toOption(): SQLToOption[A, E]

  /**
   * Set execution type as single.
   * @return SQL instance
   */
  def single(): SQLToOption[A, E]

  /**
   * Same as #first.
   * @return SQL instance
   */
  def headOption(): SQLToOption[A, E]

  /**
   * Set execution type as first.
   * @return SQL instance
   */
  def first(): SQLToOption[A, E]

  /**
   * Same as #list
   * @return SQL instance
   */
  def toList(): SQLToList[A, E]

  /**
   * Set execution type as list.
   * @return SQL instance
   */
  def list(): SQLToList[A, E]

  /**
   * Same as #traversable.
   * @return SQL instance
   */
  def toTraversable(): SQLToTraversable[A, E]

  /**
   * Set execution type as traversable.
   * @return SQL instance
   */
  def traversable(): SQLToTraversable[A, E]

  /**
   * Set execution type as execute
   * @return SQL instance
   */
  def execute(): SQLExecution = {
    new SQLExecution(statement)(parameters: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as execute with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit) = {
    new SQLExecution(statement)(parameters: _*)(before)(after)
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
    new SQLUpdate(statement)(parameters: _*)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as executeUpdate with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def updateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    new SQLUpdate(statement)(parameters: _*)(before)(after)
  }

  /**
   * Set execution type as updateAndreturnGeneratedKey
   * @return SQL instance
   */
  def updateAndReturnGeneratedKey(): SQLUpdateWithGeneratedKey = {
    updateAndReturnGeneratedKey(1)
  }

  def updateAndReturnGeneratedKey(name: String): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement)(parameters: _*)(name)
  }

  def updateAndReturnGeneratedKey(index: Int): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement)(parameters: _*)(index)
  }

}

/**
 * SQL which execute java.sql.Statement#executeBatch().
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLBatch(val statement: String)(val parameters: Seq[Any]*) {

  def apply()(implicit session: DBSession): Seq[Int] = session match {
    case AutoSession => DB autoCommit (s => s.batch(statement, parameters: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.batch(statement, parameters: _*))
    case ReadOnlyAutoSession => DB readOnly (s => s.batch(statement, parameters: _*))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.batch(statement, parameters: _*))
    case _ => session.batch(statement, parameters: _*)
  }

}

/**
 * SQL which execute java.sql.Statement#execute().
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLExecution(val statement: String)(val parameters: Any*)(before: (PreparedStatement) => Unit)(after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Boolean = session match {
    case AutoSession => DB autoCommit (s => s.executeWithFilters(before, after, statement, parameters: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.executeWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyAutoSession => DB readOnly (s => s.executeWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.executeWithFilters(before, after, statement, parameters: _*))
    case _ => session.executeWithFilters(before, after, statement, parameters: _*)
  }

}

/**
 * SQL which execute java.sql.Statement#executeUpdate().
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLUpdate(val statement: String)(val parameters: Any*)(before: (PreparedStatement) => Unit)(after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Int = session match {
    case AutoSession => DB autoCommit (s => s.updateWithFilters(before, after, statement, parameters: _*))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (s => s.updateWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyAutoSession => DB readOnly (s => s.updateWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.updateWithFilters(before, after, statement, parameters: _*))
    case _ => session.updateWithFilters(before, after, statement, parameters: _*)
  }

}

/**
 * SQL which execute java.sql.Statement#executeUpdate() and get generated key value.
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLUpdateWithGeneratedKey(val statement: String)(val parameters: Any*)(key: Any) {

  def apply()(implicit session: DBSession): Long = session match {
    case AutoSession => DB autoCommit (_.updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key))
    case NamedAutoSession(name) => NamedDB(name) autoCommit (_.updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key))
    case ReadOnlyAutoSession => DB readOnly (_.updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (_.updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key))
    case _ => session.updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key)
  }

}

/**
 * SQL to Traversable
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToTraversable[A, E <: WithExtractor] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  val statement: String
  val parameters: Seq[Any]
  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): Traversable[A] = session match {
    case AutoSession | ReadOnlyAutoSession => DB readOnly (s => s.fetchSize(fetchSize).traversable(statement, parameters: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).traversable(statement, parameters: _*)(extractor))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).traversable(statement, parameters: _*)(extractor))
    case _ => session.fetchSize(fetchSize).traversable(statement, parameters: _*)(extractor)
  }
}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.Traversable value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToTraversableImpl[A, E <: WithExtractor](override val statement: String)(override val parameters: Any*)(override val extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
  extends SQL[A, E](statement)(parameters: _*)(extractor)(output)
  with OutputDecisions[A, E]
  with SQLToTraversable[A, E]

/**
 * SQL to List
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToList[A, E <: WithExtractor] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  val statement: String
  val parameters: Seq[Any]
  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor): List[A] = session match {
    case AutoSession | ReadOnlyAutoSession => DB readOnly (s => s.fetchSize(fetchSize).list(statement, parameters: _*)(extractor))
    case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).list(statement, parameters: _*)(extractor))
    case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).list(statement, parameters: _*)(extractor))
    case _ => session.fetchSize(fetchSize).list(statement, parameters: _*)(extractor)
  }
}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.immutable.List value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToListImpl[A, E <: WithExtractor](override val statement: String)(override val parameters: Any*)(override val extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
  extends SQL[A, E](statement)(parameters: _*)(extractor)(output)
  with OutputDecisions[A, E]
  with SQLToList[A, E]

/**
 * SQL to Option
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToOption[A, E <: WithExtractor] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  val statement: String
  val parameters: Seq[Any]
  val output: Output.Value
  def apply()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): Option[A] = output match {
    case Output.single =>
      session match {
        case AutoSession | ReadOnlyAutoSession => DB readOnly (s => s.fetchSize(fetchSize).single(statement, parameters: _*)(extractor))
        case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).single(statement, parameters: _*)(extractor))
        case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).single(statement, parameters: _*)(extractor))
        case _ => session.fetchSize(fetchSize).single(statement, parameters: _*)(extractor)
      }
    case Output.first =>
      session match {
        case AutoSession | ReadOnlyAutoSession => DB readOnly (s => s.fetchSize(fetchSize).first(statement, parameters: _*)(extractor))
        case NamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).first(statement, parameters: _*)(extractor))
        case ReadOnlyNamedAutoSession(name) => NamedDB(name) readOnly (s => s.fetchSize(fetchSize).first(statement, parameters: _*)(extractor))
        case _ => session.fetchSize(fetchSize).first(statement, parameters: _*)(extractor)
      }
  }
}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.Option value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @param output output type
 * @tparam A return type
 */
class SQLToOptionImpl[A, E <: WithExtractor](override val statement: String)(override val parameters: Any*)(override val extractor: WrappedResultSet => A)(override val output: Output.Value = Output.single)
  extends SQL[A, E](statement)(parameters: _*)(extractor)(output)
  with OutputDecisions[A, E]
  with SQLToOption[A, E]

/**
 * Provides converters for default implementation.
 *
 * @tparam A return type
 * @tparam E extractor constraint
 */
private[scalikejdbc] trait OutputDecisions[A, E <: WithExtractor] extends SQL[A, E] {

  override def toOption(): SQLToOptionImpl[A, E] = {
    createSQL(statement)(parameters: _*)(extractor)(Output.single)
      .fetchSize(fetchSize)
      .asInstanceOf[SQLToOptionImpl[A, E]]
  }
  override def single(): SQLToOptionImpl[A, E] = toOption()

  override def headOption(): SQLToOptionImpl[A, E] = {
    createSQL(statement)(parameters: _*)(extractor)(Output.first)
      .fetchSize(fetchSize)
      .asInstanceOf[SQLToOptionImpl[A, E]]
  }
  override def first(): SQLToOptionImpl[A, E] = headOption()

  override def toList(): SQLToListImpl[A, E] = {
    createSQL(statement)(parameters: _*)(extractor)(Output.list)
      .fetchSize(fetchSize)
      .asInstanceOf[SQLToListImpl[A, E]]
  }

  override def list(): SQLToListImpl[A, E] = toList()

  override def toTraversable(): SQLToTraversableImpl[A, E] = {
    createSQL[A, E](statement)(parameters: _*)(extractor)(Output.traversable)
      .fetchSize(fetchSize)
      .asInstanceOf[SQLToTraversableImpl[A, E]]
  }

  override def traversable(): SQLToTraversableImpl[A, E] = toTraversable()

}
