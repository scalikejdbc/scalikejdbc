package scalikejdbc

/**
 * Exception which represents that an illegal relationship is found.
 */
case class IllegalRelationshipException(message: String)
  extends IllegalStateException(message)
