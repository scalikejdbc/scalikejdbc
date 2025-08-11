package scalikejdbc

trait DBConnectionMethods { self: DBConnection =>

  def withReadOnlySession[A](execution: DBSession ?=> A): A =
    self.readOnly[A](session => execution(using session))

  def withAutoCommitSession[A](execution: DBSession ?=> A): A =
    self.autoCommit[A](session => execution(using session))

  def withLocalTxSession[A](
    execution: DBSession ?=> A
  )(using boundary: TxBoundary[A] = self.defaultTxBoundary[A]): A =
    self.localTx[A](session => execution(using session))(using boundary)

}
