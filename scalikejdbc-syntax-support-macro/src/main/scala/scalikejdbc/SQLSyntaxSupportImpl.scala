package scalikejdbc

trait SQLSyntaxSupportImpl[A] extends SQLSyntaxSupport[A] {

  def apply(rn: ResultName[A])(rs: WrappedResultSet): A
  def apply(s: SyntaxProvider[A])(rs: WrappedResultSet): A =
    apply(s.resultName)(rs)

}
