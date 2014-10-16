scalikejdbcSettings

scalikejdbc.mapper.SbtKeys.scalikejdbcJDBCSettings in Compile := {
  val props = new java.util.Properties()
  IO.load(props, file("test.properties"))
  def loadProp(key: String): String = Option(props.get(key)).map(_.toString).getOrElse(throw new IllegalStateException("missing key " + key))
  scalikejdbc.mapper.SbtPlugin.JDBCSettings(
    driver = loadProp("jdbc.driver"),
    url = loadProp("jdbc.url"),
    username = "sa",
    password = "sa",
    schema = ""
  )
}

TaskKey[Unit]("createTestDatabase") := {
  import scalikejdbc._
  val setting = (mapper.SbtKeys.scalikejdbcJDBCSettings in Compile).value
  Class.forName(setting.driver)
  ConnectionPool.singleton(setting.url, setting.username, setting.password)
  DB.autoCommit { implicit s =>
    sql"create table programmers (id SERIAL PRIMARY KEY, name varchar(128))"
      .execute.apply()
  }
}

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.11.2", "2.10.4")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "org.scalikejdbc"     %% "scalikejdbc"                      % scalikejdbcVersion,
  "org.scalikejdbc"     %% "scalikejdbc-test"                 % scalikejdbcVersion % "test",
  "org.scalikejdbc"     %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
  "org.scalatest"       %% "scalatest"                        % System.getProperty("scalatest.version") % "test",
  "org.specs2"          %% "specs2-core"                      % System.getProperty("specs2.version") % "test"
)
