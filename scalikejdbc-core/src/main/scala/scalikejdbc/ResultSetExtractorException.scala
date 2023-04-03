package scalikejdbc

/**
 * Exception which represents failure on ResultSet extraction.
 */
case class ResultSetExtractorException(
  message: String,
  e: Option[Exception] = None
) extends IllegalArgumentException(message, e.orNull[Exception])
