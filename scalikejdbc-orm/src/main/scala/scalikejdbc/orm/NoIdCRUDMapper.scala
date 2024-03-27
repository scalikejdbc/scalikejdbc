package scalikejdbc.orm

import scalikejdbc.orm.crud.NoIdCUDFeature

/**
 * CRUD mapper for tables that don't have single primary key.
 *
 * @tparam Entity entity
 */
trait NoIdCRUDMapper[Entity]
  extends NoIdDataMapper[Entity]
  with NoIdCUDFeature[Entity]
