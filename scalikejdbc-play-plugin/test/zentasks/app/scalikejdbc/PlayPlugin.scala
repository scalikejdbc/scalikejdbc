/*
 * Copyright 2012 Kazuhiro Sera
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
package scalikejdbc

import play.api._

/**
 * The Play plugin to use ScalikeJDBC
 */
class PlayPlugin(app: Application) extends Plugin {

  import PlayPlugin._

  private lazy val dbConfig = app.configuration.getConfig("db").getOrElse(Configuration.empty)

  dbConfig.subKeys map {
    name =>
      def load(name: String): (String, String, String, ConnectionPoolSettings) = {
        implicit val config = dbConfig
        Class.forName(require(name, "driver"))
        val default = new ConnectionPoolSettings
        val settings = new ConnectionPoolSettings(
          initialSize = opt(name, "poolInitialSize").map(v => v.toInt).getOrElse(default.initialSize),
          maxSize = opt(name, "poolMaxSize").map(v => v.toInt).getOrElse(default.maxSize),
          validationQuery = opt(name, "poolValidationQuery").getOrElse(default.validationQuery)
        )
        (require(name, "url"), opt(name, "user").getOrElse(""), opt(name, "password").getOrElse(""), settings)
      }

      name match {
        case "global" =>
          Logger(classOf[PlayPlugin]).warn(
            "Configuration with \"db.global\" is ignored. Use \"scalikejdbc.global\" instead.")
        case "default" =>
          val (url, user, password, settings) = load(name)
          ConnectionPool.singleton(url, user, password, settings)
        case _ =>
          val (url, user, password, settings) = load(name)
          ConnectionPool.add(Symbol(name), url, user, password, settings)
      }

  }

  private lazy val globalConfig = app.configuration.getConfig("scalikejdbc.global").getOrElse(Configuration.empty)

  private val loggingSQLAndTime = "loggingSQLAndTime"
  opt(loggingSQLAndTime, "enabled")(globalConfig).map(_.toBoolean).foreach {
    enabled =>
      implicit val config = globalConfig
      val default = new LoggingSQLAndTimeSettings
      GlobalSettings.loggingSQLAndTime = new LoggingSQLAndTimeSettings(
        enabled = enabled,
        logLevel = opt(loggingSQLAndTime, "logLevel").map(v => Symbol(v)).getOrElse(default.logLevel),
        warningEnabled = opt(loggingSQLAndTime, "warningEnabled").map(_.toBoolean).getOrElse(default.warningEnabled),
        warningThresholdMillis = opt(loggingSQLAndTime, "warningThresholdMillis").map(_.toLong).getOrElse(default.warningThresholdMillis),
        warningLogLevel = opt(loggingSQLAndTime, "warningLogLevel").map(v => Symbol(v)).getOrElse(default.warningLogLevel)
      )
  }

}

object PlayPlugin {

  def opt(name: String, key: String)(implicit config: Configuration): Option[String] = {
    config.getString(name + "." + key)
  }

  def require(name: String, key: String)(implicit config: Configuration): String = {
    config.getString(name + "." + key) getOrElse {
      throw config.reportError(name, "Missing configuration [db." + name + "." + key + "]")
    }
  }

}

