package scalikejdbc.orm.querying

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.associations.NoIdAssociationsFeature
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  SQLSyntaxSupportBase
}
import scalikejdbc.orm.calculation.CalculationFeature
import scalikejdbc.orm.{ Alias, Pagination }

/**
 * Querying APIs feature.
 */
trait NoIdQueryingFeature[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with NoIdAssociationsFeature[Entity] {

  /**
   * Appends where conditions.
   *
   * @param conditions
   * @return query builder
   */
  def where(conditions: (String, Any)*): EntitiesSelectOperationBuilder =
    new EntitiesSelectOperationBuilder(
      mapper = this,
      conditions = conditions.flatMap { case (key, value) =>
        implicit val enableAsIs =
          ParameterBinderFactory.asisParameterBinderFactory
        value match {
          case None => Some(sqls.isNull(defaultAlias.field(key)))
          case Nil  => None
          case values: Seq[?] =>
            Some(
              sqls.in(
                defaultAlias.field(key),
                values.asInstanceOf[Seq[Any]]
              )
            )
          case value => Some(sqls.eq(defaultAlias.field(key), value))
        }
      }
    )

  /**
   * Appends a raw where condition.
   *
   * @param condition
   * @return query builder
   */
  def where(condition: SQLSyntax): EntitiesSelectOperationBuilder =
    new EntitiesSelectOperationBuilder(
      mapper = this,
      conditions = Seq(condition)
    )

  /**
   * Appends pagination settings as limit/offset.
   *
   * @param pagination pagination
   * @return query buildder
   */
  def paginate(pagination: Pagination): EntitiesSelectOperationBuilder = {
    new EntitiesSelectOperationBuilder(
      mapper = this,
      limit = Some(pagination.limit),
      offset = Some(pagination.offset)
    )
  }

  /**
   * Appends limit part.
   *
   * @param n value
   * @return query builder
   */
  def limit(n: Int): EntitiesSelectOperationBuilder =
    new EntitiesSelectOperationBuilder(mapper = this, limit = Some(n))

  /**
   * Appends offset part.
   *
   * @param n value
   * @return query builder
   */
  def offset(n: Int): EntitiesSelectOperationBuilder =
    new EntitiesSelectOperationBuilder(mapper = this, offset = Some(n))

  /**
   * Select query builder.
   *
   * @param mapper     mapper
   * @param conditions registered conditions
   * @param limit      limit
   * @param offset     offset
   */
  abstract class SelectOperationBuilder(
    mapper: NoIdQueryingFeature[Entity],
    conditions: Seq[SQLSyntax] = Nil,
    orderings: Seq[SQLSyntax] = Nil,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    isCountOnly: Boolean = false
  ) {

    /**
     * Appends where conditions.
     *
     * @param additionalConditions conditions
     * @return query builder
     */
    def where(
      additionalConditions: (String, Any)*
    ): EntitiesSelectOperationBuilder =
      new EntitiesSelectOperationBuilder(
        mapper = this.mapper,
        conditions =
          conditions ++ additionalConditions.flatMap { case (key, value) =>
            implicit val enableAsIs =
              ParameterBinderFactory.asisParameterBinderFactory
            value match {
              case Nil => None
              case values: Seq[?] =>
                Some(
                  sqls.in(
                    defaultAlias.field(key),
                    values.asInstanceOf[Seq[Any]]
                  )
                )
              case value => Some(sqls.eq(defaultAlias.field(key), value))
            }
          },
        orderings = orderings,
        limit = limit,
        offset = offset
      )

    /**
     * Appends a raw where condition.
     *
     * @param condition
     * @return query builder
     */
    def where(condition: SQLSyntax): EntitiesSelectOperationBuilder =
      new EntitiesSelectOperationBuilder(
        mapper = this.mapper,
        conditions = conditions ++ Seq(condition),
        limit = limit,
        offset = offset
      )

  }

  /**
   * Entities finder builder.
   *
   * @param mapper     mapper
   * @param conditions registered conditions
   * @param limit      limit
   * @param offset     offset
   */
  case class EntitiesSelectOperationBuilder(
    mapper: NoIdQueryingFeature[Entity],
    conditions: Seq[SQLSyntax] = Nil,
    orderings: Seq[SQLSyntax] = Nil,
    limit: Option[Int] = None,
    offset: Option[Int] = None
  ) extends SelectOperationBuilder(
      mapper,
      conditions,
      orderings,
      limit,
      offset,
      false
    )
    with CalculationFeature[Entity] {

    override def tableName = mapper.tableName

    override def defaultAlias: Alias[Entity] = mapper.defaultAlias

    override def extract(rs: WrappedResultSet, n: ResultName[Entity]): Entity =
      mapper.extract(rs, n)

    override def singleSelectQuery = mapper.singleSelectQuery

    override def defaultScopeWithDefaultAlias =
      mapper.defaultScopeWithDefaultAlias

    override def defaultOrderings: Seq[SQLSyntax] = mapper.defaultOrderings

    /**
     * Calculates rows.
     */
    override def calculate(
      sql: SQLSyntax
    )(implicit s: DBSession = autoSession): BigDecimal = {
      withSQL {
        val q: SelectSQLBuilder[Entity] = select(sql).from(as(defaultAlias))
        conditions match {
          case Nil => q.where(defaultScopeWithDefaultAlias)
          case _ =>
            conditions.tail
              .foldLeft(q.where(conditions.head)) { case (query, condition) =>
                query.and.append(condition)
              }
              .and(defaultScopeWithDefaultAlias)
        }
      }.map(_.bigDecimal(1))
        .single
        .apply()
        .map(_.toScalaBigDecimal)
        .getOrElse(BigDecimal(0))
    }

    /**
     * Appends pagination settings as limit/offset.
     *
     * @param pagination pagination
     * @return query buildder
     */
    def paginate(pagination: Pagination): EntitiesSelectOperationBuilder = {
      this.copy(
        limit = Some(pagination.limit),
        offset = Some(pagination.offset)
      )
    }

    /**
     * Appends limit part.
     *
     * @param n value
     * @return query builder
     */
    def limit(n: Int): EntitiesSelectOperationBuilder =
      this.copy(limit = Some(n))

    /**
     * Appends offset part.
     *
     * @param n value
     * @return query builder
     */
    def offset(n: Int): EntitiesSelectOperationBuilder =
      this.copy(offset = Some(n))

    /**
     * Appends order by condition.
     *
     * @param orderings orderings
     * @return query builder
     */
    def orderBy(orderings: SQLSyntax*): EntitiesSelectOperationBuilder =
      this.copy(orderings = orderings)

    /**
     * Actually applies SQL to the DB.
     */
    def apply()(implicit session: DBSession = autoSession): List[Entity] = {
      mapper
        .extract(withSQL {

          def query(conditions: Seq[SQLSyntax]): SQLBuilder[Entity] = {
            conditions match {
              case Nil =>
                selectQueryWithAssociations.where(defaultScopeWithDefaultAlias)
              case _ =>
                conditions.tail
                  .foldLeft(
                    selectQueryWithAssociations.where(conditions.head)
                  ) { case (query, condition) =>
                    query.and.append(condition)
                  }
                  .and(defaultScopeWithDefaultAlias)
            }
          }

          def appendOrderingIfExists(query: SQLBuilder[Entity]) = {
            if (orderings.isEmpty) query
            else query.append(sqls"order by").append(sqls.csv(orderings*))
          }

          val pagination = {
            sqls.join(
              Seq(
                limit.map(l => sqls.limit(l)),
                offset.map(o => sqls.offset(o))
              ).flatten,
              sqls" "
            )
          }

          if (
            hasManyAssociations.size > 0 && (limit.isDefined || offset.isDefined)
          ) {
            // find ids for pagination
            val queryForIds = (conditions match {
              case Nil => singleSelectQuery.where(defaultScopeWithDefaultAlias)
              case _ =>
                conditions.tail
                  .foldLeft(singleSelectQuery.where(conditions.head)) {
                    case (query, condition) => query.and.append(condition)
                  }
                  .and(defaultScopeWithDefaultAlias)
            })
            val ids: List[Any] = withSQL {
              queryForIds
                .orderBy(orderings.headOption.getOrElse(primaryKeyField))
                .append(pagination)
            }.map(_.any(defaultAlias.resultName.field(primaryKeyFieldName)))
              .list
              .apply()

            if (ids.isEmpty) return Nil
            else {
              implicit val enableAsIs =
                ParameterBinderFactory.asisParameterBinderFactory
              appendOrderingIfExists(
                query(
                  conditions :+ sqls
                    .in(defaultAlias.field(primaryKeyFieldName), ids)
                )
              )
            }
          } else {
            appendOrderingIfExists(query(conditions)).append(pagination)
          }

        })
        .list
        .apply()
    }

  }

}
