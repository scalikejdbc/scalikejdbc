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
package scalikejdbc.scalatest

import org.scalatest.fixture.Suite
import scalikejdbc._

/**
 * AutoRollback for ScalaTest
 *
 * {{{
 * import org.scalatest.fixture.FlatSpec
 * class MemberSpec extends FlatSpec with AutoRollback {
 *   describe of "Member"
 *   it should "create a new record" in { implicit session =>
 *     Member.create(1, "Alice")
 *     Member.find(1).isDefined should be(true)
 *   }
 * }
 * class LegacyAccountSpec extends FlatSpec with AutoRollback {
 *   override def db = NamedDB('db2).toDB
 *   override def fixture(implicit session: DBSession) {
 *     SQL("insert into legacy_accounts values ...").update.apply()
 *   }
 *
 *   it should "create a new record" in { implicit session =?
 *     LegacyAccount.create(2, "Bob")
 *     LegacyAccount.find(2).isDefined should be(true)
 *   }
 * }
 * }}}
 */
trait AutoRollback { self: Suite =>

  type FixtureParam = DBSession

  /**
   * Creates a [[scalikejdbc.DB]] instance.
   * @return DB instance
   */
  def db(): DB = DB(ConnectionPool.borrow())

  /**
   * Prepares database for the test.
   * @param session db session implicitly
   */
  def fixture(implicit session: DBSession): Unit = {}

  /**
   * Provides transactional block
   * @param test one arg test
   */
  override def withFixture(test: OneArgTest) = {
    using(db()) { db =>
      try {
        db.begin()
        db.withinTx { implicit session =>
          fixture(session)
        }
        test(db.withinTxSession())
      } finally {
        db.rollbackIfActive()
      }
    }
  }

}
