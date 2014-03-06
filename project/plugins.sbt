resolvers ++= Seq(
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.github.seratch" % "xsbt-scalag-plugin" % "[0.2,)")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.1")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "[0.6,)")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.3")

