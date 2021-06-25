package scalikejdbc.orm.basic

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.Alias
import scalikejdbc.orm.finder.FinderFeatureWithId
import scalikejdbc.orm.querying.QueryingFeatureWithId

/**
  * Provides #withTableNmae APIs.
  */
trait DynamicTableNameFeature[Entity]
  extends DynamicTableNameFeatureWithId[Long, Entity] {
  self: SQLSyntaxSupportBase[Entity] with IdFeature[Long] =>

}

trait DynamicTableNameFeatureWithId[Id, Entity] {
  self: SQLSyntaxSupportBase[Entity] with IdFeature[Id] =>

  /**
    * Appends join definition on runtime.
    *
    * @param tableName table name
    * @return self
    */
  def withTableName(
    tableName: String
  ): DynamicTableNameFeatureWithId[Id, Entity]
    with FinderFeatureWithId[Id, Entity]
    with QueryingFeatureWithId[
      Id,
      Entity
    ] = {
    val _self = this
    val dynamicTableName = tableName

    new SQLSyntaxSupportBase[Entity]
      with IdFeature[Id]
      with DynamicTableNameFeatureWithId[Id, Entity]
      with FinderFeatureWithId[Id, Entity]
      with QueryingFeatureWithId[Id, Entity] {
      override def defaultAlias = _self.defaultAlias
      override val tableName = dynamicTableName
      override def columnNames = _self.columnNames

      override def primaryKeyField = _self.primaryKeyField
      override def primaryKeyFieldName = _self.primaryKeyFieldName

      override def rawValueToId(value: Any) = _self.rawValueToId(value)
      override def idToRawValue(id: Id) = _self.idToRawValue(id)

      override def defaultScope(alias: Alias[Entity]) =
        _self.defaultScope(alias)
      // override def singleSelectQuery = _self.singleSelectQuery

      def extract(rs: WrappedResultSet, n: ResultName[Entity]) =
        _self.extract(rs, n)
    }
  }

}
