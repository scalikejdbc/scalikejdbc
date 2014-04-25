package scalikejdbc

import java.util.Properties

trait Settings {

  val props = new Properties
  props.load(classOf[Settings].getClassLoader.getResourceAsStream("jdbc.properties"))
  val driverClassName = props.getProperty("driverClassName")
  val url = props.getProperty("url")
  val user = props.getProperty("user")
  val password = props.getProperty("password")

  def initializeConnectionPools() = {
    try ConnectionPool.get()
    catch {
      case e: IllegalStateException =>
        Class.forName(driverClassName)
        val poolSettings = new ConnectionPoolSettings(initialSize = 1, maxSize = 50)
        ConnectionPool.singleton(url, user, password, poolSettings)

        try ConnectionPool.get('named)
        catch {
          case e: IllegalStateException =>
            ConnectionPool.add('named, url, user, password, poolSettings)
        }
    }
  }

  initializeConnectionPools()

}
