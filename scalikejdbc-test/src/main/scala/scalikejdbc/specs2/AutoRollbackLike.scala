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
package scalikejdbc.specs2

import scalikejdbc._
import org.specs2.specification.After

/**
 * AutoRollback support for specs2
 */
trait AutoRollbackLike extends After {

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
