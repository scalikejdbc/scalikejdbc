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

  // Play DB configuration

  private[this] lazy val playDbConfig = app.configuration.getConfig("db").getOrElse(Configuration.empty)

  // ScalikeJDBC global configuration

  private[this] lazy val globalConfig = app.configuration.getConfig("scalikejdbc.global").getOrElse(Configuration.empty)

  private[this] val loggingSQLAndTime = "loggingSQLAndTime"

  override def onStart(): Unit = {
    playDbConfig.subKeys map {
      name =>
        def load(name: String): (String, String, String, ConnectionPoolSettings) = {
          implicit val config = playDbConfig
          Class.forName(require(name, "driver"))
          val default = ConnectionPoolSettings()
          val settings = ConnectionPoolSettings(
            initialSize = opt(name, "poolInitialSize").map(v => v.toInt).getOrElse(default.initialSize),
            maxSize = opt(name, "poolMaxSize").map(v => v.toInt).getOrElse(default.maxSize),
            validationQuery = opt(name, "poolValidationQuery").getOrElse(default.validationQuery)
          )
          (require(name, "url"), opt(name, "user").getOrElse(""), opt(name, "password").getOrElse(""), settings)
        }

        registeredPoolNames.synchronized {
          name match {
            case "global" =>
              // because "db.global" was used as "scalikejdbc.global" previously
              Logger(classOf[PlayPlugin]).warn("Configuration with \"db.global\" is ignored. Use \"scalikejdbc.global\" instead.")
            case "default" =>
              if (!registeredPoolNames.contains("default")) {
                val (url, user, password, settings) = load(name)
                ConnectionPool.singleton(url, user, password, settings)
                registeredPoolNames.append("default")
              }
            case _ =>
              if (!registeredPoolNames.contains(name)) {
                val (url, user, password, settings) = load(name)
                ConnectionPool.add(Symbol(name), url, user, password, settings)
                registeredPoolNames.append(name)
              }
          }
        }
    }

    opt(loggingSQLAndTime, "enabled")(globalConfig).map(_.toBoolean).foreach {
      enabled =>
        implicit val config = globalConfig
        val default = LoggingSQLAndTimeSettings()
        GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
          enabled = enabled,
          logLevel = opt(loggingSQLAndTime, "logLevel").map(v => Symbol(v)).getOrElse(default.logLevel),
          warningEnabled = opt(loggingSQLAndTime, "warningEnabled").map(_.toBoolean).getOrElse(default.warningEnabled),
          warningThresholdMillis = opt(loggingSQLAndTime, "warningThresholdMillis").map(_.toLong).getOrElse(default.warningThresholdMillis),
          warningLogLevel = opt(loggingSQLAndTime, "warningLogLevel").map(v => Symbol(v)).getOrElse(default.warningLogLevel)
        )
    }

  }

  override def onStop(): Unit = {
    ConnectionPool.closeAll()
    registeredPoolNames.clear()
  }

}

object PlayPlugin {

  private val registeredPoolNames = new scala.collection.mutable.ListBuffer[String]

  def opt(name: String, key: String)(implicit config: Configuration): Option[String] = {
    config.getString(name + "." + key)
  }

  def require(name: String, key: String)(implicit config: Configuration): String = {
    config.getString(name + "." + key) getOrElse {
      throw config.reportError(name, "Missing configuration [db." + name + "." + key + "]")
    }
  }

}

