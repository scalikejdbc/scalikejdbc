/*
 * Copyright 2013 Kazuhiro Sera
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
 *   override def db = NamedDB('db2).toDB
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
