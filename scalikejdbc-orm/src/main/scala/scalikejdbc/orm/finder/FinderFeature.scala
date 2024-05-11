package scalikejdbc.orm.finder

/**
  * Provides #find something APIs.
  */
trait FinderFeature[Entity] extends FinderFeatureWithId[Long, Entity]
