/*
 * Copyright 2011 - 2015 scalikejdbc.org
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

import java.sql.PreparedStatement
import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom

/**
 * SQL abstraction's companion object.
 *
 * {{{
 *   ConnectionPool.singleton("jdbc:...","user","password")
 *   case class User(id: Int, name: String)
 *
 *   val users = DB.readOnly { session =>
 *     SQL("select * from user").map { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }.list.apply()
 *   }
 *
 *   DB .autoCommit { session =>
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

  def apply[A](sql: String): SQL[A, NoExtractor] = new SQLToTraversableImpl[A, NoExtractor](sql, Seq.empty)(noExtractor[A](
    "If you see this message, it's a ScalikeJDBC's bug. Please report us."
  ))

}

/**
 * Name binding [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object validateAndConvertToNormalStatement extends LogSupport {

  def apply(sql: String, parameters: Seq[(Symbol, Any)]): (String, Seq[Any]) = {
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
 * @tparam A return type
 */
abstract class SQL[A, E <: WithExtractor](
  val statement: String,
  val parameters: Seq[Any])(f: WrappedResultSet => A)
    extends Extractor[A] {

  override def extractor: (WrappedResultSet) => A = f

  private[this] var _fetchSize: Option[Int] = None
  private[this] val _tags: scala.collection.mutable.ListBuffer[String] = new scala.collection.mutable.ListBuffer[String]()

  type ThisSQL = SQL[A, E]
  type SQLWithExtractor = SQL[A, HasExtractor]

  protected def withParameters(params: Seq[Any]): SQL[A, E] = ???

  protected def withStatementAndParameters(state: String, params: Seq[Any]): SQL[A, E] = ???

  protected def withExtractor[B](f: WrappedResultSet => B): SQL[B, HasExtractor] = ???

  /**
   * Set fetchSize for this query.
   * @param fetchSize fetch size
   * @return this
   */
  def fetchSize(fetchSize: Int): this.type = {
    this._fetchSize = Some(fetchSize)
    this
  }

  def fetchSize(fetchSize: Option[Int]): this.type = {
    this._fetchSize = fetchSize
    this
  }

  /**
   * Appends tags to this SQL object.
   * @param tags tags
   * @return this
   */
  def tags(tags: String*): this.type = {
    this._tags ++= tags
    this
  }

  def tags: Seq[String] = this._tags.toSeq

  /**
   * Returns fetchSize for this query.
   * @return fetch size
   */
  def fetchSize: Option[Int] = this._fetchSize

  /**
   * Returns One-to-X API builder.
   */
  def one[Z](f: (WrappedResultSet) => A): OneToXSQL[A, E, Z] = new OneToXSQL[A, E, Z](statement, parameters)(f)

  /**
   * Binds parameters to SQL template in order.
   * @param parameters parameters
   * @return SQL instance
   */
  def bind(parameters: Any*): SQL[A, E] = {
    withParameters(parameters).fetchSize(fetchSize).tags(tags: _*)
  }

  /**
   * Binds named parameters to SQL template.
   * @param parametersByName named parameters
   * @return SQL instance
   */
  def bindByName(parametersByName: (Symbol, Any)*): SQL[A, E] = {
    val (_statement, _parameters) = validateAndConvertToNormalStatement(statement, parametersByName)
    withStatementAndParameters(_statement, _parameters).fetchSize(fetchSize).tags(tags: _*)
  }

  /**
   * Binds parameters for batch
   * @param parameters parameters
   * @return SQL for batch
   */
  def batch(parameters: Seq[Any]*): SQLBatch = {
    new SQLBatch(statement, parameters, tags)
  }

  /**
   * Binds parameters for batch
   * @param parameters parameters
   * @return SQL for batch
   */
  def batchByName(parameters: Seq[(Symbol, Any)]*): SQLBatch = {
    val _sql = validateAndConvertToNormalStatement(statement, parameters.head)._1
    val _parameters: Seq[Seq[Any]] = parameters.map { p =>
      validateAndConvertToNormalStatement(statement, p)._2
    }
    new SQLBatch(_sql, _parameters, tags)
  }

  /**
   * Apply the operation to all elements of result set
   * @param op operation
   */
  def foreach(op: WrappedResultSet => Unit)(implicit session: DBSession): Unit = {
    val f: DBSession => Unit = _.fetchSize(fetchSize).tags(tags: _*).foreach(statement, parameters: _*)(op)
    // format: OFF
    session match {
      case AutoSession                    => DB.autoCommit(f)
      case NamedAutoSession(name)         => NamedDB(name).autoCommit(f)
      case ReadOnlyAutoSession            => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name) => NamedDB(name).readOnly(f)
      case _                              => f(session)
    }
    // format: ON
  }

  /**
   * folding into one value
   * @param z initial value
   * @param op operation
   */
  def foldLeft[A](z: A)(op: (A, WrappedResultSet) => A)(implicit session: DBSession): A = {
    val f: DBSession => A = _.fetchSize(fetchSize).tags(tags: _*).foldLeft(statement, parameters: _*)(z)(op)
    // format: OFF
    session match {
      case AutoSession                    => DB.autoCommit(f)
      case NamedAutoSession(name)         => NamedDB(name).autoCommit(f)
      case ReadOnlyAutoSession            => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name) => NamedDB(name).readOnly(f)
      case _                              => f(session)
    }
    // format: ON
  }

  /**
   * Maps values from each [[scalikejdbc.WrappedResultSet]] object.
   * @param f extractor function
   * @tparam A return type
   * @return SQL instance
   */
  def map[A](f: WrappedResultSet => A): SQL[A, HasExtractor] = {
    withExtractor[A](f).fetchSize(fetchSize).tags(tags: _*)
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
  def toOption(): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](statement, parameters)(extractor)(isSingle = true)
      .fetchSize(fetchSize).tags(tags: _*)
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
    new SQLToOptionImpl[A, E](statement, parameters)(extractor)(isSingle = false)
      .fetchSize(fetchSize).tags(tags: _*)
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
    new SQLToListImpl[A, E](statement, parameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*)
  }

  /**
   * Set execution type as list.
   * @return SQL instance
   */
  def list(): SQLToList[A, E] = toList()

  /**
   * Same as #collection
   * @return SQL instance
   */
  def toCollection: SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](statement, parameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*)
  }

  /**
   * Set execution type as collection.
   * @return SQL instance
   */
  def collection: SQLToCollection[A, E] = toCollection

  /**
   * Same as #traversable.
   * @return SQL instance
   */
  def toTraversable(): SQLToTraversable[A, E] = {
    new SQLToTraversableImpl[A, E](statement, parameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*)
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
    new SQLExecution(statement, parameters, tags)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as execute with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit) = {
    new SQLExecution(statement, parameters, tags)(before)(after)
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
    new SQLUpdate(statement, parameters, tags)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as executeUpdate with filters
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def updateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    new SQLUpdate(statement, parameters, tags)(before)(after)
  }

  /**
   * Set execution type as updateAndReturnGeneratedKey
   * @return SQL instance
   */
  def updateAndReturnGeneratedKey(): SQLUpdateWithGeneratedKey = {
    updateAndReturnGeneratedKey(1)
  }

  def updateAndReturnGeneratedKey(name: String): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement, parameters, this.tags)(name)
  }

  def updateAndReturnGeneratedKey(index: Int): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement, parameters, this.tags)(index)
  }

}

/**
 * SQL which execute java.sql.Statement#executeBatch().
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLBatch(val statement: String, val parameters: Seq[Seq[Any]], val tags: Seq[String] = Nil) {

  def apply()(implicit session: DBSession): Seq[Int] = {
    val f: DBSession => Seq[Int] = _.tags(tags: _*).batch(statement, parameters: _*)
    // format: OFF
    session match {
      case AutoSession                    => DB.autoCommit(f)
      case NamedAutoSession(name)         => NamedDB(name).autoCommit(f)
      case ReadOnlyAutoSession            => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name) => NamedDB(name).readOnly(f)
      case _                              => f(session)
    }
    // format: ON
  }

}

/**
 * SQL which execute java.sql.Statement#execute().
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLExecution(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(
    before: (PreparedStatement) => Unit)(
        after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Boolean = {
    val f: DBSession => Boolean = _.tags(tags: _*).executeWithFilters(before, after, statement, parameters: _*)
    // format: OFF
    session match {
      case AutoSession                    => DB.autoCommit(f)
      case NamedAutoSession(name)         => NamedDB(name).autoCommit(f)
      case ReadOnlyAutoSession            => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name) => NamedDB(name).readOnly(f)
      case _                              => f(session)
    }
    // format: ON
  }

}

/**
 * SQL which execute java.sql.Statement#executeUpdate().
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLUpdate(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(
    before: (PreparedStatement) => Unit)(
        after: (PreparedStatement) => Unit) {

  def apply()(implicit session: DBSession): Int = session match {
    case AutoSession =>
      DB.autoCommit(_.tags(tags: _*).updateWithFilters(before, after, statement, parameters: _*))
    case NamedAutoSession(name) =>
      NamedDB(name).autoCommit(_.tags(tags: _*).updateWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyAutoSession =>
      DB.readOnly(_.tags(tags: _*).updateWithFilters(before, after, statement, parameters: _*))
    case ReadOnlyNamedAutoSession(name) =>
      NamedDB(name).readOnly(_.tags(tags: _*).updateWithFilters(before, after, statement, parameters: _*))
    case _ =>
      session.tags(tags: _*).updateWithFilters(before, after, statement, parameters: _*)
  }

}

/**
 * SQL which execute java.sql.Statement#executeUpdate() and get generated key value.
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLUpdateWithGeneratedKey(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(key: Any) {

  def apply()(implicit session: DBSession): Long = {
    val f: DBSession => Long = _.tags(tags: _*).updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key)
    // format: OFF
    session match {
      case AutoSession                    => DB.autoCommit(f)
      case NamedAutoSession(name)         => NamedDB(name).autoCommit(f)
      case ReadOnlyAutoSession            => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name) => NamedDB(name).readOnly(f)
      case _                              => f(session)
    }
    // format: ON
  }

}

trait SQLToResult[A, E <: WithExtractor, C[_]] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  def result[AA](f: WrappedResultSet => AA, session: DBSession): C[AA]
  val statement: String
  val parameters: Seq[Any]
  def apply()(
    implicit session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor): C[A] = {
    val f: DBSession => C[A] = s => result[A](extractor, s.fetchSize(fetchSize).tags(tags: _*))
    // format: OFF
    session match {
      case AutoSession | ReadOnlyAutoSession => DB.readOnly(f)
      case NamedAutoSession(name)            => NamedDB(name).readOnly(f)
      case ReadOnlyNamedAutoSession(name)    => NamedDB(name).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.Traversable value.
 * @tparam A return type
 */
trait SQLToTraversable[A, E <: WithExtractor] extends SQLToResult[A, E, Traversable] {

  def result[AA](f: WrappedResultSet => AA, session: DBSession): Traversable[AA] = {
    session.traversable[AA](statement, parameters: _*)(f)
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.Traversable value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToTraversableImpl[A, E <: WithExtractor](
  override val statement: String, override val parameters: Seq[Any])(
    override val extractor: WrappedResultSet => A)
    extends SQL[A, E](statement, parameters)(extractor)
    with SQLToTraversable[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToResult[A, E, Traversable] = {
    new SQLToTraversableImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToResult[A, E, Traversable] = {
    new SQLToTraversableImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToResult[B, HasExtractor, Traversable] = {
    new SQLToTraversableImpl[B, HasExtractor](statement, parameters)(f)
  }

}

/**
 * SQL to Collection
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToCollection[A, E <: WithExtractor] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  val statement: String
  val parameters: Seq[Any]
  def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, A, C[A]]): C[A] = {
    val f: DBSession => C[A] = _.fetchSize(fetchSize).tags(tags: _*).collection[A, C](statement, parameters: _*)(extractor)
    // format: OFF
    session match {
      case AutoSession | ReadOnlyAutoSession => DB.readOnly(f)
      case NamedAutoSession(name)            => NamedDB(name).readOnly(f)
      case ReadOnlyNamedAutoSession(name)    => NamedDB(name).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

class SQLToCollectionImpl[A, E <: WithExtractor](
  override val statement: String, override val parameters: Seq[Any])(
    override val extractor: WrappedResultSet => A)
    extends SQL[A, E](statement, parameters)(extractor)
    with SQLToCollection[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToCollection[B, HasExtractor] = {
    new SQLToCollectionImpl[B, HasExtractor](statement, parameters)(f)
  }

}

/**
 * SQL to List
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToList[A, E <: WithExtractor] extends SQLToResult[A, E, List] {

  def result[AA](f: WrappedResultSet => AA, session: DBSession): List[AA] = {
    session.list[AA](statement, parameters: _*)(f)
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.immutable.List value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToListImpl[A, E <: WithExtractor](
  override val statement: String, override val parameters: Seq[Any])(
    override val extractor: WrappedResultSet => A)
    extends SQL[A, E](statement, parameters)(extractor)
    with SQLToList[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToList[A, E] = {
    new SQLToListImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToList[A, E] = {
    new SQLToListImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToList[B, HasExtractor] = {
    new SQLToListImpl[B, HasExtractor](statement, parameters)(f)
  }

}

/**
 * SQL to Option
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToOption[A, E <: WithExtractor] extends SQLToResult[A, E, Option] {

  protected def isSingle: Boolean

  def result[AA](f: WrappedResultSet => AA, session: DBSession): Option[AA] = {
    if (isSingle) {
      session.single[AA](statement, parameters: _*)(f)
    } else {
      session.first[AA](statement, parameters: _*)(f)
    }
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.Option value.
 * @param statement SQL template
 * @param parameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToOptionImpl[A, E <: WithExtractor](
  override val statement: String, override val parameters: Seq[Any])(
    override val extractor: WrappedResultSet => A)(protected val isSingle: Boolean = true)
    extends SQL[A, E](statement, parameters)(extractor)
    with SQLToOption[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](statement, params)(extractor)(isSingle)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](state, params)(extractor)(isSingle)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToOption[B, HasExtractor] = {
    new SQLToOptionImpl[B, HasExtractor](statement, parameters)(f)(isSingle)
  }

}
