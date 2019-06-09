package scalikejdbc

import scalikejdbc.LoanPattern.Closable

import scala.concurrent.{ ExecutionContext, Future }

trait OnFinish[A] {
  def using[R <: Closable](resource: R)(f: R => A): A
}

object OnFinish extends LowPriorityImplicitsOnFinish {
  object Future {
    implicit def futureOnFinish[A](implicit ec: ExecutionContext): OnFinish[Future[A]] = new OnFinish[Future[A]] {
      def using[R <: Closable](resource: R)(f: R => Future[A]): Future[A] = LoanPattern.futureUsing(resource)(f)
    }
  }
}

sealed trait LowPriorityImplicitsOnFinish {
  implicit def onFuture[A]: OnFinish[A] = new OnFinish[A] {
    def using[R <: Closable](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)
  }
}
