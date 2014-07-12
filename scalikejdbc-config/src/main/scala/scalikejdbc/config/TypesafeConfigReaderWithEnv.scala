/*
 * Copyright 2013 - 2014 scalikejdbc.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package scalikejdbc.config

import com.typesafe.config.{ ConfigFactory, Config }

/**
 * Typesafe config reader with env prefix.
 */
case class TypesafeConfigReaderWithEnv(envValue: String)
    extends TypesafeConfigReader
    with StandardTypesafeConfig
    with EnvPrefix {

  override val env = Option(envValue)

  override lazy val config: Config = {
    val topLevelConfig = ConfigFactory.load()
    topLevelConfig.getConfig(envValue).withFallback(topLevelConfig)
  }
}
