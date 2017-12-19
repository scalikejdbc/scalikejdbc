val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

scalikejdbcJDBCSettings in Compile := {
  val props = new java.util.Properties()
  IO.load(props, file("test.properties"))
  def loadProp(key: String): String = Option(props.get(key)).map(_.toString).getOrElse(throw new IllegalStateException("missing key " + key))
  JDBCSettings(
    driver = loadProp("jdbc.driver"),
    url = loadProp("jdbc.url"),
    username = "sa",
    password = "sa",
    schema = ""
  )
}

TaskKey[Unit]("createTestDatabase") := {
  import scalikejdbc._
  val setting = (scalikejdbcJDBCSettings in Compile).value
  Class.forName(setting.driver)
  ConnectionPool.singleton(setting.url, setting.username, setting.password)
  DB.autoCommit { implicit s =>
    SQL("create table if not exists twenty_three(field1 SERIAL PRIMARY KEY," +
      (2 to 23).map("field" + _ + " bigint").mkString(",") + ")" ).execute.apply()
  }
}

val scalikejdbcVersion = System.getProperty("plugin.version")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked")

val specs2Version = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) =>
      // specs2 4 does not support Scala 2.10
      // https://repo1.maven.org/maven2/org/specs2/specs2-core_2.10/
      "3.9.5"
    case _ =>
      System.getProperty("specs2.version")
  }
}

libraryDependencies ++= Seq(
  "org.scalikejdbc"     %% "scalikejdbc"              % scalikejdbcVersion,
  "org.scalikejdbc"     %% "scalikejdbc-test"         % scalikejdbcVersion % "test",
  "org.slf4j"           %  "slf4j-simple"             % System.getProperty("slf4j.version"),
  "org.scalatest"       %% "scalatest"                % System.getProperty("scalatest.version") % "test",
  "org.specs2"          %% "specs2-core"              % specs2Version.value % "test"
)
