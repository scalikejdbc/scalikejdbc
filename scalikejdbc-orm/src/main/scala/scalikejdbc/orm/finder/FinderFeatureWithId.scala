package scalikejdbc.orm.finder

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.Pagination
import scalikejdbc.orm.associations.{ AssociationsFeature, JoinsFeature }
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  IdFeature,
  SQLSyntaxSupportBase
}
import scalikejdbc.orm.eagerloading.{
  IncludesFeatureWithId,
  IncludesQueryRepository
}

/**
  * Provides #find something APIs.
  */
trait FinderFeatureWithId[Id, Entity]
  extends SQLSyntaxSupportBase[Entity]
  with NoIdFinderFeature[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with AssociationsFeature[Entity]
  with JoinsFeature[Entity]
  with IdFeature[Id]
  with IncludesFeatureWithId[Id, Entity] {

  /**
    * Default ordering condition.
    */
  override def defaultOrdering: SQLSyntax = primaryKeyField

  /**
    * Finds a single entity by primary key.
    */
  def findById(id: Id)(implicit s: DBSession = autoSession): Option[Entity] = {
    implicit val enableAsIs = ParameterBinderFactory.asisParameterBinderFactory
    implicit val repository = IncludesQueryRepository[Entity]()
    appendIncludedAttributes(extract(withSQL {
      selectQueryWithAssociations.where
        .eq(primaryKeyField, idToRawValue(id))
        .and(defaultScopeWithDefaultAlias)
    }).single.apply())
  }

  /**
    * Finds all entities by several primary keys.
    */
  def findAllByIds(
    ids: Id*
  )(implicit s: DBSession = autoSession): List[Entity] = {
    implicit val enableAsIs = ParameterBinderFactory.asisParameterBinderFactory
    implicit val repository = IncludesQueryRepository[Entity]()
    appendIncludedAttributes(extract(withSQL {
      selectQueryWithAssociations.where
        .in(primaryKeyField, ids.map(idToRawValue))
        .and(defaultScopeWithDefaultAlias)
    }).list.apply())
  }

  override def findAll(
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit s: DBSession = autoSession): List[Entity] = {
    implicit val repository = IncludesQueryRepository[Entity]()
    appendIncludedAttributes(extract(withSQL {
      val sql = selectQueryWithAssociations.where(defaultScopeWithDefaultAlias)
      if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
    }).list.apply())
  }

  override def findAllWithPagination(
    pagination: Pagination,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {
    if (hasManyAssociations.size > 0) {
      findAllWithLimitOffsetForOneToManyRelations(
        pagination.limit,
        pagination.offset,
        orderings
      )
    } else {
      findAllWithLimitOffset(pagination.limit, pagination.offset, orderings)
    }
  }

  override def findAllWithLimitOffset(
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {

    if (hasManyAssociations.size > 0) {
      findAllWithLimitOffsetForOneToManyRelations(limit, offset, orderings)
    } else {
      implicit val repository = IncludesQueryRepository[Entity]()
      appendIncludedAttributes(
        extract(
          withSQL {
            val sql =
              selectQueryWithAssociations.where(defaultScopeWithDefaultAlias)
            (if (orderings.isEmpty) sql
             else
               sql.orderBy(sqls.csv(orderings*))).limit(limit).offset(offset)
          }
        ).list.apply()
      )
    }
  }

  def findAllWithLimitOffsetForOneToManyRelations(
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {

    // find ids for pagination
    val ids: List[Any] = withSQL {
      if (orderings.isEmpty) singleSelectQuery.limit(limit).offset(offset)
      else {
        singleSelectQuery
          .orderBy(orderings.headOption.getOrElse(defaultOrdering))
          .limit(limit)
          .offset(offset)
      }
    }.map(_.any(defaultAlias.resultName.field(primaryKeyFieldName)))
      .list
      .apply()

    if (ids.isEmpty) {
      Nil
    } else {
      implicit val enableAsIs =
        ParameterBinderFactory.asisParameterBinderFactory
      implicit val repository = IncludesQueryRepository[Entity]()
      appendIncludedAttributes(extract(withSQL {
        val sql = selectQueryWithAssociations.where(
          sqls.toAndConditionOpt(
            defaultScopeWithDefaultAlias,
            Some(sqls.in(defaultAlias.field(primaryKeyFieldName), ids))
          )
        )
        if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
      }).list.apply())
    }
  }

  override def findBy(
    where: SQLSyntax
  )(implicit s: DBSession = autoSession): Option[Entity] = {
    implicit val repository = IncludesQueryRepository[Entity]()
    appendIncludedAttributes(extract(withSQL {
      selectQueryWithAssociations
        .where(
          sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
        )
    }).list.apply()).headOption
  }

  override def findAllBy(
    where: SQLSyntax,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {

    implicit val repository = IncludesQueryRepository[Entity]()
    appendIncludedAttributes(extract(withSQL {
      val sql = selectQueryWithAssociations
        .where(
          sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
        )
      if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
    }).list.apply())
  }

  override def findAllByWithLimitOffset(
    where: SQLSyntax,
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {

    if (hasManyAssociations.size > 0) {
      findAllByWithLimitOffsetForOneToManyRelations(
        where,
        limit,
        offset,
        orderings
      )
    } else {
      implicit val repository = IncludesQueryRepository[Entity]()
      appendIncludedAttributes(extract(withSQL {
        val sql = selectQueryWithAssociations
          .where(
            sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
          )
        if (orderings.isEmpty) sql.limit(limit).offset(offset)
        else sql.orderBy(sqls.csv(orderings*)).limit(limit).offset(offset)
      }).list.apply())
    }
  }

  def findAllByWithLimitOffsetForOneToManyRelations(
    where: SQLSyntax,
    limit: Int = 100,
    offset: Int = 0,
    orderings: Seq[SQLSyntax] = defaultOrderings
  )(implicit
    s: DBSession = autoSession
  ): List[Entity] = {

    // find ids for pagination
    val ids: Seq[Any] = withSQL {
      lazy val allowedForDistinctQuery: Seq[SQLSyntax] = {
        columns
          .map(column =>
            SQLSyntax
              .createUnsafely(s"${defaultAlias.tableAliasName}.${column}", Nil)
          )
          .toIndexedSeq
      }
      val baseQuery = {
        val columnsToFetch: Seq[SQLSyntax] =
          Seq(sqls"distinct ${defaultAlias.field(primaryKeyFieldName)}") ++ {
            // in this case, intentionally orderings are empty
            if (orderings.isEmpty) Seq.empty
            else
              orderingsForDistinctQuery(orderings, allowedForDistinctQuery).map(
                removeAscDesc
              )
          }
        selectQueryWithAdditionalAssociations(
          select(columnsToFetch*).from(as(defaultAlias)),
          belongsToAssociations ++ includedBelongsToAssociations,
          hasOneAssociations ++ includedHasOneAssociations,
          hasManyAssociations ++ includedHasManyAssociations.toSet
        )
      }
      val query = baseQuery.where(
        sqls.toAndConditionOpt(Some(where), defaultScopeWithDefaultAlias)
      )

      if (orderings.isEmpty) query.limit(limit).offset(offset)
      else
        query
          .orderBy(
            sqls.csv(
              orderingsForDistinctQuery(orderings, allowedForDistinctQuery)*
            )
          )
          .limit(limit)
          .offset(offset)

    }.map(_.any(1)).list.apply()

    if (ids.isEmpty) {
      Nil
    } else {
      implicit val enableAsIs =
        ParameterBinderFactory.asisParameterBinderFactory
      implicit val repository = IncludesQueryRepository[Entity]()
      appendIncludedAttributes(extract(withSQL {
        val sql = selectQueryWithAssociations
          .where(
            sqls.toAndConditionOpt(
              Option(where),
              defaultScopeWithDefaultAlias,
              Some(sqls.in(defaultAlias.field(primaryKeyFieldName), ids))
            )
          )
        if (orderings.isEmpty) sql else sql.orderBy(sqls.csv(orderings*))
      }).list.apply())
    }
  }

  private[this] def removeAscDesc(s: SQLSyntax): SQLSyntax = {
    SQLSyntax.createUnsafely(
      s.value
        .replaceFirst(" desc$", "")
        .replaceFirst(" asc$", "")
        .replaceFirst(" DESC$", "")
        .replaceFirst(" ASC$", ""),
      s.parameters
    )
  }

  private[this] def orderingsForDistinctQuery(
    orderings: Seq[SQLSyntax],
    allowedForDistinctQuery: Seq[SQLSyntax]
  ): Seq[SQLSyntax] = {
    orderings.filter { o =>
      allowedForDistinctQuery.exists(_.value == removeAscDesc(o).value)
    }
  }

}
