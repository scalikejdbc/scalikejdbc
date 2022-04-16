package scalikejdbc.specs2.mutable

import org.specs2.mutable._
import scalikejdbc.specs2.AutoRollbackLike

/**
 * Automatic Rollback support for mutable specs2.
 *
 * If you want to test this `Member` object,
 *
 * {{{
 * import scalikejdbc._
 *
 * object Member {
 *   def create(id: Long, name: String)(implicit session: DBSession = AutoSession) {
 *     SQL("insert into members values (?, ?)".bind(id, name).update.apply()
 *   }
 * }
 * }}}
 *
 * Use [[scalikejdbc.specs2.mutable.AutoRollback]] like this:
 *
 * {{{
 * import org.specs2.mutable._
 * import scalikejdbc.specs2.mutable.AutoRollback
 *
 * trait DB2AutoRollback extends AutoRollback {
 *   override def db() = NamedDB("db2").toDB
 * }
 *
 * object MemberSpec extends Specification {
 *   "Member should create a new record" in new AutoRollback {
 *     Member.create(1, "Alice")
 *     Member.find(1).isDefined must beTrue
 *   }
 *   "LegacyAccount should create a new record" in new DB2AutoRollback {
 *     LegacyAccount.create(2, "Bob")
 *     LegacyAccount.find(2).isDefined must beTrue
 *   }
 * }
 * }}}
 */
trait AutoRollback extends After with AutoRollbackLike
