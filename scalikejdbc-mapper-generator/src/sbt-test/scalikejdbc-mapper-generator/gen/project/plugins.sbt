libraryDependencies += "com.h2database" % "h2" % "1.4.177"

{
  val pluginVersion = System.getProperty("plugin.version")
  if(pluginVersion == null)
    throw new RuntimeException("""|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
addSbtPlugin("org.scalikejdbc" %% "scalikejdbc-mapper-generator" % pluginVersion)
}

