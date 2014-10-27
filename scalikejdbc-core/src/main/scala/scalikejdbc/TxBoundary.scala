/*
 * Copyright 2014 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Try, Failure, Success }

/**
 * This type class enable users to customize the befavior of transaction boundary(commit/rollback).
 */
trait TxBoundary[A] {

  def finishTx(result: A, tx: Tx): A

  def closeConnection(result: A, doClose: () => Unit): A = {
    doClose()
    result
  }

}

/**
 * Default TxBoundary type class instances.
 * NOTE:  TxBoundary usage will be disclosed to library users since 2.2 or later.
 */
object TxBoundary {

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

  /**
   * Default TxBoundary type class instances.
   * This implicit won't be provided by default
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
   * Default TxBoundary type class instances.
   * This implicit won't be provided by default
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
