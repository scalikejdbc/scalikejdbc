package scalikejdbc

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Failure, Success }

/**
 * This type class enable users to customize the behavior of transaction boundary(commit/rollback).
 */
trait TxBoundary[A] {

  /**
   * Finishes the current transaction.
   */
  def finishTx(result: A, tx: Tx): A

  /**
   * Closes the current connection if needed.
   */
  def closeConnection(result: A, doClose: () => Unit): A = {
    doClose()
    result
  }

}

/**
 * TxBoundary type class instances.
 */
object TxBoundary {

  /**
   * Exception TxBoundary type class instance.
   */
  object Exception {

    implicit def exceptionTxBoundary[A] = new TxBoundary[A] {
      def finishTx(result: A, tx: Tx): A = {
        tx.commit()
        result
      }
    }
  }

  /** This class will tell library users about missing implicit value by compilation error with the explanatory method name. */
  private[scalikejdbc] sealed abstract class TxBoundaryMissingImplicits {
    implicit def `"!!! Please read the following error message shown as method name. !!!"`[A]: TxBoundary[A] = sys.error("Don't use this method.")
    implicit def `"To activate TxBoundary.Future, scala.concurrent.ExecutionContext value in implicit scope is required here."`[A]: TxBoundary[A] = sys.error("Don't use this method.")
  }

  /**
   * Future TxBoundary type class instance.
   */
  object Future extends TxBoundaryMissingImplicits {

    implicit def futureTxBoundary[A](implicit ec: ExecutionContext) = new TxBoundary[Future[A]] {
      def finishTx(result: Future[A], tx: Tx): Future[A] = {
        result.andThen {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
      }
      override def closeConnection(result: Future[A], doClose: () => Unit): Future[A] = {
        result.andThen {
          case _ => doClose()
        }
      }
    }
  }

  /**
   * Either TxBoundary type class instance.
   */
  object Either {

    implicit def eitherTxBoundary[L, R] = new TxBoundary[Either[L, R]] {
      def finishTx(result: Either[L, R], tx: Tx): Either[L, R] = {
        result match {
          case Right(_) => tx.commit()
          case Left(_) => tx.rollback()
        }
        result
      }
    }
  }

  /**
   * Try TxBoundary type class instance.
   */
  object Try {

    implicit def tryTxBoundary[A] = new TxBoundary[Try[A]] {
      def finishTx(result: Try[A], tx: Tx): Try[A] = {
        result match {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
        result
      }
    }
  }

}
