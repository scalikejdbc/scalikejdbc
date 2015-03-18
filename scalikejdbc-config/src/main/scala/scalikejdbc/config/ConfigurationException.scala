package scalikejdbc.config

/**
 * Configuration Exception
 */
class ConfigurationException(val message: String) extends Exception(message) {
  def this(e: Throwable) = this(e.getMessage)
}
