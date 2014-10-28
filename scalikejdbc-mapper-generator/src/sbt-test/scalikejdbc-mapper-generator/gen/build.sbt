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

testOptions in Test += Tests.Setup{ loader =>
  type Initializer = {def run(url: String, username: String, password: String)}
  val setting = (scalikejdbc.mapper.SbtKeys.scalikejdbcJDBCSettings in Compile).value
  val initializer = loader.loadClass("app.Initializer").newInstance().asInstanceOf[Initializer]
  initializer.run(setting.url, setting.username, setting.password)
}

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.11.2", "2.10.4")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "org.scalikejdbc"     %% "scalikejdbc"                      % scalikejdbcVersion,
  "org.scalikejdbc"     %% "scalikejdbc-test"                 % scalikejdbcVersion % "test",
  "org.scalikejdbc"     %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
  "com.h2database"      %  "h2"                               % System.getProperty("h2.version"),
  "mysql"               %  "mysql-connector-java"             % System.getProperty("mysql.version"),
  "org.postgresql"      %  "postgresql"                       % System.getProperty("postgresql.version"),
  "org.scalatest"       %% "scalatest"                        % System.getProperty("scalatest.version") % "test",
  "org.specs2"          %% "specs2-core"                      % System.getProperty("specs2.version") % "test"
)
