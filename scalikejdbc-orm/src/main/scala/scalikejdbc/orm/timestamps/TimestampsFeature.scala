package scalikejdbc.orm.timestamps

/**
  * ActiveRecord timestamps feature.
  *
  * @tparam Entity entity
  */
trait TimestampsFeature[Entity] extends TimestampsFeatureWithId[Long, Entity]
