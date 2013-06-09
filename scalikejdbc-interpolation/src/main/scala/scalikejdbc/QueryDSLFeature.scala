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
package scalikejdbc

/**
 * Query DSL
 */
trait QueryDSLFeature extends SQLInterpolationFeature with SQLSyntaxSupportFeature {

  /**
   * Represents UpdateOperation (used as SQLBuilder[UpdateOperation]).
   */
  trait UpdateOperation

  /**
   * Prefix object for name confiliction.
   *
   * {{{
   *   withSQL { QueryDSL.select.from(User as u).where.eq(u.id, 123) }
   * }}}
   */
  object QueryDSL {

    /**
     * Query Interface for select query.
     * {{{
     *   implicit val session = AutoSession
     *   val u = User.syntax("u")
     *   val user = withSQL { select.from(User).where.eq.(u.id, 123) }.map(User(u.resultName)).single.apply()
     *   val userIdAndName = withSQL {
     *     select(u.result.id, u.result.name).from(User).where.eq.(u.id, 123)
     *   }.map(User(u.resultName)).single.apply()
     * }}}}
     */
    object select {
      def from[A](table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
        new SelectSQLBuilder[A](
          sql = sqls"from ${table}",
          lazyColumns = true,
          resultAllProviders = table.resultAllProvider.map(p => List(p)).getOrElse(Nil)
        )
      }
      def all[A]: SelectSQLBuilder[A] = new SelectSQLBuilder[A](sql = sqls"", lazyColumns = true)
      def all[A](providers: ResultAllProvider*): SelectSQLBuilder[A] = {
        val columns = sqls.join(providers.map(p => sqls"${p.resultAll}"), sqls",")
        new SelectSQLBuilder[A](sqls"select ${columns}")
      }
      def apply[A](columns: SQLSyntax*): SelectSQLBuilder[A] = new SelectSQLBuilder[A](sqls"select ${sqls.csv(columns: _*)}")
    }

    object selectFrom {
      def apply[A](table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = select.from(table)
    }

    /**
     * Query Interface for insert query.
     * {{{
     *   implicit val session = AutoSession
     *   val (u, c) = (User.syntax("u"), User.column)
     *   withSQL { insert.into(User).columns(c.id, c.name, c.createdAt).values(1, "Alice", DateTime.now) }.update.apply()
     *   applyUpdate { insert.into(User).values(2, "Bob", DateTime.now) }
     * }}}}
     */
    object insert {
      def into(support: SQLSyntaxSupport[_]): InsertSQLBuilder = new InsertSQLBuilder(sqls"insert into ${support.table}")
    }

    object insertInto {
      def apply(support: SQLSyntaxSupport[_]): InsertSQLBuilder = insert.into(support)
    }

    /**
     * Query Interface for delete query.
     * {{{
     *   implicit val session = AutoSession
     *   val (u, c) = (User.syntax("u"), User.column)
     *   withSQL { delete.from(User as u).where.eq(u.id, 1) }.update.apply()
     *   applyUpdate { delete.from(User).where.eq(c.id, 1) }
     * }}}}
     */
    object delete {
      def from(table: TableAsAliasSQLSyntax): DeleteSQLBuilder = new DeleteSQLBuilder(sqls"delete from ${table}")
      def from(support: SQLSyntaxSupport[_]): DeleteSQLBuilder = new DeleteSQLBuilder(sqls"delete from ${support.table}")
    }

    object deleteFrom {
      def apply(table: TableAsAliasSQLSyntax): DeleteSQLBuilder = delete.from(table)
      def apply(support: SQLSyntaxSupport[_]): DeleteSQLBuilder = delete.from(support)
    }

    /**
     * Query Interface for update query.
     * {{{
     *   implicit val session = AutoSession
     *   val u = User.syntax("u")
     *   withSQL { update(User as u).set(u.name -> "Chris", u.updatedAt -> DateTime.now).where.eq(u.id, 1) }.update.apply()
     *   applyUpdate { update(User as u).set(u.name -> "Dennis").where.eq(u.id, 1) }
     * }}}}
     */
    object update {
      def apply(table: TableAsAliasSQLSyntax): UpdateSQLBuilder = new UpdateSQLBuilder(sqls"update ${table}")
      def apply(support: SQLSyntaxSupport[_]): UpdateSQLBuilder = new UpdateSQLBuilder(sqls"update ${support.table}")
    }

  }

  val select = QueryDSL.select
  val selectFrom = QueryDSL.selectFrom
  val insert = QueryDSL.insert
  val insertInto = QueryDSL.insertInto
  val update = QueryDSL.update
  val delete = QueryDSL.delete
  val deleteFrom = QueryDSL.deleteFrom

  /**
   * withSQL clause which returns SQL[A, NoExtractor] from SQLBuilder.
   */
  object withSQL {
    def apply[A](builder: SQLBuilder[A]): SQL[A, NoExtractor] = builder.toSQL
  }

  /**
   * withSQL and update.apply()
   */
  object applyUpdate {
    def apply(builder: SQLBuilder[UpdateOperation])(implicit session: DBSession): Int = withSQL[UpdateOperation](builder).update.apply()
  }

  /**
   * withSQL and updateAndReturnGeneratedKey.apply()
   */
  object applyUpdateAndReturnGeneratedKey {
    def apply(builder: SQLBuilder[UpdateOperation])(implicit session: DBSession): Long = withSQL[UpdateOperation](builder).updateAndReturnGeneratedKey.apply()
  }

  /**
   * withSQL and execute.apply()
   */
  object applyExecute {
    def apply(builder: SQLBuilder[UpdateOperation])(implicit session: DBSession): Boolean = withSQL[UpdateOperation](builder).execute.apply()
  }

  // -----
  // Query Interface SQLBuilder 

  /**
   * SQLBuilder
   */
  trait SQLBuilder[A] {
    def sql: SQLSyntax
    def append(part: SQLSyntax): SQLBuilder[A]

    def toSQLSyntax: SQLSyntax = sqls"${sql}"
    def toSQL: SQL[A, NoExtractor] = sql"${sql}"
  }

  // Featureless constructor for SQLBuilder
  private[scalikejdbc] class RawSQLBuilder[A](val sql: SQLSyntax) extends SQLBuilder[A] {
    override def append(part: SQLSyntax): SQLBuilder[A] = throw new IllegalStateException("This must be a library bug.")
  }

  trait WhereSQLBuilder[A] extends SQLBuilder[A] {
    def where: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.where}")
    def where(where: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.where(where)}")
  }

  // factory
  private[scalikejdbc] object GroupBySQLBuilder {
    def apply[A](sql: SQLSyntax) = new RawSQLBuilder[A](sql) with GroupBySQLBuilder[A]
  }

  trait GroupBySQLBuilder[A] extends SQLBuilder[A]
      with PagingSQLBuilder[A] {
    def groupBy(columns: SQLSyntax*): GroupBySQLBuilder[A] = GroupBySQLBuilder[A](sqls"${sql} ${sqls.groupBy(columns: _*)}")
    def having(condition: SQLSyntax): GroupBySQLBuilder[A] = GroupBySQLBuilder[A](sqls"${sql} ${sqls.having(condition)}")

    override def append(part: SQLSyntax): GroupBySQLBuilder[A] = GroupBySQLBuilder[A](sqls"${sql} ${part}")
  }

  // factory
  private[scalikejdbc] object PagingSQLBuilder {
    def apply[A](sql: SQLSyntax) = new RawSQLBuilder[A](sql) with PagingSQLBuilder[A]
  }

  trait PagingSQLBuilder[A] extends SQLBuilder[A]
      with UnionQuerySQLBuilder[A]
      with SubQuerySQLBuilder[A] {
    def orderBy(columns: SQLSyntax*): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} ${sqls.orderBy(columns: _*)}")
    def asc: PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} asc")
    def desc: PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} desc")
    def limit(n: Int): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} ${sqls.limit(n)}")
    def offset(n: Int): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} ${sqls.offset(n)}")

    override def append(part: SQLSyntax): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} ${part}")
  }

  // factory
  private[scalikejdbc] object ConditionSQLBuilder {
    def apply[A](sql: SQLSyntax) = new RawSQLBuilder[A](sql) with ConditionSQLBuilder[A]
  }

  trait ConditionSQLBuilder[A] extends SQLBuilder[A]
      with PagingSQLBuilder[A]
      with GroupBySQLBuilder[A]
      with UnionQuerySQLBuilder[A]
      with SubQuerySQLBuilder[A] {

    def and: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} and")
    def or: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} or")
    def not: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} not")

    def eq(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.eq(column, value)}")
    def ne(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.ne(column, value)}")
    def gt(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.gt(column, value)}")
    def ge(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.ge(column, value)}")
    def lt(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.lt(column, value)}")
    def le(column: SQLSyntax, value: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.le(column, value)}")

    def isNull(column: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.isNull(column)}")
    def isNotNull(column: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.isNotNull(column)}")

    @deprecated("use between(column: SQLSyntax, a: Any, b: Any) instead", "1.6.2")
    def between(a: Any, b: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.between(a, b)}")
    def between(column: SQLSyntax, a: Any, b: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.between(column, a, b)}")

    def in(column: SQLSyntax, values: Seq[Any]): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(column, values)}")
    def in(column: SQLSyntax, subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${column} in (${subQuery.toSQLSyntax})")

    def notIn(column: SQLSyntax, values: Seq[Any]): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(column, values)}")
    def notIn(column: SQLSyntax, subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${column} not in (${subQuery.toSQLSyntax})")

    def like(column: SQLSyntax, value: String): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${column} like ${value}")
    def notLike(column: SQLSyntax, value: String): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${column} not like ${value}")

    def exists(subQuery: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} exists (${subQuery})")
    def exists(subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] = exists(subQuery.toSQLSyntax)

    def notExists(subQuery: SQLSyntax): ConditionSQLBuilder[A] = not.exists(subQuery)
    def notExists(subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] = not.exists(subQuery)

    /**
     * Appends a round bracket in where clause.
     * e.g. select.from(User as u).where.withRoundBracket { _.eq(u.id, 123).and.eq(u.groupId, 234) }.or.eq(u.groupId, 345)
     */
    def withRoundBracket[A](insidePart: ConditionSQLBuilder[_] => ConditionSQLBuilder[_]): ConditionSQLBuilder[A] = {
      val emptyBuilder = ConditionSQLBuilder[A](sqls"")
      ConditionSQLBuilder[A](sqls"${sql} (${insidePart(emptyBuilder).toSQLSyntax})")
    }

    /**
     * Appends conditions with delimiter.
     *
     * {{{
     * .where
     * .dynamicAndConditions(
     *   id.map(i => sqls.eq(u.id, i)),
     *   Some(sqls.isNotNull(u.name))
     * )
     * }}}
     */
    def dynamicAndConditions(conditions: Option[SQLSyntax]*) = {
      val cs = conditions.flatten.map(c => sqls"(${c})")
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.joinWithAnd(cs: _*)}")
    }
    def dynamicOrConditions(conditions: Option[SQLSyntax]*) = {
      val cs = conditions.flatten.map(c => sqls"(${c})")
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.joinWithOr(cs: _*)}")
    }

    /**
     * Appends SQLSyntax directly.
     * e.g. select.from(User as u).where.eq(u.id, 123).append(sqls"order by ${u.id} desc")
     */
    override def append(part: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${part}")

    /**
     * Maps SQLBuilder as follows.
     * e.g. select.from(User as u).where.eq(u.id, 123).map { sql => if(name.isDefined) sql.and.eq(u.name, name) else sql }
     */
    def map(mapper: ConditionSQLBuilder[A] => ConditionSQLBuilder[A]): ConditionSQLBuilder[A] = mapper.apply(this)

  }

  trait SubQuerySQLBuilder[A] extends SQLBuilder[A] {

    /**
     * Converts SQLBuilder to sub-query part sqls.
     * e.g.
     *   val x = SubQuery.syntax("x").include(u, g)
     *   withSQL { select.from(select.from(User as u).leftJoin(Group as g).on(u.groupId, g.id).where.eq(u.groupId, 234).as(x)) }
     */
    def as(sq: SubQuerySQLSyntaxProvider): TableAsAliasSQLSyntax = {
      val syntax = sqls"(${this.toSQLSyntax}) ${SubQuery.as(sq)}"
      TableAsAliasSQLSyntax(syntax.value, syntax.parameters)
    }
  }

  trait UnionQuerySQLBuilder[A] extends SQLBuilder[A] {
    def union(anotherQuery: SQLSyntax): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} union ${anotherQuery}")
    def unionAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} union all ${anotherQuery}")
    def union(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = union(anotherQuery.toSQLSyntax)
    def unionAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = unionAll(anotherQuery.toSQLSyntax)
  }

  /**
   * SQLBuilder for select queries.
   */
  case class SelectSQLBuilder[A](override val sql: SQLSyntax, lazyColumns: Boolean = false, resultAllProviders: List[ResultAllProvider] = Nil)
      extends SQLBuilder[A]
      with PagingSQLBuilder[A]
      with GroupBySQLBuilder[A]
      with UnionQuerySQLBuilder[A]
      with WhereSQLBuilder[A]
      with SubQuerySQLBuilder[A] {

    private def appendResultAllProvider(table: TableAsAliasSQLSyntax, providers: List[ResultAllProvider]) = {
      table.resultAllProvider.map(provider => provider :: resultAllProviders).getOrElse(resultAllProviders)
    }

    // e.g. select.from(User as u)
    def from(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
      this.copy(
        sql = sqls"${sql} from ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders)
      )
    }

    // ---
    // join query

    def join(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = innerJoin(table)
    def innerJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
      this.copy(
        sql = sqls"${sql} inner join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders)
      )
    }

    def leftJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
      this.copy(
        sql = sqls"${sql} left join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders)
      )
    }

    def rightJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
      this.copy(
        sql = sqls"${sql} right join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders)
      )
    }

    def on(onClause: SQLSyntax): SelectSQLBuilder[A] = this.copy(sql = sqls"${sql} on ${onClause}")
    def on(left: SQLSyntax, right: SQLSyntax): SelectSQLBuilder[A] = this.copy(sql = sqls"${sql} on ${left} = ${right}")

    override def where: ConditionSQLBuilder[A] = {
      if (lazyColumns) {
        val columns = sqls.join(resultAllProviders.reverse.map(_.resultAll), sqls",")
        ConditionSQLBuilder[A](sqls"select ${columns} ${sql} ${sqls.where}")
      } else {
        ConditionSQLBuilder[A](sqls"${sql} ${sqls.where}")
      }
    }
    override def where(where: SQLSyntax): ConditionSQLBuilder[A] = {
      if (lazyColumns) {
        val columns = sqls.join(resultAllProviders.reverse.map(_.resultAll), sqls",")
        ConditionSQLBuilder[A](sqls"select ${columns} ${sql} ${sqls.where(where)}")
      } else {
        ConditionSQLBuilder[A](sqls"${sql} ${sqls.where(where)}")
      }
    }

    override def append(part: SQLSyntax): SelectSQLBuilder[A] = this.copy(sql = sqls"${sql} ${part}")

    def map(mapper: SelectSQLBuilder[A] => SelectSQLBuilder[A]): SelectSQLBuilder[A] = mapper.apply(this)

    override def toSQLSyntax: SQLSyntax = {
      if (lazyColumns) sqls"select ${sqls.join(resultAllProviders.reverse.map(_.resultAll), sqls",")} ${sql}"
      else sqls"${sql}"
    }
    override def toSQL: SQL[A, NoExtractor] = {
      if (lazyColumns) sql"select ${sqls.join(resultAllProviders.reverse.map(_.resultAll), sqls",")} ${sql}"
      else sql"${sql}"
    }

  }

  /**
   * SQLBuilder for insert queries.
   */
  case class InsertSQLBuilder(override val sql: SQLSyntax) extends SQLBuilder[UpdateOperation] {

    def columns(columns: SQLSyntax*): InsertSQLBuilder = this.copy(sql = sqls"${sql} (${sqls.csv(columns: _*)})")
    def values(values: Any*): InsertSQLBuilder = {
      val vs = sqls.csv(values.map(v => sqls"${v}"): _*)
      this.copy(sql = sqls"${sql} values (${vs})")
    }
    def namedValues(columnsAndValues: (SQLSyntax, Any)*): InsertSQLBuilder = {
      val (cs, vs) = (columnsAndValues.map(_._1), columnsAndValues.map(_._2))
      columns(cs: _*).values(vs: _*)
    }

    def select(columns: SQLSyntax*)(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = QueryDSL.select[Nothing](columns: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def selectAll(providers: ResultAllProvider*)(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = QueryDSL.select.all[Nothing](providers: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def select(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = new SelectSQLBuilder[Nothing](sql = sqls"", lazyColumns = true)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }

    override def append(part: SQLSyntax): InsertSQLBuilder = this.copy(sql = sqls"${sql} ${part}")
  }

  /**
   * SQLBuilder for update queries.
   */
  case class UpdateSQLBuilder(override val sql: SQLSyntax) extends SQLBuilder[UpdateOperation]
      with WhereSQLBuilder[UpdateOperation] {

    def set(sqlPart: SQLSyntax): UpdateSQLBuilder = this.copy(sql = sqls"${sql} set ${sqlPart}")
    def set(tuples: (SQLSyntax, Any)*): UpdateSQLBuilder = set(sqls.csv(tuples.map(each => sqls"${each._1} = ${each._2}"): _*))

    override def append(part: SQLSyntax): UpdateSQLBuilder = this.copy(sql = sqls"${sql} ${part}")
  }

  /**
   * SQLBuilder for delete queries.
   */
  case class DeleteSQLBuilder(override val sql: SQLSyntax) extends SQLBuilder[UpdateOperation]
      with WhereSQLBuilder[UpdateOperation] {

    override def append(part: SQLSyntax): DeleteSQLBuilder = this.copy(sql = sqls"${sql} ${part}")
  }

}

