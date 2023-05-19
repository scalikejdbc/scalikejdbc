package scalikejdbc.specs2

import scalikejdbc._
import org.specs2.specification.After

import scala.util.control.NonFatal

/**
 * AutoRollback support for specs2
 */
trait AutoRollbackLike extends After with LoanPattern {

  protected[this] def settingsProvider: SettingsProvider =
    SettingsProvider.default

  /**
   * Creates a [[scalikejdbc.DB]] instance.
   * @return DB instance
   */
  def db(): DB =
    DB(conn = ConnectionPool.borrow(), settingsProvider = settingsProvider)

  /**
   * Prepares database for the test.
   * @param session db session implicitly
   */
  def fixture(implicit session: DBSession): Unit = {}

  // ------------------------------
  // before execution
  // ------------------------------
  val _db: DB = db()
  _db.begin()

  try {
    _db.withinTx { implicit session =>
      fixture(session)
    }
  } catch {
    case NonFatal(e) =>
      using(_db)(_.rollbackIfActive())
      throw e
  }

  // ------------------------------
  // after execution
  // ------------------------------
  override def after: Any = using(_db) { _.rollbackIfActive() }

  /*
   * Passes implicit DBSession instance to the block
   */
  implicit val session: DBSession = _db.withinTxSession()

}
