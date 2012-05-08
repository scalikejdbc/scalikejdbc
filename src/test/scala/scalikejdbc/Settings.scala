package scalikejdbc

import java.util.Properties

trait Settings {

  private val props = new Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))

  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  Class.forName(driverClassName)

  val poolSettings = new ConnectionPoolSettings(initialSize = 100, maxSize = 100)
  ConnectionPool.singleton(url, user, password, poolSettings)
  ConnectionPool.add('named, url, user, password, poolSettings)

}
