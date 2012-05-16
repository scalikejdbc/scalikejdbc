package scalikejdbc

import java.util.Properties

trait Settings {

  val props = new Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))
  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  if (ConnectionPool.get() == null) {
    Class.forName(driverClassName)
    val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
    ConnectionPool.singleton(url, user, password, poolSettings)
    if (ConnectionPool.get('named) == null) {
      ConnectionPool.add('named, url, user, password, poolSettings)
    }
  }

}
