package scalikejdbc.orm.associations

/**
 * Associations for NoId mappers.
 */
trait NoIdAssociationsFeature[Entity]
  extends AssociationsFeature[Entity]
  with NoIdJoinsFeature[Entity]
