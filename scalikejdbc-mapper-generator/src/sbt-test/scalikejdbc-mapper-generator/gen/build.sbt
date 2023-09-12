val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

conflictWarning := {
  if (scalaBinaryVersion.value == "3") {
    // TODO
    ConflictWarning("warn", Level.Warn, false)
  } else {
    conflictWarning.value
  }
}

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

TaskKey[Unit]("createTestDatabase") := {
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

(Test / testOptions) += {
  val setting = (Compile / scalikejdbcJDBCSettings).value
  Tests.Setup { loader =>
    type Initializer = {
      def run(url: String, username: String, password: String): Unit
    }
    val initializer = loader
      .loadClass("app.Initializer")
      .getDeclaredConstructor()
      .newInstance()
      .asInstanceOf[Initializer]
    initializer.run(setting.url, setting.username, setting.password)
  }
}

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.13.12", "2.12.18", "3.1.3")

scalacOptions ++= Seq(
  "-Xlint",
  "-language:higherKinds,implicitConversions,postfixOps",
  "-deprecation",
  "-unchecked"
)

scalacOptions ++= {
  if (scalaBinaryVersion.value == "3") {
    Seq("-Xignore-scala2-macros")
  } else {
    Nil
  }
}

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-test" % scalikejdbcVersion % "test",
  "org.scalikejdbc" %% "scalikejdbc-syntax-support-macro" % scalikejdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-joda-time" % scalikejdbcVersion,
  "org.slf4j" % "slf4j-simple" % System.getProperty("slf4j.version"),
  "com.h2database" % "h2" % System.getProperty("h2.version"),
  "mysql" % "mysql-connector-java" % System.getProperty("mysql.version"),
  "org.postgresql" % "postgresql" % System.getProperty("postgresql.version"),
  "org.scalatest" %% "scalatest" % System.getProperty(
    "scalatest.version"
  ) % "test",
  "org.specs2" %% "specs2-core" % System.getProperty(
    "specs2.version"
  ) % "test" cross CrossVersion.for3Use2_13
)

TaskKey[Unit]("generateCodeForIssue339") := {
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

testFrameworks --= {
  if (scalaBinaryVersion.value == "3") {
    // specs2 does not support Scala 3
    // TODO remove this setting when specs2 for Scala 3 released
    Seq(TestFrameworks.Specs2)
  } else {
    Nil
  }
}
