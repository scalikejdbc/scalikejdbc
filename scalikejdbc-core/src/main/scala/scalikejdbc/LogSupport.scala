package scalikejdbc

import org.slf4j.LoggerFactory

/**
 * Simple [[scalikejdbc.Log]] adaptor.
 */
private[scalikejdbc] trait LogSupport {

  /**
   * Logger
   */
  protected val log = new Log(LoggerFactory.getLogger(this.getClass))

}
