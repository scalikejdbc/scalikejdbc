package scalikejdbc.orm.softdeletion

/**
  * Soft delete with timestamp value.
  *
  * @tparam Entity entity
  */
trait SoftDeleteWithTimestampFeature[Entity]
  extends SoftDeleteWithTimestampFeatureWithId[Long, Entity]
