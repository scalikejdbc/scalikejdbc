package scalikejdbc.orm.optimisticlock

/**
  * Optimistic lock with timestamp.
  *
  * @tparam Entity entity
  */
trait OptimisticLockWithTimestampFeature[Entity]
  extends OptimisticLockWithTimestampFeatureWithId[Long, Entity]
