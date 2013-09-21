/*
 * Copyright 2013 Toshiyuki Takahashi, Kazuhiro Sera
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

import scalikejdbc._
import com.typesafe.config.{ Config, ConfigFactory, ConfigException }
import scala.collection.mutable.{ Map => MutableMap }
import scala.collection.JavaConverters._

/**
 * Configuration Exception
 */
class ConfigurationException(val message: String) extends Exception(message) {
  def this(e: Throwable) = this(e.getMessage)
}

/*
 * A Trait that holds configuration
 */
trait TypesafeConfig {
  val config: Config
}

/*
 * A Trait that follows the standard behavior of typesafe-config.
 */
trait StandardTypesafeConfig extends TypesafeConfig {
  lazy val config: Config = ConfigFactory.load()
}

/**
 * Typesafe TypesafeConfig reader
 */
trait TypesafeConfigReader extends NoEnvPrefix { self: TypesafeConfig =>

  def envPrefix: String = env.map(_ + ".").getOrElse("")

  lazy val dbNames: List[String] = {
    if (config.hasPath(envPrefix + "db")) {
      config.getConfig(envPrefix + "db").root.keySet.asScala.toList
    } else {
      Nil
    }
  }

  def readAsMap(dbName: Symbol = ConnectionPool.DEFAULT_NAME): Map[String, String] = try {
    val dbConfig = config.getConfig(envPrefix + "db." + dbName.name)
    val iter = dbConfig.entrySet.iterator
    val configMap: MutableMap[String, String] = MutableMap.empty
    while (iter.hasNext) {
      val entry = iter.next()
      val key = entry.getKey
      configMap(key) = config.getString(envPrefix + "db." + dbName.name + "." + key)
    }
    configMap.toMap
  } catch {
    case e: ConfigException => throw new ConfigurationException(e)
  }

  def readJDBCSettings(dbName: Symbol = ConnectionPool.DEFAULT_NAME): JDBCSettings = {
    val configMap = self.readAsMap(dbName)
    (for {
      driver <- configMap.get("driver")
      url <- configMap.get("url")
    } yield {
      val user = configMap.get("user").orNull[String]
      val password = configMap.get("password").orNull[String]
      JDBCSettings(driver, url, user, password)
    }) getOrElse {
      throw new ConfigurationException("Configuration error for database " + dbName + ". " + configMap.toString)
    }
  }

  def readConnectionPoolSettings(dbName: Symbol = ConnectionPool.DEFAULT_NAME): ConnectionPoolSettings = {
    val configMap = self.readAsMap(dbName)
    val default = new ConnectionPoolSettings
    ConnectionPoolSettings(
      initialSize = configMap.get("poolInitialSize").map(_.toInt).getOrElse(default.initialSize),
      maxSize = configMap.get("poolMaxSize").map(_.toInt).getOrElse(default.maxSize),
      connectionTimeoutMillis = configMap.get("connectionTimeoutMillis").map(_.toLong).getOrElse(default.connectionTimeoutMillis),
      validationQuery = configMap.get("poolValidationQuery").getOrElse(default.validationQuery)
    )
  }

  def loadGlobalSettings(): Unit = {
    for {
      globalConfig <- readConfig(config, envPrefix + "scalikejdbc.global")
      logConfig <- readConfig(globalConfig, "loggingSQLAndTime")
    } {
      val enabled = readBoolean(logConfig, "enabled").getOrElse(false)
      if (enabled) {
        val default = LoggingSQLAndTimeSettings()
        GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
          enabled = enabled,
          logLevel = readString(logConfig, "logLevel").map(v => Symbol(v)).getOrElse(default.logLevel),
          warningEnabled = readString(logConfig, "warningEnabled").map(_.toBoolean).getOrElse(default.warningEnabled),
          warningThresholdMillis = readString(logConfig, "warningThresholdMillis").map(_.toLong).getOrElse(default.warningThresholdMillis),
          warningLogLevel = readString(logConfig, "warningLogLevel").map(v => Symbol(v)).getOrElse(default.warningLogLevel)
        )
      } else {
        GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(enabled = false)
      }
    }
  }

  private def readConfig(config: Config, path: String): Option[Config] = {
    if (config.hasPath(path)) Some(config.getConfig(path)) else None
  }

  private def readBoolean(config: Config, path: String): Option[Boolean] = {
    if (config.hasPath(path)) Some(config.getBoolean(path)) else None
  }

  private def readString(config: Config, path: String): Option[String] = {
    if (config.hasPath(path)) Some(config.getString(path)) else None
  }

}

/**
 * Typesafe config reader
 *
 * It follows standard behavior of typesafe-config
 */
object TypesafeConfigReader extends TypesafeConfigReader
  with StandardTypesafeConfig
  with NoEnvPrefix

/**
 * Typesafe config reader with env prefix.
 */
case class TypesafeConfigReaderWithEnv(envValue: String)
    extends TypesafeConfigReader
    with StandardTypesafeConfig
    with EnvPrefix {

  override val env = Option(envValue)
}

