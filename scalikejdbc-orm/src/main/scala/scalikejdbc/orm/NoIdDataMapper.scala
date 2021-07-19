package scalikejdbc.orm

import scalikejdbc.orm.associations.{
  AssociationsFeature,
  HasOneAssociation,
  NoIdAssociationsFeature
}
import scalikejdbc.orm.basic.{
  AutoSessionFeature,
  ConnectionPoolFeature,
  SQLSyntaxSupportBase
}
import scalikejdbc.orm.finder.NoIdFinderFeature
import scalikejdbc.orm.querying.NoIdQueryingFeature
import scalikejdbc.orm.strongparameters.StrongParametersFeature
import scalikejdbc.orm.exception.IllegalAssociationException

/**
 * Basic mapper for tables that don't have single primary key.
 *
 * @tparam Entity entity
 */
trait NoIdDataMapper[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with ConnectionPoolFeature
  with AutoSessionFeature
  with NoIdFinderFeature[Entity]
  with NoIdQueryingFeature[Entity]
  with NoIdAssociationsFeature[Entity]
  with StrongParametersFeature {

  override def primaryKeyFieldName: String =
    throw new IllegalStateException("Unexpected access to primaryKeyFieldName")

  override def hasOne[A](
    right: AssociationsFeature[A],
    merge: (Entity, Option[A]) => Entity
  ): HasOneAssociation[Entity] = {

    throw new IllegalAssociationException(
      s"NoIdDataMapper doesn't support `hasOne` relationship through single primary key (e.g. id)."
    )
  }

}
