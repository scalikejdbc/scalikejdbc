package scalikejdbc.orm.exception

/**
  * Represents DB settings issue.
  *
  * @param message message
  * @param cause cause
  */
case class DBSettingsException(message: String, cause: Throwable = null)
  extends RuntimeException(message, cause)
