/*
 * Copyright 2011 - 2015 scalikejdbc.org
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

import scala.language.reflectiveCalls
import util.control.Exception._
import scala.concurrent.{ ExecutionContext, Future }

object LoanPattern extends LoanPattern

/**
 * Loan pattern implementation
 */
trait LoanPattern {

  type Closable = { def close() }

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
