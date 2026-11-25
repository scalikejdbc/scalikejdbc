package scalikejdbc.orm

package object timstamps {

  @deprecated("use scalikejdbc.orm.timestamps.NoIdTimestampsFeature instead")
  type NoIdTimestampsFeature[Entity] =
    scalikejdbc.orm.timestamps.NoIdTimestampsFeature[Entity]

  @deprecated("use scalikejdbc.orm.timestamps.TimestampsFeature instead")
  type TimestampsFeature[Entity] =
    scalikejdbc.orm.timestamps.TimestampsFeature[Entity]

  @deprecated("use scalikejdbc.orm.timestamps.TimestampsFeatureBase instead")
  type TimestampsFeatureBase[Entity] =
    scalikejdbc.orm.timestamps.TimestampsFeatureBase[Entity]

  @deprecated("use scalikejdbc.orm.timestamps.TimestampsFeatureWithId instead")
  type TimestampsFeatureWithId[Id, Entity] =
    scalikejdbc.orm.timestamps.TimestampsFeatureWithId[Id, Entity]
}
