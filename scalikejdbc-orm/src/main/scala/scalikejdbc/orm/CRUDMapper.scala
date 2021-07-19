package scalikejdbc.orm

import scalikejdbc.orm.crud.CRUDFeatureWithId

/**
 * Out-of-the-box CRUD mapper.
 *
 * @tparam Entity entity
 */
trait CRUDMapper[Entity]
  extends DataMapper[Entity]
  with CRUDFeatureWithId[Long, Entity] {
  override def rawValueToId(value: Any): Long = value.toString.toLong

  override def idToRawValue(id: Long): Long = id
}
