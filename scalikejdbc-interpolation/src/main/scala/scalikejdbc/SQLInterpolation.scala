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

import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.language.experimental.macros
import scala.language.dynamics

/**
 * SQLInterpolation imports.
 */
object SQLInterpolation {

  // ---------------------------------
  // Implicit conversions
  // ---------------------------------

  @inline implicit def scalikejdbcSQLInterpolationImplicitDef(s: StringContext) = new scalikejdbc.SQLInterpolationString(s)

  @inline implicit def scalikejdbcSQLSyntaxToStringImplicitDef(syntax: scalikejdbc.interpolation.SQLSyntax): String = syntax.value

  // ---------------------------------
  // Query DSL
  // ---------------------------------

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

    def toSQLSyntax: SQLSyntax = sqls"${sql}"
    def toSQL: SQL[A, NoExtractor] = sql"${sql}"
  }

  // Featureless constructor for SQLBuilder
  private[scalikejdbc] class RawSQLBuilder[A](val sql: SQLSyntax) extends SQLBuilder[A]

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
    def having(condition: SQLSyntax): GroupBySQLBuilder[A] = GroupBySQLBuilder[A](sql = sqls"${sql} ${sqls.having(condition)}")
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

    /**
     * Appends SQLSyntax directly.
     * e.g. select.from(User as u).where.eq(u.id, 123).append(sqls"order by ${u.id} desc")
     */
    def append(part: SQLSyntax): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${part}")

    /**
     * Maps SQLBuilder as follows.
     * e.g. select.from(User as u).where.eq(u.id, 123).map { sql => if(name.isDefined) sql.and.eq(u.name, name) else sql }
     */
    def map(mapper: ConditionSQLBuilder[A] => ConditionSQLBuilder[A]): ConditionSQLBuilder[A] = mapper.apply(this)

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

    def between(a: Any, b: Any): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.between(a, b)}")
    def in(column: SQLSyntax, values: Seq[Any]): ConditionSQLBuilder[A] = ConditionSQLBuilder[A](sqls"${sql} ${sqls.in(column, values)}")

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

  }

  trait SubQuerySQLBuilder[A] extends SQLBuilder[A] {

    /**
     * Converts SQLBuilder to sub-query part sqls.
     * e.g.
     *   val x = SubQuery.syntax("x").include(u, g)
     *   withSQL { select.from(select.from(User as u).leftJoin(Group as g).on(u.groupId, g.id).where.eq(u.groupId, 234).as(x)) }
     */
    def as(sq: SubQuerySQLSyntaxProvider): TableAsAliasSQLSyntax = TableAsAliasSQLSyntax(sqls"(${this.toSQLSyntax}) ${SubQuery.as(sq)}")
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

    // ---

    /**
     * Appends SQLSyntax directly.
     */
    def append(part: SQLSyntax): SelectSQLBuilder[A] = this.copy(sql = sqls"${sql} ${part}")

    /**
     * Maps SQLBuilder as follows.
     * e.g. select.from(User as u).map { sql => if (groupRequired) sql.leftJoin(Group as g).on(u.groupId, g.id) else sql }
     */
    def map(mapper: SelectSQLBuilder[A] => SelectSQLBuilder[A]): SelectSQLBuilder[A] = mapper.apply(this)

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
    import sqls.csv
    def columns(columns: SQLSyntax*): InsertSQLBuilder = this.copy(sql = sqls"${sql} (${csv(columns: _*)})")
    def values(values: Any*): InsertSQLBuilder = this.copy(sql = sqls"${sql} values (${values})")

    def select(columns: SQLSyntax*)(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = scalikejdbc.SQLInterpolation.select(columns: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def selectAll(providers: ResultAllProvider*)(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = scalikejdbc.SQLInterpolation.select.all(providers: _*)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
    def select(query: SelectSQLBuilder[Nothing] => SQLBuilder[Nothing]): InsertSQLBuilder = {
      val builder: SelectSQLBuilder[Nothing] = new SelectSQLBuilder[Nothing](sql = sqls"", lazyColumns = true)
      this.copy(sql = sqls"${sql} ${query.apply(builder).toSQLSyntax}")
    }
  }

  /**
   * SQLBuilder for update queries.
   */
  case class UpdateSQLBuilder(override val sql: SQLSyntax) extends SQLBuilder[UpdateOperation]
      with WhereSQLBuilder[UpdateOperation] {

    def set(sqlPart: SQLSyntax): UpdateSQLBuilder = this.copy(sql = sqls"${sql} set ${sqlPart}")
    def set(tuples: (SQLSyntax, Any)*): UpdateSQLBuilder = set(sqls.csv(tuples.map(each => sqls"${each._1} = ${each._2}"): _*))
  }

  /**
   * SQLBuilder for delete queries.
   */
  case class DeleteSQLBuilder(override val sql: SQLSyntax) extends SQLBuilder[UpdateOperation]
    with WhereSQLBuilder[UpdateOperation]

  // ---------------------------------
  // SQL Interpolation Core Elements
  // ---------------------------------

  /**
   * Loaded columns for tables.
   */
  private[scalikejdbc] val SQLSyntaxSupportLoadedColumns = new scala.collection.concurrent.TrieMap[String, Seq[String]]()

  /**
   * SQLSyntaxSupport trait. Companion object needs this trait as follows.
   *
   * {{{
   *   case class Member(id: Long, name: Option[String])
   *   object Member extends SQLSyntaxSupport[Member]
   * }}}
   */
  trait SQLSyntaxSupport[A] {

    /**
     * Table name (default: the snake_case name from this companion object's name).
     */
    def tableName: String = {
      val className = this.getClass.getName.replaceFirst("\\$$", "").replaceFirst("^.+\\.", "").replaceFirst("^.+\\$", "")
      SQLSyntaxProvider.toColumnName(className, nameConverters, useSnakeCaseColumnName)
    }

    /**
     * [[scalikekdbc.SQLSyntax]] value for table name.
     */
    def table: TableDefSQLSyntax = TableDefSQLSyntax(tableName)

    /**
     * Column names for this table (default: column names that are loaded from JDBC metadata).
     */
    def columns: Seq[String] = SQLSyntaxSupportLoadedColumns.getOrElseUpdate(tableName, DB.getColumnNames(tableName).map(_.toLowerCase))

    /**
     * True if you need forcing upper column names in SQL.
     */
    def forceUpperCase: Boolean = false

    /**
     * True if you need shortening alias names in SQL.
     */
    def useShortenedResultName: Boolean = true

    /**
     * True if you need to convert filed names to snake_case column names in SQL.
     */
    def useSnakeCaseColumnName: Boolean = true

    /**
     * Delimiter for alias names in SQL.
     */
    def delimiterForResultName = if (forceUpperCase) "_ON_" else "_on_"

    /**
     * Rule to convert field names to column names.
     *
     * {{{
     *   override val nameConverters = Map("^serviceCode$" -> "service_cd")
     * }}}
     */
    def nameConverters: Map[String, String] = Map()

    /**
     * Returns ColumnName provider for this (expected to use for insert/update queries).
     */
    def column: ColumnName[A] = ColumnSQLSyntaxProvider[SQLSyntaxSupport[A], A](this)

    /**
     * Returns SQLSyntax provider for this.
     *
     * {{{
     *   val m = Member.syntax
     *   sql"select ${m.result.*} from ${Member as m}".map(Member(m.resultName)).list.apply()
     *   // select member.id as i_on_member, member.name as n_on_member from member
     * }}}
     */
    def syntax = {
      val _name = if (forceUpperCase) tableName.toUpperCase else tableName
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }

    /**
     * Returns SQLSyntax provider for this.
     *
     * {{{
     *   val m = Member.syntax("m")
     *   sql"select ${m.result.*} from ${Member as m}".map(Member(m.resultName)).list.apply()
     *   // select m.id as i_on_m, m.name as n_on_m from member m
     * }}}
     */
    def syntax(name: String) = {
      val _name = if (forceUpperCase) name.toUpperCase else name
      QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A](this, _name)
    }

    /**
     * Returns table name and alias name part in SQL. If alias name and table name are same, alias name will be skipped.
     *
     * {{{
     *   sql"select ${m.result.*} from ${Member.as(m)}"
     * }}}
     */
    def as(provider: QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]): TableAsAliasSQLSyntax = {
      if (tableName == provider.tableAliasName) { TableAsAliasSQLSyntax(table, Some(provider)) }
      else { TableAsAliasSQLSyntax(SQLSyntax(tableName + " " + provider.tableAliasName), Some(provider)) }
    }
  }

  /**
   * Table definition (which has alias name) part SQLSyntax
   */
  case class TableAsAliasSQLSyntax private[scalikejdbc] (
    override val value: String,
    override val parameters: Seq[Any] = Vector(),
    resultAllProvider: Option[ResultAllProvider] = None) extends SQLSyntax(value, parameters)

  object TableAsAliasSQLSyntax {
    def apply(syntax: SQLSyntax, resultAllProvider: Option[ResultAllProvider]) = new TableAsAliasSQLSyntax(syntax.value, syntax.parameters, resultAllProvider)
  }

  /**
   * Table definition part SQLSyntax
   */
  case class TableDefSQLSyntax private[scalikejdbc] (
    override val value: String, override val parameters: Seq[Any] = Vector())
      extends SQLSyntax(value, parameters)

  /**
   * SQLSyntax Provider
   */
  trait SQLSyntaxProvider[A] extends Dynamic {
    import SQLSyntaxProvider._
    import scala.reflect.runtime.universe._

    /**
     * Rule to convert field names to column names.
     */
    val nameConverters: Map[String, String]

    /**
     * True if you need forcing upper column names in SQL.
     */
    val forceUpperCase: Boolean

    /**
     * Delimiter for alias names in SQL.
     */
    val delimiterForResultName: String

    /**
     * True if you need to convert filed names to snake_case column names in SQL.
     */
    val useSnakeCaseColumnName: Boolean

    /**
     * Returns [[scalikejdbc.SQLSyntax]] value for the column.
     */
    def c(name: String): SQLSyntax = column(name)

    /**
     * Returns [[scalikejdbc.SQLSyntax]] value for the column.
     */
    def column(name: String): SQLSyntax

    /**
     * Returns [[scalikejdbc.SQLSyntax]] value for the column which is referred by the field.
     */
    def field(name: String): SQLSyntax = {
      val columnName = {
        if (forceUpperCase) toColumnName(name, nameConverters, useSnakeCaseColumnName).toUpperCase
        else toColumnName(name, nameConverters, useSnakeCaseColumnName)
      }
      c(columnName)
    }

    /**
     * Returns [[scalikejdbc.SQLSyntax]] value for the column which is referred by the field.
     */
    def selectDynamic(name: String): SQLSyntax = macro scalikejdbc.SQLInterpolationMacro.selectDynamic[A]

  }

  /**
   * SQLSyntaxProvider companion.
   */
  private[scalikejdbc] object SQLSyntaxProvider {

    private val acronymRegExpStr = "[A-Z]{2,}"
    private val acronymRegExp = acronymRegExpStr.r
    private val endsWithAcronymRegExpStr = "[A-Z]{2,}$"
    private val singleUpperCaseRegExp = """[A-Z]""".r

    /**
     * Returns the snake_case name after applying nameConverters.
     */
    def toColumnName(str: String, nameConverters: Map[String, String], useSnakeCaseColumnName: Boolean): String = {
      val convertersApplied = nameConverters.foldLeft(str) { case (s, (from, to)) => s.replaceAll(from, to) }
      if (useSnakeCaseColumnName) {
        val acronymsFiltered = acronymRegExp.replaceAllIn(
          acronymRegExp.findFirstMatchIn(convertersApplied).map { m =>
            convertersApplied.replaceFirst(endsWithAcronymRegExpStr, "_" + m.matched.toLowerCase)
          }.getOrElse(convertersApplied), // might end with an acronym
          { m => "_" + m.matched.init.toLowerCase + "_" + m.matched.last.toString.toLowerCase }
        )
        val result = singleUpperCaseRegExp.replaceAllIn(acronymsFiltered, { m => "_" + m.matched.toLowerCase })
          .replaceFirst("^_", "")
          .replaceFirst("_$", "")

        if (str.startsWith("_")) "_" + result
        else if (str.endsWith("_")) result + "_"
        else result

      } else {
        convertersApplied
      }
    }

    /**
     * Returns the shortened name for the name.
     */
    def toShortenedName(name: String, columns: Seq[String]): String = {
      def shorten(s: String): String = s.split("_").map(word => word.take(1)).mkString

      val shortenedName = shorten(toAlphabetOnly(name))
      val shortenedNames = columns.map(c => shorten(toAlphabetOnly(c)))
      if (shortenedNames.filter(_ == shortenedName).size > 1) {
        val (n, found) = columns.zip(shortenedNames).foldLeft((1, false)) {
          case ((n, found), (column, shortened)) =>
            if (found) {
              (n, found) // alread found
            } else if (column == name) {
              (n, true) // original name is expected
            } else if (shortened == shortenedName) {
              (n + 1, false) // original name is different but shorten name is same
            } else {
              (n, found) // not found yet
            }
        }
        if (!found) throw new IllegalStateException("This must be a library bug.")
        else shortenedName + n
      } else {
        shortenedName
      }
    }

    /**
     * Returns the alias name for the name.
     */
    def toAliasName(originalName: String, support: SQLSyntaxSupport[_]): String = {
      if (support.useShortenedResultName) toShortenedName(originalName, support.columns)
      else originalName
    }

    /**
     * Returns the name which is converted to pure alphabet only.
     */
    private[this] def toAlphabetOnly(name: String): String = {
      val _name = name.filter(c => c.isLetter && c <= 'z' || c == '_')
      if (_name.size == 0) "x" else _name
    }

  }

  /**
   * SQLSyntax provider for column names.
   */
  case class ColumnSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S) extends SQLSyntaxProvider[A]
      with AsteriskProvider {

    val nameConverters = support.nameConverters
    val forceUpperCase = support.forceUpperCase
    val useSnakeCaseColumnName = support.useSnakeCaseColumnName

    lazy val delimiterForResultName = throw new UnsupportedOperationException("It's a library bug if this exception is thrown.")

    val columns: Seq[SQLSyntax] = support.columns.map { c => if (support.forceUpperCase) c.toUpperCase else c }.map(c => SQLSyntax(c))

    val * : SQLSyntax = SQLSyntax(columns.map(_.value).mkString(", "))

    val asterisk: SQLSyntax = sqls"*"

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(c.value)
    }.getOrElse {
      throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME +
        s" (name: ${name}, registered names: ${columns.map(_.value).mkString(",")})")
    }

  }

  /**
   * SQLSyntax Provider basic implementation.
   */
  private[scalikejdbc] abstract class SQLSyntaxProviderCommonImpl[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProvider[A] {

    val nameConverters = support.nameConverters
    val forceUpperCase = support.forceUpperCase
    val useSnakeCaseColumnName = support.useSnakeCaseColumnName
    val delimiterForResultName = support.delimiterForResultName
    val columns: Seq[SQLSyntax] = support.columns.map { c => if (support.forceUpperCase) c.toUpperCase else c }.map(c => SQLSyntax(c))

    def notFoundInColumns(aliasName: String, name: String): InvalidColumnNameException = notFoundInColumns(aliasName, name, columns.map(_.value).mkString(","))

    def notFoundInColumns(aliasName: String, name: String, registeredNames: String): InvalidColumnNameException = {
      new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})")
    }

  }

  /**
   * SQLSyntax provider for query parts.
   */
  case class QuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName)
      with ResultAllProvider
      with AsteriskProvider {

    val result: ResultSQLSyntaxProvider[S, A] = {
      val table = if (support.forceUpperCase) tableAliasName.toUpperCase else tableAliasName
      ResultSQLSyntaxProvider[S, A](support, table)
    }

    override def resultAll: SQLSyntax = result.*

    val resultName: BasicResultNameSQLSyntaxProvider[S, A] = result.name

    val * : SQLSyntax = SQLSyntax(columns.map(c => s"${tableAliasName}.${c.value}").mkString(", "))

    val asterisk: SQLSyntax = SQLSyntax(tableAliasName + ".*")

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      SQLSyntax(s"${tableAliasName}.${c.value}")
    }.getOrElse {
      throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME +
        s" (name: ${tableAliasName}.${name}, registered names: ${columns.map(_.value).mkString(",")})")
    }

  }

  /**
   * SQLSyntax provider for result parts.
   */
  case class ResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) {
    import SQLSyntaxProvider._

    val name: BasicResultNameSQLSyntaxProvider[S, A] = BasicResultNameSQLSyntaxProvider[S, A](support, tableAliasName)

    val * : SQLSyntax = SQLSyntax(columns.map { c =>
      val name = toAliasName(c.value, support)
      s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    def apply(syntax: SQLSyntax): PartialResultSQLSyntaxProvider[S, A] = PartialResultSQLSyntaxProvider(support, tableAliasName, syntax)

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      val name = toAliasName(c.value, support)
      SQLSyntax(s"${tableAliasName}.${c.value} as ${name}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(tableAliasName, name))
  }

  case class PartialResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, aliasName: String, syntax: SQLSyntax)
      extends SQLSyntaxProviderCommonImpl[S, A](support, aliasName) {
    import SQLSyntaxProvider._

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      val name = toAliasName(c.value, support)
      SQLSyntax(s"${syntax.value} as ${name}${delimiterForResultName}${aliasName}", syntax.parameters)
    }.getOrElse(throw notFoundInColumns(aliasName, name))
  }

  /**
   * SQLSyntax provider for result names.
   */
  trait ResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A] extends SQLSyntaxProvider[A] {
    def * : SQLSyntax
    def namedColumns: Seq[SQLSyntax]
    def namedColumn(name: String): SQLSyntax
    def column(name: String): SQLSyntax
  }

  /**
   * Basic Query SQLSyntax Provider for result names.
   */
  case class BasicResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](support: S, tableAliasName: String)
      extends SQLSyntaxProviderCommonImpl[S, A](support, tableAliasName) with ResultNameSQLSyntaxProvider[S, A] {
    import SQLSyntaxProvider._

    val * : SQLSyntax = SQLSyntax(columns.map { c =>
      val name = toAliasName(c.value, support)
      s"${name}${delimiterForResultName}${tableAliasName}"
    }.mkString(", "))

    val namedColumns: Seq[SQLSyntax] = support.columns.map { columnName: String =>
      val name = toAliasName(SQLSyntax(columnName), support)
      SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
    }

    def namedColumn(name: String) = namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME +
        s" (name: ${name}, registered names: ${namedColumns.map(_.value).mkString(",")})")
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).map { c =>
      val name = toAliasName(c.value, support)
      SQLSyntax(s"${name}${delimiterForResultName}${tableAliasName}")
    }.getOrElse(throw notFoundInColumns(tableAliasName, name))
  }

  // subquery syntax providers

  object SubQuery {

    def syntax(name: String, resultNames: BasicResultNameSQLSyntaxProvider[_, _]*): SubQuerySQLSyntaxProvider = {
      SubQuerySQLSyntaxProvider(name, resultNames.head.delimiterForResultName, resultNames)
    }

    def syntax(name: String, delimiterForResultName: String, resultNames: BasicResultNameSQLSyntaxProvider[_, _]*): SubQuerySQLSyntaxProvider = {
      SubQuerySQLSyntaxProvider(name, delimiterForResultName, resultNames)
    }

    def syntax(name: String): SubQuerySQLSyntaxProviderBuilder = SubQuerySQLSyntaxProviderBuilder(name)

    def syntax(name: String, delimiterForResultName: String): SubQuerySQLSyntaxProviderBuilder = {
      SubQuerySQLSyntaxProviderBuilder(name, Option(delimiterForResultName))
    }

    case class SubQuerySQLSyntaxProviderBuilder(name: String, delimiterForResultName: Option[String] = None) {
      def include(syntaxProviders: QuerySQLSyntaxProvider[_, _]*): SubQuerySQLSyntaxProvider = {
        SubQuery.syntax(
          name,
          delimiterForResultName.getOrElse(syntaxProviders.head.resultName.delimiterForResultName),
          syntaxProviders.map(_.resultName): _*)
      }
    }

    def as(subquery: SubQuerySQLSyntaxProvider): TableDefSQLSyntax = TableDefSQLSyntax(subquery.aliasName)

  }

  case class SubQuerySQLSyntaxProvider(
    aliasName: String,
    delimiterForResultName: String,
    resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]])
      extends ResultAllProvider
      with AsteriskProvider {

    val result: SubQueryResultSQLSyntaxProvider = SubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, resultNames)
    val resultName: SubQueryResultNameSQLSyntaxProvider = result.name

    override def resultAll: SQLSyntax = result.*

    val * : SQLSyntax = SQLSyntax(resultNames.map { resultName =>
      resultName.namedColumns.map { c =>
        s"${aliasName}.${c.value}"
      }
    }.mkString(", "))

    val asterisk: SQLSyntax = SQLSyntax(aliasName + ".*")

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.value.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${aliasName}.${rn.namedColumn(name).value}")
      }.getOrElse {
        val registeredNames = resultNames.map { rn => rn.columns.map(_.value).mkString(",") }.mkString(",")
        throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name.value}, registered names: ${registeredNames})")
      }
    }

    def apply[S <: SQLSyntaxSupport[A], A](syntax: QuerySQLSyntaxProvider[S, A]): PartialSubQuerySQLSyntaxProvider[S, A] = {
      PartialSubQuerySQLSyntaxProvider(aliasName, delimiterForResultName, syntax.resultName)
    }

  }

  case class SubQueryResultSQLSyntaxProvider(
      aliasName: String,
      delimiterForResultName: String,
      resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]]) {

    def name: SubQueryResultNameSQLSyntaxProvider = SubQueryResultNameSQLSyntaxProvider(aliasName, delimiterForResultName, resultNames)

    def * : SQLSyntax = SQLSyntax(resultNames.map { rn =>
      rn.namedColumns.map { c =>
        s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
      }.mkString(", ")
    }.mkString(", "))

    def column(name: String): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${aliasName}.${rn.column(name)} as ${rn.column(name)}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        val registeredNames = resultNames.map { rn => rn.columns.map(_.value).mkString(",") }.mkString(",")
        throw new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${name}, registered names: ${registeredNames})")
      }
    }

  }

  case class SubQueryResultNameSQLSyntaxProvider(
      aliasName: String,
      delimiterForResultName: String,
      resultNames: Seq[BasicResultNameSQLSyntaxProvider[_, _]]) {

    val * : SQLSyntax = SQLSyntax(resultNames.map { rn =>
      rn.namedColumns.map { c =>
        s"${c.value}${delimiterForResultName}${aliasName}"
      }.mkString(", ")
    }.mkString(", "))

    val columns: Seq[SQLSyntax] = resultNames.flatMap { rn =>
      rn.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }
    }

    def column(name: String): SQLSyntax = columns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(aliasName, name)
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      resultNames.find(rn => rn.namedColumns.find(_.value.toLowerCase == name.toLowerCase).isDefined).map { rn =>
        SQLSyntax(s"${rn.namedColumn(name).value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value)
      }
    }

    def notFoundInColumns(aliasName: String, name: String) = {
      val registeredNames = resultNames.map { rn => rn.namedColumns.map(_.value).mkString(",") }.mkString(",")
      new InvalidColumnNameException(ErrorMessage.INVALID_COLUMN_NAME + s" (name: ${aliasName}.${name}, registered names: ${registeredNames})")
    }

  }

  // -----
  // partial subquery syntax providers

  case class PartialSubQuerySQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName)
      with AsteriskProvider {

    val result: PartialSubQueryResultSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    val resultName: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = result.name

    val * : SQLSyntax = SQLSyntax(resultName.namedColumns.map { c => s"${aliasName}.${c.value}" }.mkString(", "))

    val asterisk: SQLSyntax = SQLSyntax(aliasName + ".*")

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { _ =>
        SQLSyntax(s"${aliasName}.${underlying.namedColumn(name).value}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value, resultName.columns.map(_.value).mkString(","))
      }
    }

    def column(name: String) = {
      SQLSyntax(s"${aliasName}.${underlying.column(name).value}")
    }

  }

  case class PartialSubQueryResultSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) {

    val name: PartialSubQueryResultNameSQLSyntaxProvider[S, A] = {
      PartialSubQueryResultNameSQLSyntaxProvider(aliasName, delimiterForResultName, underlying)
    }

    val * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      s"${aliasName}.${c.value} as ${c.value}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    def column(name: String): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${aliasName}.${nc.value} as ${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name, underlying.columns.map(_.value).mkString(","))
      }
    }

  }

  case class PartialSubQueryResultNameSQLSyntaxProvider[S <: SQLSyntaxSupport[A], A](
    aliasName: String,
    override val delimiterForResultName: String,
    underlying: BasicResultNameSQLSyntaxProvider[S, A])
      extends SQLSyntaxProviderCommonImpl[S, A](underlying.support, aliasName) with ResultNameSQLSyntaxProvider[S, A] {
    import SQLSyntaxProvider._

    val * : SQLSyntax = SQLSyntax(underlying.namedColumns.map { c =>
      val name = toAliasName(c.value, underlying.support)
      s"${name}${delimiterForResultName}${aliasName}"
    }.mkString(", "))

    override val columns: Seq[SQLSyntax] = underlying.namedColumns.map { c => SQLSyntax(s"${c.value}${delimiterForResultName}${aliasName}") }

    def column(name: String): SQLSyntax = underlying.columns.find(_.value.toLowerCase == name.toLowerCase).map { original: SQLSyntax =>
      val name = toAliasName(original.value, underlying.support)
      SQLSyntax(s"${name}${delimiterForResultName}${underlying.tableAliasName}${delimiterForResultName}${aliasName}")
    }.getOrElse {
      throw notFoundInColumns(aliasName, name, underlying.columns.map(_.value).mkString(","))
    }

    val namedColumns: Seq[SQLSyntax] = underlying.namedColumns.map { nc: SQLSyntax =>
      SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
    }

    def namedColumn(name: String) = underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).getOrElse {
      throw notFoundInColumns(aliasName, name, namedColumns.map(_.value).mkString(","))
    }

    def apply(name: SQLSyntax): SQLSyntax = {
      underlying.namedColumns.find(_.value.toLowerCase == name.toLowerCase).map { nc =>
        SQLSyntax(s"${nc.value}${delimiterForResultName}${aliasName}")
      }.getOrElse {
        throw notFoundInColumns(aliasName, name.value, underlying.columns.map(_.value).mkString(","))
      }
    }

  }

  // ---------------------------------
  // Type aliases
  // ---------------------------------

  type ColumnName[A] = ColumnSQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type ResultName[A] = ResultNameSQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type SubQueryResultName = SubQueryResultNameSQLSyntaxProvider
  type SyntaxProvider[A] = QuerySQLSyntaxProvider[SQLSyntaxSupport[A], A]
  type SubQuerySyntaxProvider = SubQuerySQLSyntaxProvider

  type SQLSyntax = scalikejdbc.interpolation.SQLSyntax
  val SQLSyntax = scalikejdbc.interpolation.SQLSyntax
  val sqls = scalikejdbc.interpolation.SQLSyntax

  type ResultAllProvider = scalikejdbc.interpolation.ResultAllProvider
  type AsteriskProvider = scalikejdbc.interpolation.AsteriskProvider

}

