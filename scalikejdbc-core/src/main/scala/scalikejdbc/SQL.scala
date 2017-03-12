package scalikejdbc

import java.sql.PreparedStatement
import scala.language.higherKinds
import scala.collection.generic.CanBuildFrom

/**
 * SQL abstraction's companion object
 *
 * {{{
 *   ConnectionPool.singleton("jdbc:...","user","password")
 *   case class User(id: Int, name: String)
 *
 *   val users = DB.readOnly { implicit session =>
 *     SQL("select * from user").map { rs =>
 *       User(rs.int("id"), rs.string("name"))
 *     }.list.apply()
 *   }
 *
 *   DB .autoCommit { implicit session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   DB localTx { implicit session =>
 *     SQL("insert into user values (?,?)").bind(123, "Alice").update.apply()
 *   }
 *
 *   using(DB(ConnectionPool.borrow())) { db =>
 *     db.begin()
 *     try {
 *       DB withTx { implicit session =>
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
    ErrorMessage.THIS_IS_A_BUG
  ))

}

/**
 * Name binding [[scalikejdbc.SQL]] instance factory.
 */
private[scalikejdbc] object validateAndConvertToNormalStatement extends LogSupport {

  def apply(sql: String, settings: SettingsProvider, parameters: Seq[(Symbol, Any)]): (String, Seq[Any]) = {
    val names = SQLTemplateParser.extractAllParameters(sql)

    // check all the parameters passed by #bindByName are actually used
    import scalikejdbc.globalsettings._
    settings.nameBindingSQLValidator(GlobalSettings.nameBindingSQLValidator).ignoredParams match {
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
    (sqlWithPlaceHolders, names.map { name =>
      parameters match {
        case Nil => Nil
        case _ =>
          parameters.find(_._1 == name).orElse {
            throw new IllegalArgumentException(ErrorMessage.BINDING_PARAMETER_IS_MISSING + " (" + name + ")")
          }.map(_._2).orNull[Any]
      }
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
 * @param rawParameters parameters
 * @param f  extractor function
 * @tparam A return type
 */
abstract class SQL[A, E <: WithExtractor](
  val statement: String,
  private[scalikejdbc] val rawParameters: Seq[Any]
)(f: WrappedResultSet => A)
    extends Extractor[A] {

  final lazy val parameters: Seq[Any] = rawParameters.map {
    case ParameterBinder(v) => v
    case x => x
  }

  override def extractor: (WrappedResultSet) => A = f

  private[this] var _fetchSize: Option[Int] = None
  private[this] val _tags: scala.collection.mutable.ListBuffer[String] = new scala.collection.mutable.ListBuffer[String]()
  private[this] var _queryTimeout: Option[Int] = None
  private[this] var _settings: SettingsProvider = SettingsProvider.default

  type ThisSQL = SQL[A, E]
  type SQLWithExtractor = SQL[A, HasExtractor]

  protected def withParameters(params: Seq[Any]): SQL[A, E] = ???

  protected def withStatementAndParameters(state: String, params: Seq[Any]): SQL[A, E] = ???

  protected def withExtractor[B](f: WrappedResultSet => B): SQL[B, HasExtractor] = ???

  def dbSettingSettings(settings: SettingsProvider): this.type = {
    this._settings = settings
    this
  }

  /**
   * Set fetchSize for this query.
   *
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
   *
   * @param tags tags
   * @return this
   */
  def tags(tags: String*): this.type = {
    this._tags ++= tags
    this
  }

  /**
   * Returns tags for this SQL object.
   *
   * @return tags
   */
  def tags: Seq[String] = this._tags.toSeq

  /**
   * Returns fetchSize for this query.
   *
   * @return fetch size
   */
  def fetchSize: Option[Int] = this._fetchSize

  /**
   * Set queryTimeout for this query.
   *
   * @param seconds query timeout seconds
   * @return this
   */
  def queryTimeout(seconds: Int): this.type = {
    this._queryTimeout = Some(seconds)
    this
  }

  def queryTimeout(seconds: Option[Int]): this.type = {
    this._queryTimeout = seconds
    this
  }

  /**
   * Returns queryTimeout for this query.
   *
   * @return query timeout seconds
   */
  def queryTimeout: Option[Int] = this._queryTimeout

  /**
   * Creates a new DBSessionAttributesSwitcher which enables switching the attributes for a DBSession.
   */
  protected def createDBSessionAttributesSwitcher(): DBSessionAttributesSwitcher = {
    new DBSessionAttributesSwitcher(this)
  }

  /**
   * Returns One-to-X API builder.
   */
  def one[Z](f: (WrappedResultSet) => A): OneToXSQL[A, E, Z] = {
    val q: OneToXSQL[A, E, Z] = new OneToXSQL[A, E, Z](statement, rawParameters)(f)
    q.queryTimeout(queryTimeout)
    q.fetchSize(fetchSize)
    q.tags(tags: _*)
    q
  }

  /**
   * Binds parameters to SQL template in order.
   *
   * @param parameters parameters
   * @return SQL instance
   */
  def bind(parameters: Any*): SQL[A, E] = {
    withParameters(parameters).fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Binds named parameters to SQL template.
   *
   * @param parametersByName named parameters
   * @return SQL instance
   */
  def bindByName(parametersByName: (Symbol, Any)*): SQL[A, E] = {
    val (_statement, _parameters) = validateAndConvertToNormalStatement(statement, _settings, parametersByName)
    withStatementAndParameters(_statement, _parameters).fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Binds parameters for batch
   *
   * @param parameters parameters
   * @return SQL for batch
   */
  def batch(parameters: Seq[Any]*): SQLBatch = {
    new SQLBatch(statement, parameters, tags)
  }

  /**
   * Binds parameters for batch
   *
   * @param parameters parameters
   * @return SQL for batch
   */
  def batchAndReturnGeneratedKey(parameters: Seq[Any]*): SQLBatchWithGeneratedKey = {
    new SQLBatchWithGeneratedKey(statement, parameters, tags)(None)
  }

  /**
   * Binds parameters for batch
   *
   * @param generatedKeyName generated key name
   * @param parameters parameters
   * @return SQL for batch
   */
  def batchAndReturnGeneratedKey(generatedKeyName: String, parameters: Seq[Any]*): SQLBatchWithGeneratedKey = {
    new SQLBatchWithGeneratedKey(statement, parameters, tags)(Some(generatedKeyName))
  }

  /**
   * Binds parameters for batch
   *
   * @param parameters parameters
   * @return SQL for batch
   */
  def batchByName(parameters: Seq[(Symbol, Any)]*): SQLBatch = {
    val _sql = validateAndConvertToNormalStatement(statement, _settings, parameters.headOption.getOrElse(Seq.empty))._1
    val _parameters: Seq[Seq[Any]] = parameters.map { p =>
      validateAndConvertToNormalStatement(statement, _settings, p)._2
    }
    new SQLBatch(_sql, _parameters, tags)
  }

  /**
   * Apply the operation to all elements of result set
   *
   * @param op operation
   */
  def foreach(op: WrappedResultSet => Unit)(implicit session: DBSession): Unit = {
    val attributesSwitcher = createDBSessionAttributesSwitcher()
    val f: DBSession => Unit =
      DBSessionWrapper(_, attributesSwitcher).foreach(statement, rawParameters: _*)(op)
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

  /**
   * folding into one value
   *
   * @param z initial value
   * @param op operation
   */
  def foldLeft[A](z: A)(op: (A, WrappedResultSet) => A)(implicit session: DBSession): A = {
    val attributesSwitcher = createDBSessionAttributesSwitcher()
    val f: DBSession => A =
      DBSessionWrapper(_, attributesSwitcher).foldLeft(statement, rawParameters: _*)(z)(op)
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

  /**
   * Maps values from each [[scalikejdbc.WrappedResultSet]] object.
   *
   * @param f extractor function
   * @tparam A return type
   * @return SQL instance
   */
  def map[A](f: WrappedResultSet => A): SQL[A, HasExtractor] = {
    withExtractor[A](f).fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Maps values as a Map value from each [[scalikejdbc.WrappedResultSet]] object.
   *
   * @return SQL instance
   */
  def toMap(): SQL[Map[String, Any], HasExtractor] = map(_.toMap)

  /**
   * Same as #single.
   *
   * @return SQL instance
   */
  def toOption(): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](statement, rawParameters)(extractor)(isSingle = true)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Set execution type as single.
   *
   * @return SQL instance
   */
  def single(): SQLToOption[A, E] = toOption()

  /**
   * Same as #first.
   *
   * @return SQL instance
   */
  def headOption(): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](statement, rawParameters)(extractor)(isSingle = false)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Set execution type as first.
   *
   * @return SQL instance
   */
  def first(): SQLToOption[A, E] = headOption()

  /**
   * Same as #list
   *
   * @return SQL instance
   */
  def toList(): SQLToList[A, E] = {
    new SQLToListImpl[A, E](statement, rawParameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Set execution type as list.
   *
   * @return SQL instance
   */
  def list(): SQLToList[A, E] = toList()

  /**
   * Same as #collection
   *
   * @return SQL instance
   */
  def toCollection: SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](statement, rawParameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Set execution type as collection.
   *
   * @return SQL instance
   */
  def collection: SQLToCollection[A, E] = toCollection

  /**
   * Same as #traversable.
   *
   * @return SQL instance
   */
  def toTraversable(): SQLToTraversable[A, E] = {
    new SQLToTraversableImpl[A, E](statement, rawParameters)(extractor)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)
  }

  /**
   * Set execution type as traversable.
   *
   * @return SQL instance
   */
  def traversable(): SQLToTraversable[A, E] = toTraversable()

  /**
   * Set execution type as execute
   *
   * @return SQL instance
   */
  def execute(): SQLExecution = {
    new SQLExecution(statement, rawParameters, tags)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as execute with filters
   *
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit) = {
    new SQLExecution(statement, rawParameters, tags)(before)(after)
  }

  /**
   * Set execution type as executeUpdate
   *
   * @return SQL instance
   */
  def executeUpdate(): SQLUpdate = update()

  /**
   * Set execution type as executeUpdate with filters
   *
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def executeUpdateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    updateWithFilters(before, after)
  }

  /**
   * Set execution type as executeUpdate
   *
   * @return SQL instance
   */
  def update(): SQLUpdate = {
    new SQLUpdate(statement, rawParameters, tags)((stmt: PreparedStatement) => {})((stmt: PreparedStatement) => {})
  }

  /**
   * Set execution type as executeUpdate with filters
   *
   * @param before before filter
   * @param after after filter
   * @return SQL instance
   */
  def updateWithFilters(before: (PreparedStatement) => Unit, after: (PreparedStatement) => Unit): SQLUpdate = {
    new SQLUpdate(statement, rawParameters, tags)(before)(after)
  }

  /**
   * Set execution type as updateAndReturnGeneratedKey
   *
   * @return SQL instance
   */
  def updateAndReturnGeneratedKey(): SQLUpdateWithGeneratedKey = {
    updateAndReturnGeneratedKey(1)
  }

  def updateAndReturnGeneratedKey(name: String): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement, rawParameters, this.tags)(name)
  }

  def updateAndReturnGeneratedKey(index: Int): SQLUpdateWithGeneratedKey = {
    new SQLUpdateWithGeneratedKey(statement, rawParameters, this.tags)(index)
  }

  def stripMargin(marginChar: Char): SQL[A, E] =
    withStatementAndParameters(statement.stripMargin(marginChar), rawParameters)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)

  def stripMargin: SQL[A, E] =
    withStatementAndParameters(statement.stripMargin, rawParameters)
      .fetchSize(fetchSize).tags(tags: _*).queryTimeout(queryTimeout)

}

/**
 * SQL which execute java.sql.Statement#executeBatch().
 *
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLBatch(val statement: String, val parameters: Seq[Seq[Any]], val tags: Seq[String] = Nil) {

  def apply[C[_]]()(implicit session: DBSession, cbf: CanBuildFrom[Nothing, Int, C[Int]]): C[Int] = {
    val attributesSwitcher = new DBSessionAttributesSwitcher(SQL("").tags(tags: _*))
    val f: DBSession => C[Int] = DBSessionWrapper(_, attributesSwitcher).batch(statement, parameters: _*)
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

object SQLBatch {
  def unapply(sqlObject: SQLBatch): Option[(String, Seq[Seq[Any]], Seq[String])] = {
    Some((sqlObject.statement, sqlObject.parameters, sqlObject.tags))
  }
}

class SQLBatchWithGeneratedKey(val statement: String, val parameters: Seq[Seq[Any]], val tags: Seq[String] = Nil)(val key: Option[String]) {

  def apply[C[_]]()(implicit session: DBSession, cbf: CanBuildFrom[Nothing, Long, C[Long]]): C[Long] = {
    val attributesSwitcher = new DBSessionAttributesSwitcher(SQL("").tags(tags: _*))
    val f: DBSession => C[Long] = (session) => {
      key match {
        case Some(k) => DBSessionWrapper(session, attributesSwitcher).batchAndReturnSpecifiedGeneratedKey(statement, k, parameters: _*)
        case _ => DBSessionWrapper(session, attributesSwitcher).batchAndReturnGeneratedKey(statement, parameters: _*)
      }
    }
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

object SQLBatchWithGeneratedKey {
  def unapply(sqlObject: SQLBatchWithGeneratedKey): Option[(String, Seq[Seq[Any]], Seq[String], Option[String])] = {
    Some((sqlObject.statement, sqlObject.parameters, sqlObject.tags, sqlObject.key))
  }
}

/**
 * SQL which execute java.sql.Statement#execute().
 *
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLExecution(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(
    val before: (PreparedStatement) => Unit
)(
    val after: (PreparedStatement) => Unit
) {

  def apply()(implicit session: DBSession): Boolean = {
    val attributesSwitcher = new DBSessionAttributesSwitcher(SQL("").tags(tags: _*))
    val f: DBSession => Boolean = DBSessionWrapper(_, attributesSwitcher).executeWithFilters(before, after, statement, parameters: _*)
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

object SQLExecution {
  def unapply(sqlObject: SQLExecution): Option[(String, Seq[Any], Seq[String], PreparedStatement => Unit, PreparedStatement => Unit)] = {
    Some((sqlObject.statement, sqlObject.parameters, sqlObject.tags, sqlObject.before, sqlObject.after))
  }
}

/**
 * SQL which execute java.sql.Statement#executeUpdate().
 *
 * @param statement SQL template
 * @param parameters parameters
 * @param before before filter
 * @param after after filter
 */
class SQLUpdate(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(
    val before: (PreparedStatement) => Unit
)(
    val after: (PreparedStatement) => Unit
) {

  def apply()(implicit session: DBSession): Int = {
    val attributesSwitcher = new DBSessionAttributesSwitcher(SQL("").tags(tags: _*))
    session match {
      case AutoSession =>
        DB.autoCommit(DBSessionWrapper(_, attributesSwitcher).updateWithFilters(before, after, statement, parameters: _*))
      case NamedAutoSession(name, _) =>
        NamedDB(name, session.settings).autoCommit(DBSessionWrapper(_, attributesSwitcher).updateWithFilters(before, after, statement, parameters: _*))
      case ReadOnlyAutoSession =>
        DB.readOnly(DBSessionWrapper(_, attributesSwitcher).updateWithFilters(before, after, statement, parameters: _*))
      case ReadOnlyNamedAutoSession(name, _) =>
        NamedDB(name, session.settings).readOnly(DBSessionWrapper(_, attributesSwitcher).updateWithFilters(before, after, statement, parameters: _*))
      case _ =>
        DBSessionWrapper(session, attributesSwitcher).updateWithFilters(before, after, statement, parameters: _*)
    }
  }

}

object SQLUpdate {
  def unapply(sqlObject: SQLUpdate): Option[(String, Seq[Any], Seq[String], PreparedStatement => Unit, PreparedStatement => Unit)] = {
    Some((sqlObject.statement, sqlObject.parameters, sqlObject.tags, sqlObject.before, sqlObject.after))
  }
}

/**
 * SQL which execute java.sql.Statement#executeUpdate() and get generated key value.
 *
 * @param statement SQL template
 * @param parameters parameters
 */
class SQLUpdateWithGeneratedKey(val statement: String, val parameters: Seq[Any], val tags: Seq[String] = Nil)(val key: Any) {

  def apply()(implicit session: DBSession): Long = {
    val attributesSwitcher = new DBSessionAttributesSwitcher(SQL("").tags(tags: _*))
    val f: DBSession => Long = DBSessionWrapper(_, attributesSwitcher).updateAndReturnSpecifiedGeneratedKey(statement, parameters: _*)(key)
    // format: OFF
    session match {
      case AutoSession                       => DB.autoCommit(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).autoCommit(f)
      case ReadOnlyAutoSession               => DB.readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

object SQLUpdateWithGeneratedKey {
  def unapply(sqlObject: SQLUpdateWithGeneratedKey): Option[(String, Seq[Any], Seq[String], Any)] = {
    Some((sqlObject.statement, sqlObject.parameters, sqlObject.tags, sqlObject.key))
  }
}

trait SQLToResult[A, E <: WithExtractor, C[_]] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._

  def result[AA](f: WrappedResultSet => AA, session: DBSession): C[AA]
  val statement: String
  private[scalikejdbc] val rawParameters: Seq[Any]
  def apply()(
    implicit
    session: DBSession,
    context: ConnectionPoolContext = NoConnectionPoolContext,
    hasExtractor: ThisSQL =:= SQLWithExtractor
  ): C[A] = {
    val attributesSwitcher = createDBSessionAttributesSwitcher()
    val f: DBSession => C[A] = s => result[A](extractor, DBSessionWrapper(s, attributesSwitcher))
    // format: OFF
    session match {
      case AutoSession | ReadOnlyAutoSession => DB.readOnly(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.Traversable value.
 *
 * @tparam A return type
 */
trait SQLToTraversable[A, E <: WithExtractor] extends SQLToResult[A, E, Traversable] {

  def result[AA](f: WrappedResultSet => AA, session: DBSession): Traversable[AA] = {
    session.traversable[AA](statement, rawParameters: _*)(f)
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.Traversable value.
 *
 * @param statement SQL template
 * @param rawParameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToTraversableImpl[A, E <: WithExtractor](
  override val statement: String, override val rawParameters: Seq[Any]
)(
  override val extractor: WrappedResultSet => A
)
    extends SQL[A, E](statement, rawParameters)(extractor)
    with SQLToTraversable[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToResult[A, E, Traversable] = {
    new SQLToTraversableImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToResult[A, E, Traversable] = {
    new SQLToTraversableImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToResult[B, HasExtractor, Traversable] = {
    new SQLToTraversableImpl[B, HasExtractor](statement, rawParameters)(f)
  }

}

object SQLToTraversableImpl {
  def unapply[A, E <: WithExtractor](sqlObject: SQLToTraversableImpl[A, E]): Option[(String, Seq[Any], WrappedResultSet => A)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.extractor))
  }
}

/**
 * SQL to Collection
 *
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToCollection[A, E <: WithExtractor] extends SQL[A, E] with Extractor[A] {
  import GeneralizedTypeConstraintsForWithExtractor._
  val statement: String
  private[scalikejdbc] val rawParameters: Seq[Any]
  def apply[C[_]]()(implicit session: DBSession, context: ConnectionPoolContext = NoConnectionPoolContext, hasExtractor: ThisSQL =:= SQLWithExtractor, cbf: CanBuildFrom[Nothing, A, C[A]]): C[A] = {
    val attributesSwitcher = createDBSessionAttributesSwitcher()
    val f: DBSession => C[A] = DBSessionWrapper(_, attributesSwitcher).collection[A, C](statement, rawParameters: _*)(extractor)
    // format: OFF
    session match {
      case AutoSession | ReadOnlyAutoSession => DB.readOnly(f)
      case NamedAutoSession(name, _)         => NamedDB(name, session.settings).readOnly(f)
      case ReadOnlyNamedAutoSession(name, _) => NamedDB(name, session.settings).readOnly(f)
      case _                                 => f(session)
    }
    // format: ON
  }

}

class SQLToCollectionImpl[A, E <: WithExtractor](
  override val statement: String, override val rawParameters: Seq[Any]
)(
  override val extractor: WrappedResultSet => A
)
    extends SQL[A, E](statement, rawParameters)(extractor)
    with SQLToCollection[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToCollection[A, E] = {
    new SQLToCollectionImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToCollection[B, HasExtractor] = {
    new SQLToCollectionImpl[B, HasExtractor](statement, rawParameters)(f)
  }

}

object SQLToCollectionImpl {
  def unapply[A, E <: WithExtractor](sqlObject: SQLToCollectionImpl[A, E]): Option[(String, Seq[Any], WrappedResultSet => A)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.extractor))
  }
}

/**
 * SQL to List
 *
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToList[A, E <: WithExtractor] extends SQLToResult[A, E, List] {

  def result[AA](f: WrappedResultSet => AA, session: DBSession): List[AA] = {
    session.list[AA](statement, rawParameters: _*)(f)
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.collection.immutable.List value.
 *
 * @param statement SQL template
 * @param rawParameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToListImpl[A, E <: WithExtractor](
  override val statement: String, override val rawParameters: Seq[Any]
)(
  override val extractor: WrappedResultSet => A
)
    extends SQL[A, E](statement, rawParameters)(extractor)
    with SQLToList[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToList[A, E] = {
    new SQLToListImpl[A, E](statement, params)(extractor)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToList[A, E] = {
    new SQLToListImpl[A, E](state, params)(extractor)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToList[B, HasExtractor] = {
    new SQLToListImpl[B, HasExtractor](statement, rawParameters)(f)
  }

}

object SQLToListImpl {
  def unapply[A, E <: WithExtractor](sqlObject: SQLToListImpl[A, E]): Option[(String, Seq[Any], WrappedResultSet => A)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.extractor))
  }
}

/**
 * SQL to Option
 *
 * @tparam A return type
 * @tparam E extractor settings
 */
trait SQLToOption[A, E <: WithExtractor] extends SQLToResult[A, E, Option] {

  protected def isSingle: Boolean

  def result[AA](f: WrappedResultSet => AA, session: DBSession): Option[AA] = {
    if (isSingle) {
      session.single[AA](statement, rawParameters: _*)(f)
    } else {
      session.first[AA](statement, rawParameters: _*)(f)
    }
  }

}

/**
 * SQL which execute java.sql.Statement#executeQuery() and returns the result as scala.Option value.
 *
 * @param statement SQL template
 * @param rawParameters parameters
 * @param extractor  extractor function
 * @tparam A return type
 */
class SQLToOptionImpl[A, E <: WithExtractor](
  override val statement: String, override val rawParameters: Seq[Any]
)(
  override val extractor: WrappedResultSet => A
)(val isSingle: Boolean = true)
    extends SQL[A, E](statement, rawParameters)(extractor)
    with SQLToOption[A, E] {

  override protected def withParameters(params: Seq[Any]): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](statement, params)(extractor)(isSingle)
  }

  override protected def withStatementAndParameters(state: String, params: Seq[Any]): SQLToOption[A, E] = {
    new SQLToOptionImpl[A, E](state, params)(extractor)(isSingle)
  }

  override protected def withExtractor[B](f: WrappedResultSet => B): SQLToOption[B, HasExtractor] = {
    new SQLToOptionImpl[B, HasExtractor](statement, rawParameters)(f)(isSingle)
  }

}

object SQLToOptionImpl {
  def unapply[A, E <: WithExtractor](sqlObject: SQLToOptionImpl[A, E]): Option[(String, Seq[Any], WrappedResultSet => A, Boolean)] = {
    Some((sqlObject.statement, sqlObject.rawParameters, sqlObject.extractor, sqlObject.isSingle))
  }
}
