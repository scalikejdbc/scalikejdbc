package scalikejdbc.orm.crud

/**
 * Provides auto-generated CRUD feature.
 *
 * @tparam Entity entity
 */
trait CRUDFeature[Entity] extends CRUDFeatureWithId[Long, Entity]
