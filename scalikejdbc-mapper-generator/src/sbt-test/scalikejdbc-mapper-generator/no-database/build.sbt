import scalikejdbc.mapper._

val root = project.in(file(".")).enablePlugins(ScalikejdbcPlugin)

val scalikejdbcVersion = System.getProperty("plugin.version")

crossScalaVersions := List("2.12.2", "2.11.12", "2.10.6")

scalacOptions ++= Seq("-Xlint", "-language:_", "-deprecation", "-unchecked")

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc" % scalikejdbcVersion,
  "org.slf4j" % "slf4j-simple" % System.getProperty("slf4j.version")
)

(scalikejdbcJDBCSettings in Compile) := JDBCSettings(
  "dummy driver name",
  "dummy url",
  "dummy username",
  "dummy password",
  "dummy schema"
)

(scalikejdbcCodeGeneratorAll in Compile) := { (_, generatorSettings) =>
  val idColumn = Column("id", java.sql.Types.INTEGER, true, true)
  val columns = List(
    idColumn,
    Column("name", java.sql.Types.VARCHAR, true, false),
    Column("updated_at", java.sql.Types.TIMESTAMP, false, false)
  )
  val table = Table(
    "Programmers",
    columns,
    columns.filter(_.isAutoIncrement),
    idColumn :: Nil
  )
  Seq(new scalikejdbc.mapper.CodeGenerator(table))
}
