/*
 * Copyright 2011 Kazuhiro Sera
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
import scala.language.reflectiveCalls
import scala.util.control.Exception._

object LoanPattern extends LoanPattern

/**
 * Loan pattern implementation
 */
trait LoanPattern {

  type Closable = { def close() }

  private def forceClose(resource: Closable) = ignoring(classOf[Throwable]) apply {
    resource.close()
  }

  def using[R <: Closable, A](resource: R)(f: R => A): A = {
    var closeInFuture = false
    try {
      val a = f(resource)
      a match {
        case fut: Future[_] =>
          closeInFuture = true
          fut.andThen { case _ => forceClose(resource) }(ExecutionContext.global).asInstanceOf[A]
        case _ =>
          a
      }
    } finally {
      if (!closeInFuture) {
        forceClose(resource)
      }
    }
  }

  /**
   * Guarantees a Closeable resource will be closed after being passed to a block that takes
   * the resource as a parameter and returns a Future.
   */
  @deprecated("simply use #using")
  def futureUsing[R <: Closable, A](resource: R)(f: R => Future[A])(implicit ec: ExecutionContext): Future[A] = {
    f(resource) andThen { case _ => forceClose(resource) } // close no matter what
  }

}
