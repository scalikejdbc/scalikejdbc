package scalikejdbc.orm.logging

trait LoggerProvider {

  // The logger. Instantiated the first time it's used.
  private lazy val _logger = Logger(getClass)

  /**
   * Get the `Logger` for the class that mixes this trait in. The `Logger`
   * is created the first time this method is call. The other methods (e.g.,
   * `error`, `info`, etc.) call this method to get the logger.
   */
  protected def logger: Logger = _logger

  /**
   * Get the name associated with this logger.
   */
  protected def loggerName = logger.name

}
