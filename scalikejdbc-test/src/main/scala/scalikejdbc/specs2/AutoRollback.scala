package scalikejdbc.specs2

/**
 * Automatic Rollback support for specs2.
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
 * Use [[scalikejdbc.specs2.AutoRollback]] like this:
 *
 * {{{
 * import org.specs2.Specification
 * import scalikejdbc.specs2.AutoRollback
 *
 * class MemberSpec extends Specification { def is =
 *
 *   "Member should create a new record" ! autoRollback().create ^
 *   "LegacyAccount should create a new record" ! db2AutoRollback().create ^ end
 *
 *   case class autoRollback() extends AutoRollback {
 *     def create = this {
 *       Member.create(1, "Alice")
 *       Member.find(1).isDefined must beTrue
 *     }
 *   }
 *
 *   case class db2AutoRollback() extends AutoRollback {
 *     override def db() = NamedDB("db2").toDB
 *     def create = this {
 *       LegacyAccount.create(2, "Bob")
 *       LegacyAccount.find(2).isDefined must beTrue
 *     }
 *   }
 * }
 * }}}
 */
trait AutoRollback extends AutoRollbackLike
