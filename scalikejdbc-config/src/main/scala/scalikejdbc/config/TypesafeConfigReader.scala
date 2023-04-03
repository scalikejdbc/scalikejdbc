package scalikejdbc.config

import scalikejdbc._
import com.typesafe.config.{ Config, ConfigException }
import scala.collection.mutable.{ Map => MutableMap }
import scala.collection.JavaConverters._

/**
 * TypesafeConfig reader
 */
trait TypesafeConfigReader extends NoEnvPrefix with LogSupport {
  self: TypesafeConfig =>

  def envPrefix: String = env.map(_ + ".").getOrElse("")

  lazy val dbNames: List[String] = {
    if (config.hasPath(envPrefix + "db")) {
      config.getConfig(envPrefix + "db").root.keySet.asScala.toList
    } else {
      Nil
    }
  }

  private val attributeNames = Seq(
    "url",
    "driver",
    "user",
    "username",
    "password",
    "poolInitialSize",
    "poolMaxSize",
    "poolConnectionTimeoutMillis",
    "connectionTimeoutMillis",
    "poolValidationQuery",
    "poolFactoryName",
    "poolWarmUpTimeMillis",
    "timeZone"
  )

  def readAsMap(
    dbName: String = ConnectionPool.DEFAULT_NAME
  ): Map[String, String] = try {
    val configMap: MutableMap[String, String] = MutableMap.empty

    {
      val dbConfig = config.getConfig(envPrefix + "db." + dbName)
      val iter = dbConfig.entrySet.iterator
      while (iter.hasNext) {
        val entry = iter.next()
        val key = entry.getKey
        if (attributeNames.contains(key)) {
          configMap(key) =
            config.getString(envPrefix + "db." + dbName + "." + key)
        }
      }
    }

    try {
      val topLevelConfig = config.getConfig("db." + dbName)
      val iter = topLevelConfig.entrySet.iterator
      while (iter.hasNext) {
        val entry = iter.next()
        val key = entry.getKey
        if (attributeNames.contains(key) && !configMap.contains(key)) {
          configMap(key) = config.getString("db." + dbName + "." + key)
        }
      }
    } catch { case e: ConfigException => }

    configMap.toMap
  } catch {
    case e: ConfigException => throw new ConfigurationException(e)
  }

  def readJDBCSettings(
    dbName: String = ConnectionPool.DEFAULT_NAME
  ): JDBCSettings = {
    val configMap = self.readAsMap(dbName)
    // https://github.com/scalikejdbc/scalikejdbc/issues/494#issuecomment-184015480
    // never forcing scalikejdbc-config users to load JDBC drivers in global
    val driver = configMap.getOrElse("driver", "")
    (for {
      url <- configMap.get("url")
    } yield {
      val user =
        configMap.get("user").orElse(configMap.get("username")).orNull[String]
      val password = configMap.get("password").orNull[String]
      JDBCSettings(url, user, password, driver)
    }) getOrElse {
      throw new ConfigurationException(
        "Configuration error for database " + dbName + ". " + configMap.toString
      )
    }
  }

  def readConnectionPoolSettings(
    dbName: String = ConnectionPool.DEFAULT_NAME
  ): ConnectionPoolSettings = {
    val configMap = self.readAsMap(dbName)
    val default = new ConnectionPoolSettings

    def readTimeoutMillis(): Option[Long] = {
      val timeout = configMap.get("poolConnectionTimeoutMillis")
      val oldTimeout = configMap.get("connectionTimeoutMillis")
      oldTimeout.foreach { _ =>
        log.info(
          "connectionTimeoutMillis is deprecated. Use poolConnectionTimeoutMillis instead."
        )
      }
      timeout.orElse(oldTimeout).map(_.toLong)
    }

    ConnectionPoolSettings(
      initialSize = configMap
        .get("poolInitialSize")
        .map(_.toInt)
        .getOrElse(default.initialSize),
      maxSize =
        configMap.get("poolMaxSize").map(_.toInt).getOrElse(default.maxSize),
      connectionTimeoutMillis =
        readTimeoutMillis().getOrElse(default.connectionTimeoutMillis),
      validationQuery =
        configMap.getOrElse("poolValidationQuery", default.validationQuery),
      connectionPoolFactoryName = configMap
        .getOrElse("poolFactoryName", default.connectionPoolFactoryName),
      driverName = configMap.get("driver").orNull[String],
      warmUpTime = configMap
        .get("poolWarmUpTimeMillis")
        .map(_.toLong)
        .getOrElse(default.warmUpTime),
      timeZone = configMap.get("timeZone").orNull[String]
    )
  }

  def loadGlobalSettings(): Unit = {
    readConfig(config, envPrefix + "scalikejdbc.global").foreach {
      globalConfig =>
        GlobalSettings.loggingSQLErrors =
          readBoolean(globalConfig, "loggingSQLErrors").getOrElse(
            GlobalSettings.loggingSQLErrors
          )
        GlobalSettings.loggingConnections =
          readBoolean(globalConfig, "loggingConnections").getOrElse(
            GlobalSettings.loggingConnections
          )

        for {
          logConfig <- readConfig(globalConfig, "loggingSQLAndTime")
        } {
          val enabled = readBoolean(logConfig, "enabled").getOrElse(
            GlobalSettings.loggingSQLAndTime.enabled
          )
          if (enabled) {
            val default = LoggingSQLAndTimeSettings()
            GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
              enabled = enabled,
              singleLineMode = readBoolean(logConfig, "singleLineMode")
                .getOrElse(default.singleLineMode),
              printUnprocessedStackTrace =
                readBoolean(logConfig, "printUnprocessedStackTrace").getOrElse(
                  default.printUnprocessedStackTrace
                ),
              stackTraceDepth = readInt(logConfig, "stackTraceDepth").getOrElse(
                default.stackTraceDepth
              ),
              logLevel =
                readString(logConfig, "logLevel").getOrElse(default.logLevel),
              warningEnabled = readBoolean(logConfig, "warningEnabled")
                .getOrElse(default.warningEnabled),
              warningThresholdMillis =
                readLong(logConfig, "warningThresholdMillis").getOrElse(
                  default.warningThresholdMillis
                ),
              warningLogLevel =
                readString(logConfig, "warningLogLevel").getOrElse(
                  default.warningLogLevel
                )
            )
          } else {
            GlobalSettings.loggingSQLAndTime =
              LoggingSQLAndTimeSettings(enabled = false)
          }
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

  private def readInt(config: Config, path: String): Option[Int] = {
    if (config.hasPath(path)) Some(config.getInt(path)) else None
  }

  private def readLong(config: Config, path: String): Option[Long] = {
    if (config.hasPath(path)) Some(config.getLong(path)) else None
  }

}

/**
 * Typesafe config reader
 *
 * It follows standard behavior of typesafe-config
 */
object TypesafeConfigReader
  extends TypesafeConfigReader
  with StandardTypesafeConfig
  with NoEnvPrefix
