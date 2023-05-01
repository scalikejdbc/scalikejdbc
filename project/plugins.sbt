resolvers ++= Seq(
  "sonatype staging" at "https://oss.sonatype.org/content/repositories/staging",
  "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.0")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.19")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
