package scalikejdbc.orm.querying

/**
  * Querying APIs feature.
  */
trait QueryingFeature[Entity] extends QueryingFeatureWithId[Long, Entity] {

  override def rawValueToId(value: Any) = value.toString.toLong
  override def idToRawValue(id: Long): Any = id
}
