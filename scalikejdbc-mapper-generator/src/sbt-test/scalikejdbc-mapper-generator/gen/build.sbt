val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

Compile / scalikejdbcGeneratorSettings ~= { setting =>
  setting.copy(tableNameToSyntaxName = { tableName =>
    setting.tableNameToSyntaxName(tableName) match {
      case "as"       => "as_"
      case syntaxName => syntaxName
    }
  })
}

(Compile / scalikejdbcJDBCSettings) := {
  val props = new java.util.Properties()
  IO.load(props, file("test.properties"))
  def loadProp(key: String): String = Option(props.get(key))
    .map(_.toString)
    .getOrElse(throw new IllegalStateException("missing key " + key))
  JDBCSettings(
    driver = loadProp("jdbc.driver"),
    url = loadProp("jdbc.url"),
    username = "sa",
    password = "sa",
    schema = ""
  )
}

InputKey[Unit]("createTestDatabase") := {
  import scalikejdbc._
  val setting = (Compile / scalikejdbcJDBCSettings).value
  Class.forName(setting.driver)
  ConnectionPool.singleton(setting.url, setting.username, setting.password)
  DB.autoCommit { implicit s =>
    sql"create table if not exists programmers (id SERIAL PRIMARY KEY, name varchar(128), t1 timestamp not null, t2 date, t3 time, type int, to_string int, hash_code int, wait int, get_class int, notify int, notify_all int, product_arity int, product_iterator int, product_prefix int, copy int)".execute
      .apply()
    sql"create view programmers_view as (select * from programmers)".execute
      .apply()
    // https://github.com/scalikejdbc/scalikejdbc/issues/810
    sql"create table address_street (id int not null)".execute.apply()
  }
}

InputKey[Unit]("deleteTestDatabase") := {
  import scalikejdbc._
  val setting = (Compile / scalikejdbcJDBCSettings).value
  Class.forName(setting.driver)
  ConnectionPool.singleton(setting.url, setting.username, setting.password)
  DB.autoCommit { implicit s =>
    sql"drop view programmers_view".execute.apply()
    sql"drop table programmers".execute.apply()
    sql"drop table address_street".execute.apply()
  }
}

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.13.17", "2.12.20", "3.3.7")

scalacOptions ++= Seq(
  "-Xlint",
  "-language:higherKinds,implicitConversions",
  "-deprecation",
  "-unchecked"
)

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % "test",
  "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-joda-time" % scalikejdbcVersion,
  "org.slf4j" % "slf4j-simple" % System.getProperty("slf4j.version"),
  "com.h2database" % "h2" % System.getProperty("h2.version"),
  "com.mysql" % "mysql-connector-j" % System.getProperty("mysql.version"),
  "org.postgresql" % "postgresql" % System.getProperty("postgresql.version"),
  "org.scalatest" %% "scalatest" % System.getProperty(
    "scalatest.version"
  ) % "test",
  "org.specs2" %% "specs2-core" % System.getProperty(
    "specs2.version"
  ) % "test"
)

InputKey[Unit]("generateCodeForIssue339") := {
  import java.sql.JDBCType
  import scalikejdbc.mapper._
  val key = Column("key", JDBCType.INTEGER, true, true)
  val other = List(
    Column("column1", JDBCType.OTHER, true, false),
    Column("column2", JDBCType.OTHER, false, false)
  )
  val all = key :: other
  val table =
    Table("Issue339table", all, all.filter(_.isAutoIncrement), key :: Nil)
  val generator = new CodeGenerator(table)
  val code = generator.modelAll()
  println(code)
  assert(code.contains("rs.any("))
  assert(code.contains("rs.anyOpt("))
  generator.writeModel()
}

testResultLogger := TestResultLogger.Defaults.Main(
  printNoTests = TestResultLogger((_, _, _) => sys.error("invalid test name"))
)
