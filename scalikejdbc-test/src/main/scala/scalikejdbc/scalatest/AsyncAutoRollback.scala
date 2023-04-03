package scalikejdbc.scalatest

import org.scalatest.{ FixtureAsyncTestSuite, FutureOutcome }
import scalikejdbc._

/**
 * AsyncAutoRollback for ScalaTest
 *
 * {{{
 * import org.scalatest.fixture.AsyncFlatSpec
 * class MemberSpec extends AsyncFlatSpec with AsyncAutoRollback {
 *   describe of "Member"
 *   it should "create a new record" in { implicit session =>
 *    Future {
 *      Member.create(1, "Alice")
 *      Member.find(1).isDefined should be(true)
 *    }
 *   }
 * }
 * class LegacyAccountSpec extends AsyncFlatSpec with AsyncAutoRollback {
 *   override def db() = NamedDB("db2").toDB
 *   override def fixture(implicit session: DBSession) {
 *     SQL("insert into legacy_accounts values ...").update.apply()
 *   }
 *
 *   it should "create a new record" in { implicit session =>
 *     Future {
 *       LegacyAccount.create(2, "Bob")
 *       LegacyAccount.find(2).isDefined should be(true)
 *     }
 *   }
 * }
 * }}}
 */
trait AsyncAutoRollback extends LoanPattern { self: FixtureAsyncTestSuite =>

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
  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    val database = db()
    database.begin()
    database.withinTx { implicit session =>
      fixture(session)
    }
    withFixture(test.toNoArgAsyncTest(database.withinTxSession()))
      .onCompletedThen { _ =>
        using(database) { d =>
          d.rollbackIfActive()
        }
      }
  }

}
