package scalikejdbc.orm.settings

import scalikejdbc.config.{
  DBs,
  NoEnvPrefix,
  TypesafeConfig,
  TypesafeConfigReader
}

/**
 * DB setup executor with default settings
 */
case class DBsWithEnv(envValue: String)
  extends DBs
  with TypesafeConfigReader
  with TypesafeConfig
  with NoEnvPrefix {

  // Replacing Config because (at least) ScalikeJDBC 2.0.4 or lower versions doesn't support default values
  override val config = ORMTypesafeConfigReader.config(envValue)

}
