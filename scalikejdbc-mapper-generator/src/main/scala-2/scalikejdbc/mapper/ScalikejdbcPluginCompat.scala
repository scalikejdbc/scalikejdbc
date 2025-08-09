package scalikejdbc.mapper

private[mapper] object ScalikejdbcPluginCompat {
  implicit class DefOps(private val self: sbt.Def.type) extends AnyVal {
    def uncached[A](a: A): A = a
  }
}
