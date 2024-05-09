package scalikejdbc.orm.associations

import scalikejdbc.orm.Alias
import scalikejdbc.orm.basic.SQLSyntaxSupportBase
import scalikejdbc.orm.eagerloading.IncludesQueryRepository
import scalikejdbc.orm.finder.NoIdFinderFeature
import scalikejdbc.orm.querying.NoIdQueryingFeature
import scalikejdbc.{
  HasExtractor,
  NoExtractor,
  ResultName,
  SQL,
  WrappedResultSet
}

/**
 * Provides #joins APIs.
 *
 * NOTE: CRUDFeature has copy implementation from this trait.
 */
trait NoIdJoinsFeature[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with AssociationsFeature[Entity] {

  /**
   * Appends join definition on runtime.
   *
   * @param associations associations
   * @return self
   */
  def joins(
    associations: Association[?]*
  ): NoIdJoinsFeature[Entity]
    with NoIdFinderFeature[Entity]
    with NoIdQueryingFeature[Entity] = {
    val _self = this
    val _associations = associations

    new NoIdJoinsFeature[Entity]
      with NoIdFinderFeature[Entity]
      with NoIdQueryingFeature[Entity] {
      override protected val underlying: SQLSyntaxSupportBase[Entity] = _self

      override def defaultAlias = _self.defaultAlias

      override def tableName = _self.tableName

      override def columnNames = _self.columnNames

      override def primaryKeyField = _self.primaryKeyField

      override def primaryKeyFieldName = _self.primaryKeyFieldName

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

}
