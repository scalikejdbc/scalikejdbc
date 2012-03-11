package object scalikejdbc {

  import scalikejdbc.LoanPattern._

  type Closable = { def close() }

  def using[R <: Closable, A](resource: R)(f: R => A): A = LoanPattern.using(resource)(f)

}

