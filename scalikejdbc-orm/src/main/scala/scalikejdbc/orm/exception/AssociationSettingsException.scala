package scalikejdbc.orm.exception

/**
  * Represents settings failures of associations.
  *
  * @param message message
  * @param cause cause
  */
case class AssociationSettingsException(
  message: String,
  cause: Throwable = null
) extends IllegalStateException(message, cause)
