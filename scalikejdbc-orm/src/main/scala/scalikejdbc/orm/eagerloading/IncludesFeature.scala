package scalikejdbc.orm.eagerloading

/**
  * Provides #includes APIs.
  */
trait IncludesFeature[Entity] extends IncludesFeatureWithId[Long, Entity]
