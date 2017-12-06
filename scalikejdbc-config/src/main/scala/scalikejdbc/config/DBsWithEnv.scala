package scalikejdbc.config

/**
 * DB setup executor with env prefix
 */
case class DBsWithEnv(envValue: String) extends DBs
  with TypesafeConfigReader
  with StandardTypesafeConfig
  with EnvPrefix {

  override val env = Option(envValue)
}

