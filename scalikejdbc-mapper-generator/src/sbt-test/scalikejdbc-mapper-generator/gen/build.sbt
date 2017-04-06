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
    sql"create table if not exists programmers (id SERIAL PRIMARY KEY, name varchar(128), t1 timestamp not null, t2 date, t3 time, type int, to_string int, hash_code int, wait int, get_class int, notify int, notify_all int, product_arity int, product_iterator int, product_prefix int)"
      .execute.apply()
    sql"create view programmers_view as (select * from programmers)"
      .execute.apply()
  }
}

testOptions in Test += Tests.Setup{ loader =>
  type Initializer = {def run(url: String, username: String, password: String)}
  val setting = (scalikejdbc.mapper.SbtKeys.scalikejdbcJDBCSettings in Compile).value
  val initializer = loader.loadClass("app.Initializer").getDeclaredConstructor().newInstance().asInstanceOf[Initializer]
  initializer.run(setting.url, setting.username, setting.password)
}

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.12.1", "2.11.8", "2.10.6")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked", "-Xfatal-warnings")

libraryDependencies ++= Seq(
  "org.scalikejdbc"     %% "scalikejdbc"                      % scalikejdbcVersion,
  "org.scalikejdbc"     %% "scalikejdbc-test"                 % scalikejdbcVersion % "test",
  "org.scalikejdbc"     %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
  "org.slf4j"           %  "slf4j-simple"                     % System.getProperty("slf4j.version"),
  "com.h2database"      %  "h2"                               % System.getProperty("h2.version"),
  "mysql"               %  "mysql-connector-java"             % System.getProperty("mysql.version"),
  "org.postgresql"      %  "postgresql"                       % System.getProperty("postgresql.version"),
  "org.scalatest"       %% "scalatest"                        % System.getProperty("scalatest.version") % "test",
  "org.specs2"          %% "specs2-core"                      % System.getProperty("specs2.version") % "test"
)

TaskKey[Unit]("generateCodeForIssue339") := {
  import java.sql.Types
  import scalikejdbc.mapper._
  val key = Column("key", Types.INTEGER, true, true)
  val other = List(
    Column("column1", Types.OTHER, true, false),
    Column("column2", Types.OTHER, false, false)
  )
  val all = key :: other
  val table = Table("Issue339table", all, all.filter(_.isAutoIncrement), key :: Nil)
  val generator = new CodeGenerator(table)
  val code = generator.modelAll()
  println(code)
  assert(code.contains("rs.any("))
  assert(code.contains("rs.anyOpt("))
  generator.writeModel()
}
