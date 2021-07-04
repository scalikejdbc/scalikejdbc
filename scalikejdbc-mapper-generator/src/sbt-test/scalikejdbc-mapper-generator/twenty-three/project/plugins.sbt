libraryDependencies ++= Seq(
  "com.h2database" % "h2" % System.getProperty("h2.version"),
  "mysql" % "mysql-connector-java" % System.getProperty("mysql.version"),
  "org.postgresql" % "postgresql" % System.getProperty("postgresql.version"),
)

{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null)
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin
    )
  addSbtPlugin(
    "org.scalikejdbc" %% "scalikejdbc-mapper-generator" % pluginVersion
  )
}
