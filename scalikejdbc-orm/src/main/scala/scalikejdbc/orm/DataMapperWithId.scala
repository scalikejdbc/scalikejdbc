package scalikejdbc.orm

import scalikejdbc.orm.associations.AssociationsWithIdFeature
import scalikejdbc.orm.basic._
import scalikejdbc.orm.finder.FinderFeatureWithId
import scalikejdbc.orm.querying.QueryingFeatureWithId
import scalikejdbc.orm.strongparameters.StrongParametersFeature

/**
 * Basic DataMapper implementation.
 *
 * @tparam Id     id
 * @tparam Entity entity
 */
trait DataMapperWithId[Id, Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with IdFeature[Id]
  with AssociationsWithIdFeature[Id, Entity]
  with FinderFeatureWithId[Id, Entity]
  with QueryingFeatureWithId[Id, Entity]
  with DynamicTableNameFeatureWithId[Id, Entity]
  with StrongParametersFeature
