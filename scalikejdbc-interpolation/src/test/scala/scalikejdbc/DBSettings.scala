package scalikejdbc

trait DBSettings {

  if (!ConnectionPool.isInitialized(ConnectionPool.DEFAULT_NAME)) {
    val props = new java.util.Properties
    using(new java.io.FileInputStream("scalikejdbc-library/src/test/resources/jdbc.properties")) { in => props.load(in) }
    val driverClassName = props.getProperty("driverClassName")
    val url = props.getProperty("url")
    val user = props.getProperty("user")
    val password = props.getProperty("password")
    Class.forName(driverClassName)
    val poolSettings = new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
    ConnectionPool.singleton(url, user, password, poolSettings)
    GlobalSettings.sqlFormatter = SQLFormatterSettings("scalikejdbc.HibernateSQLFormatter")
  }

}

