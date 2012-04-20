/*
 * Copyright 2012 Kazuhiro Sera
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
package scalikejdbc

import scalikejdbc.SQL.Output

object SQL {

  object Output extends Enumeration {
    val single, first, list, traversable = Value
  }

  def apply[A](sql: String): SQL[A] = createSQL(sql)(Seq(): _*)()()

}

private[scalikejdbc] object createSQL {

  def apply[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A = (rs: WrappedResultSet) => throw new IllegalStateException("The extractor isn't specified yet."))(output: Output.Value = Output.traversable): SQL[A] = output match {
    case Output.single | Output.first => new SQLToOption(sql)(params: _*)(extractor)(output)
    case Output.list => new SQLToList(sql)(params: _*)(extractor)(output)
    case Output.traversable => new SQLToTraversable(sql)(params: _*)(extractor)(output)
  }

}

abstract class SQL[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable) {

  def bind(params: Any*): SQL[A] = createSQL[A](sql)(params: _*)(extractor)(output)

  def map[A](extractor: (WrappedResultSet => A)): SQL[A] = createSQL[A](sql)(params: _*)(extractor)(output)

  def toOption(): SQLToOption[A] = createSQL(sql)(params: _*)(extractor)(Output.single).asInstanceOf[SQLToOption[A]]

  def single(): SQLToOption[A] = toOption()

  def headOption(): SQLToOption[A] = createSQL(sql)(params: _*)(extractor)(Output.first).asInstanceOf[SQLToOption[A]]

  def first(): SQLToOption[A] = headOption()

  def toList(): SQLToList[A] = createSQL(sql)(params: _*)(extractor)(Output.list).asInstanceOf[SQLToList[A]]

  def list(): SQLToList[A] = toList()

  def toTraversable(): SQLToTraversable[A] = {
    createSQL[A](sql)(params: _*)(extractor)(Output.traversable).asInstanceOf[SQLToTraversable[A]]
  }

  def traversable(): SQLToTraversable[A] = toTraversable()

  def execute(): SQLExecution = new SQLExecution(sql)(params: _*)

  def executeUpdate(): SQLUpdateExecution = new SQLUpdateExecution(sql)(params: _*)

}

class SQLExecution(sql: String)(params: Any*) {

  def apply()(implicit session: DBSession): Boolean = session.execute(sql, params: _*)

}

class SQLUpdateExecution(sql: String)(params: Any*) {

  def apply()(implicit session: DBSession): Int = session.update(sql, params: _*)

}

class SQLToTraversable[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): Traversable[A] = session.traversable(sql, params: _*)(extractor)

}
class SQLToList[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.traversable)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): List[A] = session.list(sql, params: _*)(extractor)

}

class SQLToOption[A](sql: String)(params: Any*)(extractor: WrappedResultSet => A)(output: Output.Value = Output.single)
    extends SQL[A](sql)(params: _*)(extractor)(output) {

  def apply()(implicit session: DBSession): Option[A] = output match {
    case Output.single => session.single(sql, params: _*)(extractor)
    case Output.first => session.first(sql, params: _*)(extractor)
  }

}
