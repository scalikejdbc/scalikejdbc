package scalikejdbc.orm.basic

// Don't change this import
import scalikejdbc._

/**
 * Provides AutoSession for this mapper.
 */
trait AutoSessionFeature {
  self: SQLSyntaxSupport[?] & ConnectionPoolFeature =>

  /**
   * AutoSession definition.
   */
  override def autoSession: DBSession = {
    connectionPoolName match {
      case ConnectionPool.DEFAULT_NAME =>
        Option(ThreadLocalDB.load()).map { threadLocalDB =>
          if (threadLocalDB.isTxNotActive) AutoSession
          else if (threadLocalDB.isTxAlreadyStarted)
            threadLocalDB.withinTxSession()
          else threadLocalDB.autoCommitSession()
        } getOrElse AutoSession
      case _ => NamedAutoSession(connectionPoolName)
    }
  }

}
