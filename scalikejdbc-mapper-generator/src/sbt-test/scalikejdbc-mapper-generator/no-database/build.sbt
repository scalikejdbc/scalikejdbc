import scalikejdbc.mapper._

val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.13.10", "2.12.17", "3.2.2")

scalacOptions ++= Seq(
  "-Xlint",
  "-language:higherKinds,implicitConversions",
  "-deprecation",
  "-unchecked"
)

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
