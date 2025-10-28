package scalikejdbc

import scala.concurrent.{ Promise, ExecutionContext, Future }
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

    implicit def exceptionTxBoundary[A]: TxBoundary[A] = (result: A, tx: Tx) =>
      {
        tx.commit()
        result
      }
  }

  /** This class will tell library users about missing implicit value by compilation error with the explanatory method name. */
  private[scalikejdbc] sealed abstract class TxBoundaryMissingImplicits {
    implicit def `"!!! Please read the following error message shown as method name. !!!"`[
      A
    ]: TxBoundary[A] = sys.error("Don't use this method.")
    implicit def `"To activate TxBoundary.Future, scala.concurrent.ExecutionContext value in implicit scope is required here."`[
      A
    ]: TxBoundary[A] = sys.error("Don't use this method.")
  }

  /**
   * Applies an operation to finish current transaction to the result.
   * When the operation throws some exception, the exception will be returned without fail.
   */
  private def doFinishTx[A](result: Try[A])(doFinish: Try[A] => Unit): Try[A] =
    scala.util
      .Try(doFinish(result))
      .transform(
        _ => result,
        finishError =>
          Failure(result match {
            case Success(_)           => finishError
            case Failure(resultError) =>
              resultError.addSuppressed(finishError)
              resultError
          })
      )

  /**
   * Applies an operation to finish current transaction to the Future value which holds the result.
   * When the operation throws some exception, the exception will be returned without fail.
   */
  private def onFinishTx[A](
    resultF: Future[A]
  )(doFinish: Try[A] => Unit)(implicit ec: ExecutionContext): Future[A] = {
    val p = Promise[A]()
    resultF.onComplete(result => p.complete(doFinishTx(result)(doFinish)))
    p.future
  }

  /**
   * Future TxBoundary type class instance.
   */
  object Future extends TxBoundaryMissingImplicits {

    implicit def futureTxBoundary[A](implicit
      ec: ExecutionContext
    ): TxBoundary[Future[A]] = new TxBoundary[Future[A]] {

      def finishTx(result: Future[A], tx: Tx): Future[A] = {
        onFinishTx(result) {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
      }

      override def closeConnection(
        result: Future[A],
        doClose: () => Unit
      ): Future[A] =
        onFinishTx(result)(_ => doClose())
    }
  }

  /**
   * Either TxBoundary type class instance.
   *
   * NOTE: Either TxBoundary may throw an Exception when commit/rollback operation fails.
   */
  object Either {

    implicit def eitherTxBoundary[L, R]: TxBoundary[Either[L, R]] =
      (result: Either[L, R], tx: Tx) => {
        result match {
          case Right(_) => tx.commit()
          case Left(_)  => tx.rollback()
        }
        result
      }
  }

  /**
   * Try TxBoundary type class instance.
   */
  object Try {

    implicit def tryTxBoundary[A]: TxBoundary[Try[A]] = new TxBoundary[Try[A]] {
      def finishTx(result: Try[A], tx: Tx): Try[A] = {
        doFinishTx(result) {
          case Success(_) => tx.commit()
          case Failure(_) => tx.rollback()
        }
      }

      override def closeConnection(
        result: Try[A],
        doClose: () => Unit
      ): Try[A] = {
        doFinishTx(result)(_ => doClose())
      }
    }
  }

}
