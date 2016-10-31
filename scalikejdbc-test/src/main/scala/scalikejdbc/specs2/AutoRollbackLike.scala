package scalikejdbc.specs2

import scalikejdbc._
import org.specs2.specification.After

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
  val _db = db()
  _db.begin()
  _db.withinTx { implicit session =>
    fixture(session)
  }

  // ------------------------------
  // after execution
  // ------------------------------
  override def after: Any = using(_db) { _db =>
    _db.rollbackIfActive()
  }

  /*
   * Passes implicit DBSession instance to the block
   */
  implicit val session: DBSession = _db.withinTxSession()

}
