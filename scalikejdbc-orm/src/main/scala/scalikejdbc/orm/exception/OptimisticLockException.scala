package scalikejdbc.orm.exception

/**
  * Represents optimistic locking conflict.
  *
  * @param message message
  * @param cause cause
  */
case class OptimisticLockException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)
