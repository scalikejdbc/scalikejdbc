addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.2.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.14.0")

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)

libraryDependencies += "com.github.xuwei-k" %% "scala-version-from-sbt-version" % "0.2.0"
