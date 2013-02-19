package scalikejdbc.config

import scalikejdbc._
import com.typesafe.config.{ Config => TypesafeConfig, ConfigFactory, ConfigException }
import scala.collection.mutable.{ Map => MutableMap, ListBuffer }

class ConfigurationException(val message: String) extends Exception(message) {
  def this(e: Throwable) = this(e.getMessage)
}

object TypesafeConfigReader {

  lazy val _config: TypesafeConfig = ConfigFactory.load()

  lazy val dbNames: List[String] = {
    val it = _config.entrySet.iterator
    val buf: ListBuffer[String] = new ListBuffer
    while (it.hasNext) {
      val entry = it.next
      val key = entry.getKey
      key.split("\\.").toList match {
        case List("db", dbName, _) => {
          buf.append(dbName)
        }
        case _ => ()
      }
    }
    buf.toList.distinct
  }

  def readAsMap(dbName: Symbol): Map[String, String] = {
    try {
      val dbConfig = _config.getConfig("db." + dbName.name)
      val it = dbConfig.entrySet.iterator
      val configMap: MutableMap[String, String] = MutableMap.empty
      while (it.hasNext) {
        val entry = it.next
        val key = entry.getKey
        configMap(key) = _config.getString("db." + dbName.name + "." + key)
      }
      configMap.toMap
    } catch {
      case e: ConfigException => throw new ConfigurationException(e)
      case e: ConfigException => throw new ConfigurationException(e)
    }
  }

  def readJDBCSettings(dbName: Symbol): JDBCSettings = {
    val configMap = TypesafeConfigReader.readAsMap(dbName)

    (for {
      driver <- configMap.get("driver")
      url <- configMap.get("url")
      user <- configMap.get("user")
      password <- configMap.get("password")
    } yield {
      JDBCSettings(driver, url, user, password)
    }) getOrElse {
      throw new ConfigurationException("Configuration error for database " + dbName + ". " + configMap.toString)
    }
  }

}
