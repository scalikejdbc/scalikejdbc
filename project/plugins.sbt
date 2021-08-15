resolvers ++= Seq(
  "sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.3")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.9.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.9")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
