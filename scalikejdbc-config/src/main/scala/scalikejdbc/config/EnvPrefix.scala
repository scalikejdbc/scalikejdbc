package scalikejdbc.config

/**
 * Env prefix for config reader
 */
trait EnvPrefix {
  val env: Option[String]
}
