package scalikejdbc.orm

import scalikejdbc.orm.crud.CRUDFeatureWithId

/**
 * Out-of-the-box CRUD mapper.
 *
 * @tparam Id     id
 * @tparam Entity entity
 */
trait CRUDMapperWithId[Id, Entity]
  extends DataMapperWithId[Id, Entity]
  with CRUDFeatureWithId[Id, Entity]
