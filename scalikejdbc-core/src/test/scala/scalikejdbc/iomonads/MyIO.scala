package scalikejdbc.iomonads

import scalikejdbc.{ Tx, TxBoundary }

sealed abstract class MyIO[+A] {
  import MyIO._

  def flatMap[B](f: A => MyIO[B]): MyIO[B] = {
    this match {
      case Delay(thunk) => Delay(() => f(thunk()).run())
    }
  }

  def map[B](f: A => B): MyIO[B] = flatMap(x => MyIO(f(x)))

  def run(): A = {
    this match {
      case Delay(f) => f.apply()
    }
  }

  def attempt: MyIO[Either[Throwable, A]] =
    MyIO(try {
      Right(run())
    } catch {
      case scala.util.control.NonFatal(t) => Left(t)
    })

}

object MyIO {
  def apply[A](a: => A): MyIO[A] = Delay(() => a)

  final case class Delay[+A](thunk: () => A) extends MyIO[A]

  implicit def myIOTxBoundary[A]: TxBoundary[MyIO[A]] =
    new TxBoundary[MyIO[A]] {

      def finishTx(result: MyIO[A], tx: Tx): MyIO[A] = {
        result.attempt.flatMap {
          case Right(a) => MyIO(tx.commit()).flatMap(_ => MyIO(a))
          case Left(e)  => MyIO(tx.rollback()).flatMap(_ => MyIO(throw e))
        }
      }

      override def closeConnection(
        result: MyIO[A],
        doClose: () => Unit
      ): MyIO[A] = {
        for {
          x <- result.attempt
          _ <- MyIO(doClose).map(x => x.apply())
          a <- MyIO(x.fold(throw _, identity))
        } yield a
      }
    }
}
