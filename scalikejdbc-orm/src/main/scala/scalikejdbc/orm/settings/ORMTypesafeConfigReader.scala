package scalikejdbc.orm.settings

import com.typesafe.config.impl.ConfigImpl
import com.typesafe.config._

import java.io.File
import java.net.URL
import scala.util.Try
import scala.collection.JavaConverters._

object ORMTypesafeConfigReader {

  /**
   * Loads a configuration file.
   *
   * @param file file
   * @return config
   */
  def load(file: File): Config = ConfigFactory.parseFile(file)

  /**
   * Loads a configuration file.
   *
   * @param resource file resource
   * @return config
   */
  def load(resource: String): Config =
    ConfigFactory.load(getClass.getClassLoader, resource)

  /**
   * Loads config values without system properties.
   *
   * @param resource file resource
   * @return config
   */
  def loadWithoutSystemProperties(resource: String): Config = {
    val loader: ClassLoader = getClass.getClassLoader
    val parseOptions: ConfigParseOptions =
      ConfigParseOptions.defaults.setClassLoader(loader)
    val config: Config =
      ConfigImpl.parseResourcesAnySyntax(resource, parseOptions).toConfig
    config.resolve(ConfigResolveOptions.defaults)
  }

  /**
   * Loads a configuration file as Map object.
   *
   * @param resource file resource
   * @return Map object
   */
  def loadAsMap(resource: String): Map[String, String] = fromConfigToMap(
    load(resource)
  )

  /**
   * Loads config values without system properties.
   *
   * @param resource file resource
   * @return Map object
   */
  def loadAsMapWithoutSystemProperties(
    resource: String
  ): Map[String, String] = {
    fromConfigToMap(loadWithoutSystemProperties(resource))
  }

  /**
   * Loads a Map object from Typesafe-config object.
   *
   * @param config config
   * @return Map object
   */
  def fromConfigToMap(config: Config): Map[String, String] = {
    def extract(map: java.util.Map[String, Any]): Map[String, String] = {
      map.asScala.flatMap {
        case (parentKey, value: java.util.Map[?, ?]) =>
          extract(value.asInstanceOf[java.util.Map[String, Any]]).map {
            case (k, v) => s"${parentKey}.${k}" -> v
          }
        case (key, value) => Map(key -> value)
      }
    }.map { case (k, v) => k -> v.toString }.toMap

    config
      .root()
      .keySet()
      .asScala
      .flatMap { parentKey =>
        config.root().unwrapped().get(parentKey) match {
          case map: java.util.Map[?, ?] =>
            extract(
              config.root().unwrapped().asInstanceOf[java.util.Map[String, Any]]
            )
          case value =>
            Map(parentKey -> value)
        }
      }
      .map { case (k, v) => k -> v.toString }
      .toMap
  }

  /**
   * Returns a config object from env or default.
   *
   * @param env env string
   * @return config
   */
  def config(env: String): Config = {
    val config = defaultConfig
    config.getConfig(env).withFallback(config)
  }

  /**
   * Returns default config object.
   *
   * @return default config
   */
  def defaultConfig: Config = {
    Seq(
      findEnv("SCALIKEJDBC_CONFIG_RESOURCE", "config.resource").map(r =>
        ConfigFactory.load(r)
      ),
      findEnv("SCALIKEJDBC_CONFIG_FILE", "config.file").map(f =>
        ConfigFactory.parseFile(new File(f))
      ),
      findEnv("SCALIKEJDBC_CONFIG_URL", "config.url").map(u =>
        ConfigFactory.parseURL(new URL(u))
      )
    ).flatten.foldLeft(ConfigFactory.load()) { case (config, each) =>
      config.withFallback(each)
    }
  }

  private[this] def findEnv(env: String, prop: String): Option[String] = {
    Option(System.getenv(env))
      .orElse(Option(System.getProperty(prop)))
      .filterNot(_.trim.isEmpty)
  }

  def boolean(env: String, path: String): Option[Boolean] = {
    Try(config(env).getBoolean(path)).toOption
  }

  def booleanSeq(env: String, path: String): Option[Seq[Boolean]] = {
    Try(
      config(env)
        .getBooleanList(path)
        .asScala
        .map(_.asInstanceOf[Boolean])
        .toIndexedSeq
    ).toOption
  }

  def double(env: String, path: String): Option[Double] = {
    Try(config(env).getDouble(path)).toOption
  }

  def doubleSeq(env: String, path: String): Option[Seq[Double]] = {
    Try(
      config(env)
        .getDoubleList(path)
        .asScala
        .map(_.asInstanceOf[Double])
        .toIndexedSeq
    ).toOption
  }

  def int(env: String, path: String): Option[Int] = {
    Try(config(env).getInt(path)).toOption
  }

  def intSeq(env: String, path: String): Option[Seq[Int]] = {
    Try(
      config(env).getIntList(path).asScala.map(_.asInstanceOf[Int]).toIndexedSeq
    ).toOption
  }

  def long(env: String, path: String): Option[Long] = {
    Try(config(env).getLong(path)).toOption
  }

  def longSeq(env: String, path: String): Option[Seq[Long]] = {
    Try(
      config(env)
        .getLongList(path)
        .asScala
        .map(_.asInstanceOf[Long])
        .toIndexedSeq
    ).toOption
  }

  def string(env: String, path: String): Option[String] = {
    Try(config(env).getString(path)).toOption
  }

  def stringSeq(env: String, path: String): Option[Seq[String]] = {
    Try(config(env).getStringList(path).asScala.toIndexedSeq).toOption
  }

  def get(env: String, path: String): Option[ConfigValue] = {
    Try(config(env).getValue(path)).toOption
  }

}
