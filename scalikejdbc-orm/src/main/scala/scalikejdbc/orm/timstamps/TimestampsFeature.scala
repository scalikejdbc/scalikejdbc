package scalikejdbc.orm.timstamps

/**
  * ActiveRecord timestamps feature.
  *
  * @tparam Entity entity
  */
trait TimestampsFeature[Entity] extends TimestampsFeatureWithId[Long, Entity]
