package scalikejdbc

trait DBSettings {

  val driverClassName: String = {
    val props = new java.util.Properties
    using(
      new java.io.FileInputStream(
        "scalikejdbc-core/src/test/resources/jdbc.properties"
      )
    ) { in => props.load(in) }
    val url = props.getProperty("url")
    val user = props.getProperty("user")
    val password = props.getProperty("password")

    if (!ConnectionPool.isInitialized(ConnectionPool.DEFAULT_NAME)) {
      val poolSettings =
        new ConnectionPoolSettings(initialSize = 50, maxSize = 50)
      ConnectionPool.singleton(url, user, password, poolSettings)
    }

    GlobalSettings.sqlFormatter = SQLFormatterSettings(
      "scalikejdbc.HibernateSQLFormatter"
    )
    val driver = props.getProperty("driverClassName")
    Class.forName(driver)
    driver
  }

  Class.forName("org.h2.Driver")
  ConnectionPool.add("yetanother", "jdbc:h2:mem:yetanother", "sa", "sa")

}
