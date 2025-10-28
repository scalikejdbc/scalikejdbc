package scalikejdbc.orm.eagerloading

// Don't change this import
import scalikejdbc._
import scalikejdbc.orm.Alias
import scalikejdbc.orm.associations._
import scalikejdbc.orm.basic.{ IdFeature, SQLSyntaxSupportBase }
import scalikejdbc.orm.finder.FinderFeatureWithId
import scalikejdbc.orm.querying.QueryingFeatureWithId
import scalikejdbc.orm.exception.AssociationSettingsException
import scalikejdbc.orm.internals.JavaReflectionUtil

/**
  * Provides #includes APIs.
  */
trait IncludesFeatureWithId[Id, Entity]
  extends SQLSyntaxSupportBase[Entity]
  with IdFeature[Id]
  with AssociationsFeature[Entity]
  with JoinsFeature[Entity] {

  private[scalikejdbc] val includedBelongsToAssociations
    : Seq[BelongsToAssociation[Entity]] = Nil
  private[scalikejdbc] val includedHasOneAssociations
    : Seq[HasOneAssociation[Entity]] = Nil
  private[scalikejdbc] val includedHasManyAssociations
    : Seq[HasManyAssociation[Entity]] = Nil

  /**
    * Adds includes conditions.
    *
    * @param associations associations
    * @return extended self
    */
  def includes(
    associations: Association[?]*
  ): IncludesFeatureWithId[Id, Entity]
    with FinderFeatureWithId[Id, Entity]
    with QueryingFeatureWithId[Id, Entity] = {
    val _self = this
    val _associations = associations
    val _belongsTo = associations
      .filter(_.isInstanceOf[BelongsToAssociation[Entity]])
      .map(_.asInstanceOf[BelongsToAssociation[Entity]])
    val _hasOne =
      associations
        .filter(_.isInstanceOf[HasOneAssociation[Entity]])
        .map(_.asInstanceOf[HasOneAssociation[Entity]])
    val _hasMany =
      associations
        .filter(_.isInstanceOf[HasManyAssociation[Entity]])
        .map(_.asInstanceOf[HasManyAssociation[Entity]])

    // creates new instance but ideally this should be more DRY & safe implementation
    new IncludesFeatureWithId[Id, Entity]
      with FinderFeatureWithId[Id, Entity]
      with QueryingFeatureWithId[Id, Entity] {
      override protected val underlying: SQLSyntaxSupportBase[Entity] = _self
      override def defaultAlias = _self.defaultAlias

      override def tableName = _self.tableName
      override def columnNames = _self.columnNames

      override def primaryKeyField = _self.primaryKeyField
      override def primaryKeyFieldName = _self.primaryKeyFieldName

      override def rawValueToId(value: Any) = _self.rawValueToId(value)
      override def idToRawValue(id: Id): Any = _self.idToRawValue(id)

      override private[scalikejdbc] val belongsToAssociations =
        _self.belongsToAssociations ++ _belongsTo
      override private[scalikejdbc] val hasOneAssociations =
        _self.hasOneAssociations ++ _hasOne
      override private[scalikejdbc] val hasManyAssociations =
        _self.hasManyAssociations ++ _hasMany

      override private[scalikejdbc] val includedBelongsToAssociations =
        _self.includedBelongsToAssociations ++ _belongsTo
      override private[scalikejdbc] val includedHasOneAssociations =
        _self.includedHasOneAssociations ++ _hasOne
      override private[scalikejdbc] val includedHasManyAssociations =
        _self.includedHasManyAssociations ++ _hasMany

      override val associations = _self.associations ++ _associations
      override val defaultJoinDefinitions = _self.defaultJoinDefinitions
      override val defaultBelongsToExtractors = _self.defaultBelongsToExtractors
      override val defaultHasOneExtractors = _self.defaultHasOneExtractors
      override val defaultOneToManyExtractors = _self.defaultOneToManyExtractors

      override def autoSession = underlying.autoSession
      override def connectionPoolName: Any = underlying.connectionPoolName
      override def connectionPool = underlying.connectionPool

      override def defaultScope(alias: Alias[Entity]) =
        _self.defaultScope(alias)
      // override def singleSelectQuery = _self.singleSelectQuery

      def extract(rs: WrappedResultSet, n: ResultName[Entity]) =
        underlying.extract(rs, n)
    }
  }

  /**
    * Returns ids from entities.
    *
    * @param entities entities
    * @param primaryKeyFieldName primary key name
    * @return ids
    */
  private[this] def toIds(
    entities: Seq[Any],
    primaryKeyFieldName: String
  ): Seq[Id] = {
    entities.flatMap { e =>
      JavaReflectionUtil.getter(e, primaryKeyFieldName) match {
        case null    => None
        case None    => None
        case Some(v) =>
          try Some(v.asInstanceOf[Id])
          catch {
            case e: ClassCastException =>
              throw new IllegalStateException(
                s"Casting ${v.getClass.getCanonicalName} value to expected identity type is failed."
              )
          }
        case v =>
          try Some(v.asInstanceOf[Id])
          catch {
            case e: ClassCastException =>
              throw new IllegalStateException(
                s"Casting ${v.getClass.getCanonicalName} value to expected identity type is failed."
              )
          }
      }
    }
  }

  /**
    * Applies includes operations to query results.
    *
    * @param entities entities
    * @param s session
    * @param repository repository
    * @return entities with included attributes
    */
  def appendIncludedAttributes(entities: List[Entity])(implicit
    s: DBSession,
    repository: IncludesQueryRepository[Entity]
  ): List[Entity] = {
    def toFinder(
      mapper: AssociationsFeature[?]
    ): FinderFeatureWithId[Id, Entity] = {
      mapper.asInstanceOf[FinderFeatureWithId[Id, Entity]]
    }
    try {
      val withBelongsTo = includedBelongsToAssociations.foldLeft(entities) {
        (entities, assoc) =>
          val ids: Seq[Id] = toIds(
            repository.entitiesFor(assoc.extractor),
            assoc.extractor.mapper.primaryKeyFieldName
          )
          if (ids.isEmpty) entities
          else {
            assoc.extractor
              .includesMerge(
                entities,
                toFinder(assoc.extractor.mapper).findAllByIds(ids*)
              )
              .toList
          }
      }
      val withHasOne = includedHasOneAssociations.foldLeft(withBelongsTo) {
        (entities, assoc) =>
          val ids = toIds(
            repository.entitiesFor(assoc.extractor),
            assoc.extractor.mapper.primaryKeyFieldName
          )
          if (ids.isEmpty) entities
          else {
            assoc.extractor
              .includesMerge(
                entities,
                toFinder(assoc.extractor.mapper).findAllByIds(ids*)
              )
              .toList
          }
      }
      includedHasManyAssociations.foldLeft(withHasOne) { (entities, assoc) =>
        val ids = toIds(
          repository.entitiesFor(assoc.extractor),
          assoc.extractor.mapper.primaryKeyFieldName
        )
        if (ids.isEmpty) entities
        else {
          assoc.extractor
            .includesMerge(
              entities,
              toFinder(assoc.extractor.mapper).findAllByIds(ids*)
            )
            .toList
        }
      }

    } catch {
      case e: ClassCastException =>
        throw new AssociationSettingsException(
          s"Failed to execute an includes query because ${e.getMessage}!"
        )
    }
  }

  override def selectQueryWithAssociations: SelectSQLBuilder[Entity] = {
    selectQueryWithAdditionalAssociations(
      defaultSelectQuery,
      belongsToAssociations ++ includedBelongsToAssociations,
      hasOneAssociations ++ includedHasOneAssociations,
      hasManyAssociations ++ includedHasManyAssociations.toSet
    )
  }

  /**
    * Applies includes operations to query result.
    *
    * @param entity entity
    * @param s session
    * @param repository repository
    * @return entity with included attributes
    */
  def appendIncludedAttributes(entity: Option[Entity])(implicit
    s: DBSession,
    repository: IncludesQueryRepository[Entity]
  ): Option[Entity] = {
    appendIncludedAttributes(entity.toList).headOption
  }

}
