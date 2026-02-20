addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.5")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.1")

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:implicitConversions"
)
