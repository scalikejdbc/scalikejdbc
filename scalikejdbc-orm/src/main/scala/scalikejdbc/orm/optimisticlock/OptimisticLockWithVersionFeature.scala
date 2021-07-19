package scalikejdbc.orm.optimisticlock

/**
  * Optimistic lock with version.
  *
  * @tparam Entity entity
  */
trait OptimisticLockWithVersionFeature[Entity]
  extends OptimisticLockWithVersionFeatureWithId[Long, Entity]
