package scalikejdbc.orm.exception

/**
  * Represents illegal association definitions (which won't be detected by compilation).
  *
  * @param message message
  * @param cause cause
  */
case class IllegalAssociationException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)
