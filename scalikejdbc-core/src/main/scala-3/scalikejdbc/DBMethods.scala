package scalikejdbc

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

trait DBMethods { self: DB.type =>
  def withReadOnlySession[A](execution: DBSession ?=> A)(using
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = self.readOnly[A](session => execution(using session))(using
    context,
    settings
  )

  def withAutoCommitSession[A](execution: DBSession ?=> A)(using
    context: CPContext = NoCPContext,
    settings: SettingsProvider = SettingsProvider.default
  ): A = self.autoCommit[A](session => execution(using session))(using
    context,
    settings
  )

  def withLocalTxSession[A](execution: DBSession ?=> A)(using
    context: CPContext = NoCPContext,
    boundary: TxBoundary[A] = DB.defaultTxBoundary[A],
    settings: SettingsProvider = SettingsProvider.default
  ): A = self.localTx[A](session => execution(using session))(using
    context,
    boundary,
    settings
  )

  def withFutureLocalTxSession[A](execution: DBSession ?=> Future[A])(using
    context: CPContext = NoCPContext,
    ec: ExecutionContext,
    settings: SettingsProvider = SettingsProvider.default
  ): Future[A] =
    self.futureLocalTx[A](session => execution(using session))(using
      context,
      ec,
      settings
    )
}
