resolvers ++= Seq(
  "sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.3")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.8.1")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.4.6")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
