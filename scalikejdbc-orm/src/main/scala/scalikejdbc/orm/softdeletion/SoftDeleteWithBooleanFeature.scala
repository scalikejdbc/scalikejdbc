package scalikejdbc.orm.softdeletion

/**
  * Soft delete with boolean value.
  *
  * @tparam Entity entity
  */
trait SoftDeleteWithBooleanFeature[Entity]
  extends SoftDeleteWithBooleanFeatureWithId[Long, Entity]
