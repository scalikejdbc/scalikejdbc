// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "idea" at "http://mpeltonen.github.com/maven/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % Option(System.getProperty("play.version")).getOrElse("2.0.2"))

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.0.0")

