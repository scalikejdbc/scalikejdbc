import scalikejdbc.mapper._

val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.13.8", "2.12.17", "3.1.3")

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
  "org.scalikejdbc" %% "scalikejdbc-joda-time" % scalikejdbcVersion,
  "org.slf4j" % "slf4j-simple" % System.getProperty("slf4j.version")
)

Compile / scalikejdbcJDBCSettings := JDBCSettings(
  "dummy driver name",
  "dummy url",
  "dummy username",
  "dummy password",
  "dummy schema"
)

(Compile / scalikejdbcCodeGeneratorAll) := { (_, generatorSettings) =>
  val idColumn = Column("id", java.sql.JDBCType.INTEGER, true, true)
  val columns = List(
    idColumn,
    Column("name", java.sql.JDBCType.VARCHAR, true, false),
    Column("updated_at", java.sql.JDBCType.TIMESTAMP, false, false)
  )
  val table = Table(
    "Programmers",
    columns,
    columns.filter(_.isAutoIncrement),
    idColumn :: Nil
  )
  Seq(new scalikejdbc.mapper.CodeGenerator(table))
}

testFrameworks --= {
  if (scalaBinaryVersion.value == "3") {
    // specs2 does not support Scala 3
    // TODO remove this setting when specs2 for Scala 3 released
    Seq(TestFrameworks.Specs2)
  } else {
    Nil
  }
}
