package scalikejdbc.orm.finder

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.Pagination
import scalikejdbc.orm.associations.AssociationsFeature
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  SQLSyntaxSupportBase
}
import scalikejdbc.orm.calculation.CalculationFeature
import scalikejdbc.orm.eagerloading.IncludesQueryRepository

trait NoIdFinderFeature[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with AssociationsFeature[Entity]
  with CalculationFeature[Entity] {

  override def extract(sql: SQL[Entity, NoExtractor])(implicit
    includesRepository: IncludesQueryRepository[Entity]
  ): SQL[Entity, HasExtractor] = {
    extractWithAssociations(
      sql,
      belongsToAssociations,
      hasOneAssociations,
      hasManyAssociations
    )
  }

  /**
   * Default ordering condition.
   * NOTE: sqls"" means empty to be compatible with 1.0 API.
   */
  def defaultOrdering: SQLSyntax = sqls""

  def defaultOrderings: Seq[SQLSyntax] =
    Seq(defaultOrdering).filter(_.length > 0)

  /**
   * Counts all rows by condition.
   */
  def countBy(where: SQLSyntax)(implicit s: DBSession = autoSession): Long = {
    withSQL {
      countQueryWithAssociations.where(where).and(defaultScopeWithDefaultAlias)
    }.map(_.long(1)).single.apply().getOrElse(0L)
  }

  /**
   * Finds all entities.
   */
  def findAll(
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit s: DBSession = autoSession): List[Entity] = {
    extract(withSQL {
      val sql = selectQueryWithAssociations.where(defaultScopeWithDefaultAlias)
      if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
    }).list.apply()
  }

  /**
   * Finds all entities with pagination.
   */
  def findAllWithPagination(
    pagination: Pagination,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {
    findAllWithLimitOffset(pagination.limit, pagination.offset, orderings)
  }

  /**
   * Finds all entities with pagination.
   */
  def findAllWithLimitOffset(
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {
    extract(withSQL {
      val sql = selectQueryWithAssociations.where(defaultScopeWithDefaultAlias)
      if (orderings.isEmpty) sql.limit(limit).offset(offset)
      else sql.orderBy(sqls.csv(orderings*)).limit(limit).offset(offset)
    }).list.apply()
  }

  /**
   * Finds an entity by condition.
   */
  def findBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Option[Entity] = {
    extract(withSQL {
      selectQueryWithAssociations.where(
        sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
      )
    }).single.apply()
  }

  /**
   * Finds all entities by condition.
   */
  def findAllBy(where: SQLSyntax, orderings: Seq[SQLSyntax] = defaultOrderings)(
    implicit s: DBSession = autoSession
  ): List[Entity] = {
    extract(withSQL {
      val sql = selectQueryWithAssociations.where(
        sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
      )
      if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
    }).list.apply()
  }

  /**
   * Finds all entities by condition and with pagination.
   */
  def findAllByWithLimitOffset(
    where: SQLSyntax,
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {
    extract(withSQL {
      val sql = selectQueryWithAssociations
        .where(
          sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
        )
      (if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*)))
        .limit(limit)
        .offset(offset)
    }).list.apply()
  }

  /**
   * Finds all entities by condition and with pagination.
   */
  def findAllByWithPagination(
    where: SQLSyntax,
    pagination: Pagination,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {
    findAllByWithLimitOffset(
      where,
      pagination.limit,
      pagination.offset,
      orderings
    )
  }

}
