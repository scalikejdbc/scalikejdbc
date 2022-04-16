package scalikejdbc.scalatest

import org.scalatest.{ FixtureTestSuite, Outcome }
import scalikejdbc._

/**
 * AutoRollback for ScalaTest
 *
 * {{{
 * import org.scalatest.flatspec.FixtureAnyFlatSpec
 * class MemberSpec extends FixtureAnyFlatSpec with AutoRollback {
 *   describe of "Member"
 *   it should "create a new record" in { implicit session =>
 *     Member.create(1, "Alice")
 *     Member.find(1).isDefined should be(true)
 *   }
 * }
 * class LegacyAccountSpec extends FlatSpec with AutoRollback {
 *   override def db() = NamedDB("db2").toDB
 *   override def fixture(implicit session: DBSession) {
 *     SQL("insert into legacy_accounts values ...").update.apply()
 *   }
 *
 *   it should "create a new record" in { implicit session =>
 *     LegacyAccount.create(2, "Bob")
 *     LegacyAccount.find(2).isDefined should be(true)
 *   }
 * }
 * }}}
 */
trait AutoRollback extends LoanPattern { self: FixtureTestSuite =>

  type FixtureParam = DBSession

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

  /**
   * Provides transactional block
   * @param test one arg test
   */
  override def withFixture(test: OneArgTest): Outcome = {
    using(db()) { db =>
      try {
        db.begin()
        db.withinTx { implicit session =>
          fixture(session)
        }
        withFixture(test.toNoArgTest(db.withinTxSession()))
      } finally {
        db.rollbackIfActive()
      }
    }
  }

}
