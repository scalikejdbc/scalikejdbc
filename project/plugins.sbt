addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "1.1.4")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.13.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.21")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

if (sys.env.isDefinedAt("GITHUB_ACTION")) {
  Def.settings(
    addSbtPlugin("net.virtual-void" % "sbt-hackers-digest" % "0.1.2")
  )
} else {
  Nil
}
