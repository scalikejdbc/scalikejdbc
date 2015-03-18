package scalikejdbc.config

/**
 * No Env prefix for config reader
 */
trait NoEnvPrefix extends EnvPrefix {

  override val env: Option[String] = None
}
