package app

object Initializer {
  def run(): Unit = synchronized {
    val props = new java.util.Properties()
    props.load(new java.io.FileInputStream("test.properties"))
    scalikejdbc.ConnectionPool.singleton(
      url = props.get("jdbc.url").toString,
      user = "sa",
      password = "sa",
    )
  }
}
