package scalikejdbc.orm

// Don't change this import
import scalikejdbc._

import scalikejdbc.orm.associations.AssociationsFeature
import scalikejdbc.orm.basic.SQLSyntaxSupportBase
import scalikejdbc.orm.crud.NoIdCUDFeature
import scalikejdbc.orm.finder.NoIdFinderFeature
import scalikejdbc.orm.querying.NoIdQueryingFeature

/**
 * DataMapper which represents join table which is used for associations.
 *
 * This mapper don't have primary key search and so on because they cannot work as expected or no need to implement.
 *
 * @tparam Entity entity
 */
trait JoinTable[Entity]
  extends SQLSyntaxSupportBase[Entity]
  with AssociationsFeature[Entity]
  with NoIdCUDFeature[Entity]
  with NoIdQueryingFeature[Entity]
  with NoIdFinderFeature[Entity] {

  override def extract(rs: WrappedResultSet, s: ResultName[Entity]): Entity = {
    throw new IllegalStateException(
      "You must implement this method if ResultSet extraction is needed."
    )
  }

}
