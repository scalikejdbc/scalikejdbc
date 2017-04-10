package scalikejdbc

import scala.language.reflectiveCalls
import util.control.Exception._
import scala.concurrent.{ ExecutionContext, Future }

object LoanPattern extends LoanPattern

/**
 * Loan pattern implementation
 */
trait LoanPattern {

  type Closable = { def close(): Unit }

  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    try {
      f(resource)
    } finally {
      ignoring(classOf[Throwable]) apply {
        resource.close()
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
