package scalikejdbc

/**
 * Query DSL
 */
trait QueryDSLFeature {
  self: SQLInterpolationFeature with SQLSyntaxSupportFeature =>

  /**
   * Represents UpdateOperation (used as SQLBuilder[UpdateOperation]).
   */
  trait UpdateOperation

  /**
   * Prefix object to avoid name conflict.
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
     * }}}
     */
    object select {
      def from[A](table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = {
        new SelectSQLBuilder[A](
          sql = sqls"from ${table}",
          lazyColumns = true,
          resultAllProviders =
            table.resultAllProvider.map(p => List(p)).getOrElse(Nil)
        )
      }
      def all[A]: SelectSQLBuilder[A] =
        new SelectSQLBuilder[A](sql = SQLSyntax.empty, lazyColumns = true)
      def all[A](providers: ResultAllProvider*): SelectSQLBuilder[A] = {
        val columns =
          sqls.join(providers.map(p => sqls"${p.resultAll}"), sqls",")
        new SelectSQLBuilder[A](sqls"select ${columns}")
      }
      def apply[A](columns: SQLSyntax*): SelectSQLBuilder[A] =
        new SelectSQLBuilder[A](sqls"select ${sqls.csv(columns: _*)}")
    }

    object selectFrom {
      def apply[A](table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] =
        select.from(table)
    }

    /**
     * Query Interface for insert query.
     * {{{
     *   implicit val session = AutoSession
     *   val (u, c) = (User.syntax("u"), User.column)
     *   withSQL { insert.into(User).columns(c.id, c.name, c.createdAt).values(1, "Alice", DateTime.now) }.update.apply()
     *   applyUpdate { insert.into(User).values(2, "Bob", DateTime.now) }
     * }}}
     */
    object insert {
      def into(support: SQLSyntaxSupport[_]): InsertSQLBuilder =
        InsertSQLBuilder(sqls"insert into ${support.table}")
    }

    object insertInto {
      def apply(support: SQLSyntaxSupport[_]): InsertSQLBuilder =
        insert.into(support)
    }

    /**
     * Query Interface for delete query.
     * {{{
     *   implicit val session = AutoSession
     *   val (u, c) = (User.syntax("u"), User.column)
     *   withSQL { delete.from(User as u).where.eq(u.id, 1) }.update.apply()
     *   applyUpdate { delete.from(User).where.eq(c.id, 1) }
     * }}}
     */
    object delete {
      def from(table: TableAsAliasSQLSyntax): DeleteSQLBuilder =
        DeleteSQLBuilder(sqls"delete from ${table}")
      def from(support: SQLSyntaxSupport[_]): DeleteSQLBuilder =
        DeleteSQLBuilder(sqls"delete from ${support.table}")
    }

    object deleteFrom {
      def apply(table: TableAsAliasSQLSyntax): DeleteSQLBuilder =
        delete.from(table)
      def apply(support: SQLSyntaxSupport[_]): DeleteSQLBuilder =
        delete.from(support)
    }

    /**
     * Query Interface for update query.
     * {{{
     *   implicit val session = AutoSession
     *   val u = User.syntax("u")
     *   withSQL { update(User as u).set(u.name -> "Chris", u.updatedAt -> DateTime.now).where.eq(u.id, 1) }.update.apply()
     *   applyUpdate { update(User as u).set(u.name -> "Dennis").where.eq(u.id, 1) }
     * }}}
     */
    object update {
      def apply(table: TableAsAliasSQLSyntax): UpdateSQLBuilder =
        UpdateSQLBuilder(sqls"update ${table}")
      def apply(support: SQLSyntaxSupport[_]): UpdateSQLBuilder =
        UpdateSQLBuilder(sqls"update ${support.table}")
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
    def apply(builder: SQLBuilder[UpdateOperation])(implicit
      session: DBSession
    ): Int = withSQL[UpdateOperation](builder).update.apply()
  }

  /**
   * withSQL and updateAndReturnGeneratedKey.apply()
   */
  object applyUpdateAndReturnGeneratedKey {
    def apply(
      builder: SQLBuilder[UpdateOperation]
    )(implicit session: DBSession): Long =
      withSQL[UpdateOperation](builder).updateAndReturnGeneratedKey.apply()
  }

  /**
   * withSQL and execute.apply()
   */
  object applyExecute {
    def apply(builder: SQLBuilder[UpdateOperation])(implicit
      session: DBSession
    ): Boolean = withSQL[UpdateOperation](builder).execute.apply()
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
  private[scalikejdbc] class RawSQLBuilder[A](val sql: SQLSyntax)
    extends SQLBuilder[A] {
    override def append(part: SQLSyntax): SQLBuilder[A] =
      throw new IllegalStateException("This must be a library bug.")
  }

  trait WhereSQLBuilder[A] extends SQLBuilder[A] {
    def where: ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.where}")
    def where(where: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.where(where)}")
  }

  // factory
  private[scalikejdbc] object GroupBySQLBuilder {
    def apply[A](sql: SQLSyntax): RawSQLBuilder[A] with GroupBySQLBuilder[A] =
      new RawSQLBuilder[A](sql) with GroupBySQLBuilder[A]
  }

  trait GroupBySQLBuilder[A] extends SQLBuilder[A] with PagingSQLBuilder[A] {
    def groupBy(columns: SQLSyntax*): GroupBySQLBuilder[A] =
      GroupBySQLBuilder[A](sqls"${sql} ${sqls.groupBy(columns: _*)}")
    def having(condition: SQLSyntax): GroupBySQLBuilder[A] =
      GroupBySQLBuilder[A](sqls"${sql} ${sqls.having(condition)}")

    override def append(part: SQLSyntax): GroupBySQLBuilder[A] =
      GroupBySQLBuilder[A](sqls"${sql} ${part}")
  }

  // factory
  private[scalikejdbc] object PagingSQLBuilder {
    def apply[A](sql: SQLSyntax): RawSQLBuilder[A] with PagingSQLBuilder[A] =
      new RawSQLBuilder[A](sql) with PagingSQLBuilder[A]
  }

  trait PagingSQLBuilder[A]
    extends SQLBuilder[A]
    with UnionQuerySQLBuilder[A]
    with ExceptQuerySQLBuilder[A]
    with IntersectQuerySQLBuilder[A]
    with ForUpdateQuerySQLBuilder[A]
    with SubQuerySQLBuilder[A] {
    def orderBy(columns: SQLSyntax*): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} ${sqls.orderBy(columns: _*)}")
    def asc: PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} asc")
    def desc: PagingSQLBuilder[A] = PagingSQLBuilder[A](sqls"${sql} desc")
    def limit(n: Int): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} ${sqls.limit(n)}")
    def offset(n: Int): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} ${sqls.offset(n)}")

    override def append(part: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} ${part}")
  }

  // factory
  private[scalikejdbc] object ConditionSQLBuilder {
    def apply[A](sql: SQLSyntax): RawSQLBuilder[A] with ConditionSQLBuilder[A] =
      new RawSQLBuilder[A](sql) with ConditionSQLBuilder[A]
  }

  trait ConditionSQLBuilder[A]
    extends SQLBuilder[A]
    with PagingSQLBuilder[A]
    with GroupBySQLBuilder[A] {

    def and: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sql.and)

    // Never append 'and' if sqlPart is empty.
    def and(sqlPart: Option[SQLSyntax]): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sql.and(sqlPart))

    def or: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sql.or)

    // Never append 'or' if sqlPart is empty.
    def or(sqlPart: Option[SQLSyntax]): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sql.or(sqlPart))

    def not: ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} not")

    def eq[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.eq(column, value)}")
    def ne[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.ne(column, value)}")
    def gt[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.gt(column, value)}")
    def ge[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.ge(column, value)}")
    def lt[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.lt(column, value)}")
    def le[B: ParameterBinderFactory](
      column: SQLSyntax,
      value: B
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.le(column, value)}")

    def isNull(column: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.isNull(column)}")
    def isNotNull(column: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.isNotNull(column)}")

    def between[B: ParameterBinderFactory, C: ParameterBinderFactory](
      column: SQLSyntax,
      a: B,
      b: C
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.between(column, a, b)}")
    def notBetween[B: ParameterBinderFactory, C: ParameterBinderFactory](
      column: SQLSyntax,
      a: B,
      b: C
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notBetween(column, a, b)}")

    def in[B: ParameterBinderFactory](
      column: SQLSyntax,
      values: collection.Seq[B]
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(column, values)}")

    def in(column: SQLSyntax, subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](
        sqls"${sql} ${column} in (${subQuery.toSQLSyntax})"
      )

    def in[B: ParameterBinderFactory, C: ParameterBinderFactory](
      columns: (SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(columns, valueSeqs)}")
    }
    def in[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(columns, valueSeqs)}")
    }
    def in[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory,
      E: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D, E)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(columns, valueSeqs)}")
    }
    def in[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory,
      E: ParameterBinderFactory,
      G: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D, E, G)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(columns, valueSeqs)}")
    }

    def notIn[B: ParameterBinderFactory](
      column: SQLSyntax,
      values: collection.Seq[B]
    ): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(column, values)}")

    def notIn(
      column: SQLSyntax,
      subQuery: SQLBuilder[_]
    ): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](
      sqls"${sql} ${column} not in (${subQuery.toSQLSyntax})"
    )

    def notIn[B: ParameterBinderFactory, C: ParameterBinderFactory](
      columns: (SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(columns, valueSeqs)}")
    }
    def notIn[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(columns, valueSeqs)}")
    }
    def notIn[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory,
      E: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D, E)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(columns, valueSeqs)}")
    }
    def notIn[
      B: ParameterBinderFactory,
      C: ParameterBinderFactory,
      D: ParameterBinderFactory,
      E: ParameterBinderFactory,
      G: ParameterBinderFactory
    ](
      columns: (SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax, SQLSyntax),
      valueSeqs: collection.Seq[(B, C, D, E, G)]
    ): ConditionSQLBuilder[A] = {
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notIn(columns, valueSeqs)}")
    }

    def like(column: SQLSyntax, value: String): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.like(column, value)}")
    def notLike(column: SQLSyntax, value: String): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${sqls.notLike(column, value)}")

    def exists(subQuery: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} exists (${subQuery})")
    def exists(subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] = exists(
      subQuery.toSQLSyntax
    )

    def notExists(subQuery: SQLSyntax): ConditionSQLBuilder[A] =
      not.exists(subQuery)
    def notExists(subQuery: SQLBuilder[_]): ConditionSQLBuilder[A] =
      not.exists(subQuery)

    /**
     * Appends a round bracket in where clause.
     * e.g. select.from(User as u).where.withRoundBracket { _.eq(u.id, 123).and.eq(u.groupId, 234) }.or.eq(u.groupId, 345)
     */
    def withRoundBracket[A](
      insidePart: ConditionSQLBuilder[_] => ConditionSQLBuilder[_]
    ): ConditionSQLBuilder[A] = {
      val emptyBuilder = ConditionSQLBuilder[A](SQLSyntax.empty)
      ConditionSQLBuilder[A](
        sqls"${sql} (${insidePart(emptyBuilder).toSQLSyntax})"
      )
    }

    def roundBracket(inner: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sql.roundBracket(inner))

    /**
     * Appends SQLSyntax directly.
     * e.g. select.from(User as u).where.eq(u.id, 123).append(sqls"order by ${u.id} desc")
     */
    override def append(part: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${sql} ${part}")

    /**
     * Maps SQLBuilder as follows.
     * e.g. select.from(User as u).where.eq(u.id, 123).map { sql => if(name.isDefined) sql.and.eq(u.name, name) else sql }
     */
    def map(
      mapper: ConditionSQLBuilder[A] => ConditionSQLBuilder[A]
    ): ConditionSQLBuilder[A] = mapper.apply(this)

  }

  /**
   * Sub query builder
   */
  trait SubQuerySQLBuilder[A] extends SQLBuilder[A] {

    /**
     * Converts SQLBuilder to sub-query part sqls.
     * e.g.
     *   val x = SubQuery.syntax("x").include(u, g)
     *   withSQL { select.from(select.from(User as u).leftJoin(Group as g).on(u.groupId, g.id).where.eq(u.groupId, 234).as(x)) }
     */
    def as(sq: SubQuerySQLSyntaxProvider): TableAsAliasSQLSyntax = {
      val syntax = sqls"(${this.toSQLSyntax}) ${SubQuery.as(sq)}"
      TableAsAliasSQLSyntax(syntax.value, syntax.rawParameters)
    }
  }

  /**
   * for update query builder
   */
  trait ForUpdateQuerySQLBuilder[A] extends SQLBuilder[A] {
    def forUpdate: PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} for update")
    def forUpdate(option: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} for update ${option}")
  }

  /**
   * Union query builder
   */
  trait UnionQuerySQLBuilder[A] extends SQLBuilder[A] {
    def union(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](
        sqls"${withRoundBracket(sql)} union ${withRoundBracket(anotherQuery)}"
      )

    def unionAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](
        sqls"${withRoundBracket(sql)} union all ${withRoundBracket(anotherQuery)}"
      )

    def union(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = union(
      anotherQuery.toSQLSyntax
    )
    def unionAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = unionAll(
      anotherQuery.toSQLSyntax
    )

    private def withRoundBracket(sqlQuery: SQLSyntax): SQLSyntax = {
      val statement = sqlQuery.value.trim()
      if (statement.startsWith("(") && statement.endsWith(")"))
        sqlQuery
      else sqls"(${sqlQuery})"
    }
  }

  /**
   * Except query builder
   */
  trait ExceptQuerySQLBuilder[A] extends SQLBuilder[A] {
    def except(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} except ${anotherQuery}")
    def exceptAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} except all ${anotherQuery}")
    def except(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = except(
      anotherQuery.toSQLSyntax
    )
    def exceptAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = exceptAll(
      anotherQuery.toSQLSyntax
    )
  }

  /**
   * Intersect query builder
   */
  trait IntersectQuerySQLBuilder[A] extends SQLBuilder[A] {
    def intersect(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} intersect ${anotherQuery}")
    def intersectAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${sql} intersect all ${anotherQuery}")
    def intersect(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = intersect(
      anotherQuery.toSQLSyntax
    )
    def intersectAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] =
      intersectAll(anotherQuery.toSQLSyntax)
  }

  /**
   * SQLBuilder for select queries.
   */
  case class SelectSQLBuilder[A](
    override val sql: SQLSyntax,
    lazyColumns: Boolean = false,
    resultAllProviders: List[ResultAllProvider] = Nil,
    ignoreOnClause: Boolean = false
  ) extends SQLBuilder[A]
    with SubQuerySQLBuilder[A] {

    private def appendResultAllProvider(
      table: TableAsAliasSQLSyntax,
      providers: List[ResultAllProvider]
    ) = {
      table.resultAllProvider
        .map(provider => provider :: resultAllProviders)
        .getOrElse(resultAllProviders)
    }

    // e.g. select.from(User as u)
    def from(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = this.copy(
      sql = sqls"${sql} from ${table}",
      resultAllProviders = appendResultAllProvider(table, resultAllProviders)
    )

    // ---
    // join query

    def join(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = innerJoin(
      table
    )

    // if table is none, this join part will be skipped
    def join(table: Option[TableAsAliasSQLSyntax]): SelectSQLBuilder[A] =
      innerJoin(table)

    def innerJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] =
      this.copy(
        sql = sqls"${sql} inner join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders),
        ignoreOnClause = false
      )

    // if table is none, this join part will be skipped
    def innerJoin(table: Option[TableAsAliasSQLSyntax]): SelectSQLBuilder[A] =
      table.map(innerJoin) getOrElse copy(ignoreOnClause = true)

    def leftJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] = this.copy(
      sql = sqls"${sql} left join ${table}",
      resultAllProviders = appendResultAllProvider(table, resultAllProviders),
      ignoreOnClause = false
    )

    // if table is none, this join part will be skipped
    def leftJoin(table: Option[TableAsAliasSQLSyntax]): SelectSQLBuilder[A] =
      table.map(leftJoin) getOrElse copy(ignoreOnClause = true)

    def rightJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] =
      this.copy(
        sql = sqls"${sql} right join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders),
        ignoreOnClause = false
      )

    // if table is none, this join part will be skipped
    def rightJoin(table: Option[TableAsAliasSQLSyntax]): SelectSQLBuilder[A] =
      table.map(rightJoin) getOrElse copy(ignoreOnClause = true)

    def crossJoin(table: TableAsAliasSQLSyntax): SelectSQLBuilder[A] =
      this.copy(
        sql = sqls"${sql} cross join ${table}",
        resultAllProviders = appendResultAllProvider(table, resultAllProviders),
        ignoreOnClause = false
      )

    // if table is none, this join part will be skipped
    def crossJoin(table: Option[TableAsAliasSQLSyntax]): SelectSQLBuilder[A] =
      table.map(crossJoin) getOrElse copy(ignoreOnClause = true)

    def on(onClause: SQLSyntax): SelectSQLBuilder[A] = {
      if (ignoreOnClause) this.copy(ignoreOnClause = false)
      else this.copy(sql = sqls"${sql} on ${onClause}", ignoreOnClause = false)
    }

    def on(left: SQLSyntax, right: SQLSyntax): SelectSQLBuilder[A] = {
      if (ignoreOnClause) this.copy(ignoreOnClause = false)
      else
        this.copy(
          sql = sqls"${sql} on ${left} = ${right}",
          ignoreOnClause = false
        )
    }

    // ---
    // sort, paging

    def orderBy(columns: SQLSyntax*): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.orderBy(columns: _*)}")
    def limit(n: Int): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.limit(n)}")
    def offset(n: Int): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.offset(n)}")

    // ---
    // group by, having

    def groupBy(columns: SQLSyntax*): GroupBySQLBuilder[A] =
      GroupBySQLBuilder[A](sqls"${toSQLSyntax} ${sqls.groupBy(columns: _*)}")
    def having(condition: SQLSyntax): GroupBySQLBuilder[A] =
      GroupBySQLBuilder[A](sqls"${toSQLSyntax} ${sqls.having(condition)}")

    // ---
    // union, except, intersect

    def union(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](
        sqls"${withRoundBracket(toSQLSyntax)} union ${withRoundBracket(anotherQuery)}"
      )
    def unionAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](
        sqls"${withRoundBracket(toSQLSyntax)} union all ${withRoundBracket(anotherQuery)}"
      )

    def union(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = union(
      anotherQuery.toSQLSyntax
    )
    def unionAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = unionAll(
      anotherQuery.toSQLSyntax
    )

    def except(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} except ${anotherQuery}")
    def exceptAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} except all ${anotherQuery}")
    def except(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = except(
      anotherQuery.toSQLSyntax
    )
    def exceptAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = exceptAll(
      anotherQuery.toSQLSyntax
    )

    def intersect(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} intersect ${anotherQuery}")
    def intersectAll(anotherQuery: SQLSyntax): PagingSQLBuilder[A] =
      PagingSQLBuilder[A](sqls"${toSQLSyntax} intersect all ${anotherQuery}")
    def intersect(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] = intersect(
      anotherQuery.toSQLSyntax
    )
    def intersectAll(anotherQuery: SQLBuilder[_]): PagingSQLBuilder[A] =
      intersectAll(anotherQuery.toSQLSyntax)

    // ---
    // where

    def where: ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.where}")

    def where(where: SQLSyntax): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.where(where)}")

    // Never append 'where' if whereOpt is empty.
    def where(whereOpt: Option[SQLSyntax]): ConditionSQLBuilder[A] =
      ConditionSQLBuilder[A](sqls"${toSQLSyntax} ${sqls.where(whereOpt)}")

    // ---
    // common functions

    override def append(part: SQLSyntax): SelectSQLBuilder[A] =
      this.copy(sql = sqls"${sql} ${part}")

    def map(
      mapper: SelectSQLBuilder[A] => SelectSQLBuilder[A]
    ): SelectSQLBuilder[A] = mapper.apply(this)

    private def withRoundBracket(sqlSyntax: SQLSyntax): SQLSyntax = {
      val statement = sqlSyntax.value.trim()
      if (statement.startsWith("(") && statement.endsWith(")"))
        sqlSyntax
      else sqls"(${sqlSyntax})"
    }

    private def lazyLoadedPart: SQLSyntax =
      sqls"select ${sqls.join(resultAllProviders.reverseIterator.map(_.resultAll).toSeq, sqls",")}"

    override def toSQLSyntax: SQLSyntax =
      if (lazyColumns) sqls"${lazyLoadedPart} ${sql}" else sqls"${sql}"
    override def toSQL: SQL[A, NoExtractor] =
      if (lazyColumns) sql"${lazyLoadedPart} ${sql}" else sql"${sql}"
  }

  /**
   * SQLBuilder for insert queries.
   */
  case class InsertSQLBuilder(override val sql: SQLSyntax)
    extends SQLBuilder[UpdateOperation] {

    def columns(columns: SQLSyntax*): InsertSQLBuilder =
      this.copy(sql = sqls"${sql} (${sqls.csv(columns: _*)})")
    def values(values: Any*): InsertSQLBuilder = {
      val vs = sqls.csv(values.map(v => sqls"${v}"): _*)
      this.copy(sql = sqls"${sql} values (${vs})")
    }
    def multipleValues(
      multipleValues: collection.Seq[Any]*
    ): InsertSQLBuilder = {
      val vs = multipleValues match {
        case Nil => Seq(sqls"()")
        case ss  => ss.map(s => sqls"(${sqls.toCSV(s.map(v => sqls"${v}"))})")
      }
      this.copy(sql = sqls"${sql} values ${sqls.join(vs, sqls",", false)}")
    }

    def namedValues(
      columnsAndValues: (SQLSyntax, ParameterBinder)*
    ): InsertSQLBuilder = {
      val (cs, vs) = columnsAndValues.unzip
      columns(cs: _*).values(vs: _*)
    }

    /**
     * This is a work around of a Scala compiler bug (SI-7420).
     *
     * @see [[https://github.com/scalikejdbc/scalikejdbc/pull/507]]
     */
    def namedValues(
      columnsAndValues: Map[SQLSyntax, ParameterBinder]
    ): InsertSQLBuilder = {
      val (cs, vs) = columnsAndValues.toSeq.unzip
      columns(cs: _*).values(vs: _*)
    }

    def select(columns: SQLSyntax*)(
      query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]
    ): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] =
        QueryDSL.select[Nothing](columns: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def selectAll(providers: ResultAllProvider*)(
      query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]
    ): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] =
        QueryDSL.select.all[Nothing](providers: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def select(
      query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]
    ): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] =
        new SelectSQLBuilder[Nothing](sql = SQLSyntax.empty, lazyColumns = true)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }

    /**
     * `returning id` for PostgreSQL
     */
    def returningId: InsertSQLBuilder = append(sqls"returning id")

    /**
     *  `returning` for PostgreSQL
     */
    def returning(columns: SQLSyntax*): InsertSQLBuilder = append(
      sqls"returning ${sqls.csv(columns: _*)}"
    )

    override def append(part: SQLSyntax): InsertSQLBuilder =
      this.copy(sql = sqls"${sql} ${part}")
  }

  /**
   * SQLBuilder for update queries.
   */
  case class UpdateSQLBuilder(override val sql: SQLSyntax)
    extends SQLBuilder[UpdateOperation]
    with WhereSQLBuilder[UpdateOperation] {

    def set(sqlPart: SQLSyntax): UpdateSQLBuilder =
      this.copy(sql = sqls"${sql} set ${sqlPart}")

    def set(tuples: (SQLSyntax, ParameterBinder)*): UpdateSQLBuilder = set(
      sqls.csv(tuples.map(each => sqls"${each._1} = ${each._2}"): _*)
    )

    /**
     * This is a work around of a Scala compiler bug (SI-7420).
     *
     * @see [[https://github.com/scalikejdbc/scalikejdbc/pull/507]]
     */
    def set(tuples: Map[SQLSyntax, ParameterBinder]): UpdateSQLBuilder = set(
      sqls.csv(tuples.map(each => sqls"${each._1} = ${each._2}").toSeq: _*)
    )

    /**
     *  `returning` for PostgreSQL
     */
    def returning(columns: SQLSyntax*): UpdateSQLBuilder = append(
      sqls"returning ${sqls.csv(columns: _*)}"
    )

    override def append(part: SQLSyntax): UpdateSQLBuilder =
      this.copy(sql = sqls"${sql} ${part}")
  }

  /**
   * SQLBuilder for delete queries.
   */
  case class DeleteSQLBuilder(override val sql: SQLSyntax)
    extends SQLBuilder[UpdateOperation]
    with WhereSQLBuilder[UpdateOperation] {

    override def append(part: SQLSyntax): DeleteSQLBuilder =
      this.copy(sql = sqls"${sql} ${part}")
  }

  case class BatchParamsBuilder(
    parameters: Seq[Seq[(SQLSyntax, ParameterBinder)]]
  ) {

    private[this] val results = parameters match {
      case x +: xs =>
        val (columns, params) = x.unzip
        (withPlaceholders(columns), params +: xs.map(_.map(_._2)))
      case _ =>
        (Nil, Nil)
    }

    private[this] def withPlaceholders(
      columns: Seq[SQLSyntax]
    ): Seq[(SQLSyntax, ParameterBinder)] = {
      columns.zip(List.fill(columns.size)(SQLSyntaxParameterBinder(sqls.?)))
    }

    val columnsAndPlaceholders: Seq[(SQLSyntax, ParameterBinder)] = results._1
    val batchParams: Seq[Seq[ParameterBinder]] = results._2
  }

}
