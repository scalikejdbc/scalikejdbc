package scalikejdbc.orm

/**
 * Basic DataMapper implementation.
 *
 * @tparam Entity entity
 */
trait DataMapper[Entity] extends DataMapperWithId[Long, Entity] {
  override def rawValueToId(value: Any) = value.toString.toLong

  override def idToRawValue(id: Long): Any = id
}
