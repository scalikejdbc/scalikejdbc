package scalikejdbc

import org.slf4j.LoggerFactory

import scala.language.reflectiveCalls
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

object LoanPattern extends LoanPattern

/**
 * Loan pattern implementation
 */
trait LoanPattern {

  private[scalikejdbc] val loanPatternLogger = LoggerFactory.getLogger(classOf[LoanPattern])

  type Closable = { def close(): Unit }

  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      try {
        resource.close()
      } catch {
        case NonFatal(e) =>
          loanPatternLogger.warn(s"Failed to close a resource (resource: ${resource.getClass().getName()} error: ${e.getMessage})")
      }
    }
  }

  /**
   * Guarantees a Closeable resource will be closed after being passed to a block that takes
   * the resource as a parameter and returns a Future.
   */
  def futureUsing[R <: Closable, A](resource: R)(f: R => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    f(resource) andThen { case _ => resource.close() } // close no matter what
  }

}
