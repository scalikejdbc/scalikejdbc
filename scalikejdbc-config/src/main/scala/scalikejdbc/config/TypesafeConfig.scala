package scalikejdbc.config

import com.typesafe.config.Config

/*
 * A Trait that holds configuration
 */
trait TypesafeConfig {
  def config: Config
}
