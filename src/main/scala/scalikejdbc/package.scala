/*
 * Copyright 2011 Kazuhiro Sera
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
package object scalikejdbc {

  import scalikejdbc.DB

  def autoCommit[A](execution: DBSession => A)(implicit db: DB): A = DB.autoCommit(execution)

  def withinTx[A](execution: DBSession => A)(implicit db: DB): A = DB.withinTx(execution)

  def localTx[A](execution: DBSession => A)(implicit db: DB): A = DB.localTx(execution)

}