package scalikejdbc.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/*
 * A Trait that follows the standard behavior of typesafe-config.
 */
trait StandardTypesafeConfig extends TypesafeConfig {

  lazy val config: Config = ConfigFactory.load()
}
