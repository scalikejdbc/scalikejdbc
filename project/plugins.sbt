resolvers ++= Seq(
  "sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

addSbtPlugin("com.github.seratch" % "xsbt-scalag-plugin" % "[0.2,)")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "[1.0,)")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "[0.6,)")

libraryDependencies <+= (sbtVersion){ sv =>
  sv.split('.') match { case Array("0", a, b, _@_*) =>
    if (a.toInt <= 10 || a.toInt <= 11 && b.toInt <= 2) "org.scala-tools.sbt" %% "scripted-plugin" % sv
    else if (a.toInt == 11) "org.scala-sbt" %% "scripted-plugin" % sv
    else "org.scala-sbt" % "scripted-plugin" % sv
  }
}

logLevel := Level.Warn

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.2.0")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "[0.1,)")

